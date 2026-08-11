package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.catalog.CatalogChangeSummary
import com.iptvcinema.tv.core.catalog.CatalogFingerprint
import com.iptvcinema.tv.core.catalog.CatalogSyncResource
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.database.entity.CatalogSyncMetadataEntity
import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import com.iptvcinema.tv.core.epg.EpgSyncRepository
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.network.ConditionalFetchResult
import com.iptvcinema.tv.core.network.HttpValidators
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository
import com.iptvcinema.tv.core.player.WatchedSeriesEpisodePrefetcher
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class XtreamSyncStep {
    VALIDATING_URL,
    AUTHENTICATING,
    LIVE_CATEGORIES,
    LIVE_STREAMS,
    VOD_CATEGORIES,
    VOD_STREAMS,
    SERIES_CATEGORIES,
    SERIES,
    WATCHED_SERIES_EPISODES,
    EPG,
    COMPLETE,
}

data class XtreamSyncProgress(
    val step: XtreamSyncStep,
    val isSuccess: Boolean = true,
    val message: String? = null,
)

sealed class XtreamSyncResult {
    data class Success(
        val liveChannelCount: Int,
        val movieCount: Int,
        val seriesCount: Int,
        val changes: CatalogChangeSummary = CatalogChangeSummary(),
        val checkedProvider: Boolean = true,
    ) : XtreamSyncResult()

    data class AuthFailed(
        val message: String,
        val status: SourceStatus,
        val retryable: Boolean = false,
    ) : XtreamSyncResult()
    data class Failed(val message: String, val retryable: Boolean = false) : XtreamSyncResult()
}

@Singleton
class XtreamSyncRepository @Inject constructor(
    private val xtreamRepository: XtreamRepository,
    private val catalogDaoFacade: CatalogDaoFacade,
    private val playlistSourcesRepository: PlaylistSourcesRepository,
    private val epgSyncRepository: EpgSyncRepository,
    private val watchedSeriesEpisodePrefetcher: WatchedSeriesEpisodePrefetcher,
    private val episodeCatalogRepository: EpisodeCatalogRepository,
) {
    private val _progress = MutableStateFlow<List<XtreamSyncProgress>>(emptyList())
    val progress: StateFlow<List<XtreamSyncProgress>> = _progress.asStateFlow()

    internal companion object {
        fun hasCachedCatalogData(
            syncState: LocalSourceSyncStateEntity?,
            channelRowCount: Int,
        ): Boolean {
            if (syncState != null &&
                (syncState.liveChannelCount > 0 || syncState.movieCount > 0 || syncState.seriesCount > 0)
            ) {
                return true
            }
            return channelRowCount > 0
        }
    }

    suspend fun syncSource(
        sourceId: String,
        credentials: XtreamCredentials,
        refreshEpgWhenUnchanged: Boolean = false,
        useConditionalRequests: Boolean = true,
        awaitEpgCompletion: Boolean = false,
    ): XtreamSyncResult = performSync(
        sourceId,
        credentials,
        refreshEpgWhenUnchanged,
        useConditionalRequests,
        awaitEpgCompletion,
    )

    private suspend fun performSync(
        sourceId: String,
        credentials: XtreamCredentials,
        refreshEpgWhenUnchanged: Boolean,
        useConditionalRequests: Boolean,
        awaitEpgCompletion: Boolean,
    ): XtreamSyncResult {
        _progress.value = emptyList()
        updateRemoteStatus(sourceId, SourceStatus.SYNCING, null)

        val serverUrl = runCatching { xtreamRepository.normalizedServer(credentials) }.getOrElse { error ->
            val message = error.message ?: "Invalid server URL"
            markFailure(sourceId, message, SourceStatus.FAILED)
            return XtreamSyncResult.Failed(message)
        }
        appendProgress(XtreamSyncStep.VALIDATING_URL, message = "Server URL validated")

        val authResult = xtreamRepository.validateAndAuthenticate(credentials)
        if (authResult !is XtreamAuthResult.Success) {
            val status = xtreamRepository.authResultToStatus(authResult)
            val message = when (authResult) {
                is XtreamAuthResult.InvalidCredentials -> authResult.message
                is XtreamAuthResult.Expired -> authResult.message
                is XtreamAuthResult.Unreachable -> authResult.message
                is XtreamAuthResult.Error -> authResult.message
                else -> "Authentication failed"
            }
            appendProgress(XtreamSyncStep.AUTHENTICATING, isSuccess = false, message = message)
            markFailure(sourceId, message, status)
            return XtreamSyncResult.AuthFailed(
                message,
                status,
                retryable = (authResult as? XtreamAuthResult.Unreachable)?.retryable == true,
            )
        }
        appendProgress(XtreamSyncStep.AUTHENTICATING, message = "Authenticated")

        return try {
            val previousMetadata = catalogDaoFacade.syncMetadata.getBySource(sourceId).associateBy { it.resourceKey }
            val requestMetadata = if (useConditionalRequests) previousMetadata else emptyMap()
            val liveCategoryResponse = xtreamRepository.fetchLiveCategories(
                credentials,
                requestMetadata.validators(CatalogSyncResource.XTREAM_LIVE_CATEGORIES),
            )
            val liveStreamResponse = xtreamRepository.fetchLiveStreams(
                credentials,
                requestMetadata.validators(CatalogSyncResource.XTREAM_LIVE_STREAMS),
            )
            val vodCategoryResponse = xtreamRepository.fetchVodCategories(
                credentials,
                requestMetadata.validators(CatalogSyncResource.XTREAM_VOD_CATEGORIES),
            )
            val vodStreamResponse = xtreamRepository.fetchVodStreams(
                credentials,
                requestMetadata.validators(CatalogSyncResource.XTREAM_VOD_STREAMS),
            )
            val seriesCategoryResponse = xtreamRepository.fetchSeriesCategories(
                credentials,
                requestMetadata.validators(CatalogSyncResource.XTREAM_SERIES_CATEGORIES),
            )
            val seriesResponse = xtreamRepository.fetchSeries(
                credentials,
                requestMetadata.validators(CatalogSyncResource.XTREAM_SERIES),
            )

            val liveCategories = prepareCategories(
                sourceId,
                CatalogDaoFacade.LIVE,
                CatalogSyncResource.XTREAM_LIVE_CATEGORIES,
                liveCategoryResponse,
                previousMetadata,
            ) { id, dtos -> XtreamNormalizer.normalizeLiveCategories(id, dtos).first }
            appendProgress(XtreamSyncStep.LIVE_CATEGORIES, message = "${liveCategories.effective.size} categories")
            val liveCategoryNames = liveCategories.effective.associate { it.id to it.name }

            val liveChannels = when (liveStreamResponse) {
                is ConditionalFetchResult.Modified -> XtreamNormalizer.normalizeLiveStreams(
                    sourceId,
                    credentials,
                    serverUrl,
                    liveStreamResponse.body,
                    liveCategoryNames,
                )
                is ConditionalFetchResult.NotModified -> null
            }
            val preparedLive = prepareEntities(
                sourceId,
                CatalogSyncResource.XTREAM_LIVE_STREAMS,
                liveChannels,
                liveStreamResponse.validators,
                previousMetadata,
            )
            appendProgress(
                XtreamSyncStep.LIVE_STREAMS,
                message = "${liveChannels?.size ?: currentCount(sourceId, ContentCount.LIVE)} channels",
            )

            val vodCategories = prepareCategories(
                sourceId,
                CatalogDaoFacade.VOD,
                CatalogSyncResource.XTREAM_VOD_CATEGORIES,
                vodCategoryResponse,
                previousMetadata,
            ) { id, dtos -> XtreamNormalizer.normalizeVodCategories(id, dtos) }
            appendProgress(XtreamSyncStep.VOD_CATEGORIES, message = "${vodCategories.effective.size} categories")
            val vodCategoryNames = vodCategories.effective.associate { it.id to it.name }

            val movies = when (vodStreamResponse) {
                is ConditionalFetchResult.Modified -> XtreamNormalizer.normalizeVodStreams(
                    sourceId,
                    credentials,
                    serverUrl,
                    vodStreamResponse.body,
                    vodCategoryNames,
                )
                is ConditionalFetchResult.NotModified -> null
            }
            val preparedMovies = prepareEntities(
                sourceId,
                CatalogSyncResource.XTREAM_VOD_STREAMS,
                movies,
                vodStreamResponse.validators,
                previousMetadata,
            )
            appendProgress(
                XtreamSyncStep.VOD_STREAMS,
                message = "${movies?.size ?: currentCount(sourceId, ContentCount.MOVIES)} movies",
            )

            val seriesCategories = prepareCategories(
                sourceId,
                CatalogDaoFacade.SERIES,
                CatalogSyncResource.XTREAM_SERIES_CATEGORIES,
                seriesCategoryResponse,
                previousMetadata,
            ) { id, dtos -> XtreamNormalizer.normalizeSeriesCategories(id, dtos) }
            appendProgress(XtreamSyncStep.SERIES_CATEGORIES, message = "${seriesCategories.effective.size} categories")
            val seriesCategoryNames = seriesCategories.effective.associate { it.id to it.name }

            val seriesItems = when (seriesResponse) {
                is ConditionalFetchResult.Modified -> XtreamNormalizer.normalizeSeries(
                    sourceId,
                    seriesResponse.body,
                    seriesCategoryNames,
                )
                is ConditionalFetchResult.NotModified -> null
            }
            val preparedSeries = prepareEntities(
                sourceId,
                CatalogSyncResource.XTREAM_SERIES,
                seriesItems,
                seriesResponse.validators,
                previousMetadata,
            )
            appendProgress(
                XtreamSyncStep.SERIES,
                message = "${seriesItems?.size ?: currentCount(sourceId, ContentCount.SERIES)} series",
            )

            val metadata = listOf(
                liveCategories.metadata,
                preparedLive.metadata,
                vodCategories.metadata,
                preparedMovies.metadata,
                seriesCategories.metadata,
                preparedSeries.metadata,
            )
            val syncedAt = Instant.now()
            val reconciliation = catalogDaoFacade.reconcileCatalog(
                sourceId = sourceId,
                liveCategories = liveCategories.changed,
                channels = preparedLive.changed,
                vodCategories = vodCategories.changed,
                movies = preparedMovies.changed,
                seriesCategories = seriesCategories.changed,
                seriesItems = preparedSeries.changed,
                metadata = metadata,
                syncedAtEpochMs = syncedAt.toEpochMilli(),
            )
            val changes = reconciliation.changes

            if (changes.hasChanges) {
                runCatching { episodeCatalogRepository.prefetchTopSeriesEpisodes(sourceId, limit = 5) }
            }
            val prefetchedSeriesCount = if (changes.hasChanges || refreshEpgWhenUnchanged) {
                runCatching { watchedSeriesEpisodePrefetcher.prefetchForCurrentSession() }.getOrDefault(0)
            } else {
                0
            }
            appendProgress(
                XtreamSyncStep.WATCHED_SERIES_EPISODES,
                message = when {
                    prefetchedSeriesCount > 0 -> "$prefetchedSeriesCount watched series"
                    changes.hasChanges || refreshEpgWhenUnchanged -> "No watched series to prefetch"
                    else -> "Catalog unchanged"
                },
            )

            updateRemoteStatus(sourceId, SourceStatus.ACTIVE, syncedAt)
            appendProgress(XtreamSyncStep.COMPLETE, message = "Sync complete")

            currentCoroutineContext().ensureActive()
            if (changes.hasChanges || refreshEpgWhenUnchanged) {
                val channelsForEpg = liveChannels ?: catalogDaoFacade.channels.getAllOrdered(sourceId)
                val onComplete: suspend (com.iptvcinema.tv.core.epg.EpgSyncOutcome) -> Unit = { outcome ->
                    appendProgress(XtreamSyncStep.EPG, outcome.isSuccess, outcome.message)
                }
                if (awaitEpgCompletion) {
                    epgSyncRepository.syncEpgAndWait(
                        sourceId = sourceId,
                        channels = channelsForEpg,
                        fetchXml = { xtreamRepository.fetchXmltv(credentials) },
                        onComplete = onComplete,
                    )
                } else {
                    epgSyncRepository.syncEpgInBackground(
                        sourceId = sourceId,
                        channels = channelsForEpg,
                        fetchXml = { xtreamRepository.fetchXmltv(credentials) },
                        onComplete = onComplete,
                    )
                }
            }

            XtreamSyncResult.Success(
                liveChannelCount = reconciliation.liveChannelCount,
                movieCount = reconciliation.movieCount,
                seriesCount = reconciliation.seriesCount,
                changes = changes,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val message = error.message ?: "Sync failed"
            markFailure(sourceId, message, SourceStatus.FAILED)
            XtreamSyncResult.Failed(message, retryable = xtreamRepository.isRetryable(error))
        }
    }

    suspend fun getSyncState(sourceId: String): LocalSourceSyncStateEntity? = catalogDaoFacade.syncState.get(sourceId)

    private suspend fun prepareCategories(
        sourceId: String,
        contentType: String,
        resourceKey: String,
        response: ConditionalFetchResult<List<XtreamCategoryDto>>,
        previous: Map<String, CatalogSyncMetadataEntity>,
        normalize: (String, List<XtreamCategoryDto>) -> List<LocalCategoryEntity>,
    ): PreparedCategories {
        val old = previous[resourceKey]
        return when (response) {
            is ConditionalFetchResult.Modified -> {
                val normalized = normalize(sourceId, response.body)
                val fingerprint = CatalogFingerprint.of(normalized)
                PreparedCategories(
                    effective = normalized,
                    changed = normalized.takeIf { fingerprint != old?.contentFingerprint },
                    metadata = metadata(sourceId, resourceKey, response.validators, fingerprint),
                )
            }
            is ConditionalFetchResult.NotModified -> PreparedCategories(
                effective = catalogDaoFacade.categories.getByType(sourceId, contentType),
                changed = null,
                metadata = metadata(
                    sourceId,
                    resourceKey,
                    response.validators,
                    old?.contentFingerprint ?: throw java.io.IOException(
                        "Provider returned not modified without cached $resourceKey metadata",
                    ),
                ),
            )
        }
    }

    private fun <T> prepareEntities(
        sourceId: String,
        resourceKey: String,
        entities: List<T>?,
        validators: HttpValidators,
        previous: Map<String, CatalogSyncMetadataEntity>,
    ): PreparedEntities<T> {
        val old = previous[resourceKey]
        val fingerprint = entities?.let(CatalogFingerprint::of) ?: old?.contentFingerprint
            ?: throw java.io.IOException("Provider returned not modified without cached $resourceKey metadata")
        return PreparedEntities(
            changed = entities?.takeIf { fingerprint != old?.contentFingerprint },
            metadata = metadata(sourceId, resourceKey, validators, fingerprint),
        )
    }

    private fun metadata(
        sourceId: String,
        resourceKey: String,
        validators: HttpValidators,
        fingerprint: String?,
    ) = CatalogSyncMetadataEntity(
        sourceId = sourceId,
        resourceKey = resourceKey,
        etag = validators.etag,
        lastModified = validators.lastModified,
        contentFingerprint = fingerprint,
        lastCheckedAtEpochMs = System.currentTimeMillis(),
    )

    private suspend fun currentCount(sourceId: String, type: ContentCount): Int = when (type) {
        ContentCount.LIVE -> catalogDaoFacade.channels.countBySource(sourceId)
        ContentCount.MOVIES -> catalogDaoFacade.movies.countBySource(sourceId)
        ContentCount.SERIES -> catalogDaoFacade.series.countBySource(sourceId)
    }

    private suspend fun markFailure(sourceId: String, message: String, status: SourceStatus) {
        val existing = catalogDaoFacade.syncState.get(sourceId)
        catalogDaoFacade.syncState.upsert(
            (existing ?: LocalSourceSyncStateEntity(sourceId, null)).copy(lastError = message),
        )
        updateRemoteStatus(sourceId, status, existing?.lastSyncedAtEpochMs?.let(Instant::ofEpochMilli))
    }

    private suspend fun updateRemoteStatus(sourceId: String, status: SourceStatus, lastSyncedAt: Instant?) {
        runCatching { playlistSourcesRepository.updateSyncStatus(sourceId, status, lastSyncedAt) }
    }

    private fun appendProgress(step: XtreamSyncStep, isSuccess: Boolean = true, message: String? = null) {
        _progress.value += XtreamSyncProgress(step, isSuccess, message)
    }

    private fun Map<String, CatalogSyncMetadataEntity>.validators(resourceKey: String): HttpValidators =
        this[resourceKey]?.let { HttpValidators(it.etag, it.lastModified) } ?: HttpValidators()

    private data class PreparedCategories(
        val effective: List<LocalCategoryEntity>,
        val changed: List<LocalCategoryEntity>?,
        val metadata: CatalogSyncMetadataEntity,
    )

    private data class PreparedEntities<T>(
        val changed: List<T>?,
        val metadata: CatalogSyncMetadataEntity,
    )

    private enum class ContentCount { LIVE, MOVIES, SERIES }
}
