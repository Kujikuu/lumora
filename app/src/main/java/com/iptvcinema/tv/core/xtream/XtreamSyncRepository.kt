package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import com.iptvcinema.tv.core.epg.EpgSyncRepository
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository
import com.iptvcinema.tv.core.player.WatchedSeriesEpisodePrefetcher
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    data class Success(val liveChannelCount: Int, val movieCount: Int, val seriesCount: Int) : XtreamSyncResult()
    data class AuthFailed(val message: String, val status: SourceStatus) : XtreamSyncResult()
    data class Failed(val message: String) : XtreamSyncResult()
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

    suspend fun hasCachedCatalog(sourceId: String): Boolean =
        hasCachedCatalogData(
            syncState = catalogDaoFacade.syncState.get(sourceId),
            channelRowCount = catalogDaoFacade.channels.countBySource(sourceId),
        )

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

    suspend fun syncSourceIfNeeded(
        sourceId: String,
        credentials: XtreamCredentials,
        force: Boolean = false,
    ): XtreamSyncResult {
        if (!force && hasCachedCatalog(sourceId)) {
            return cachedSuccessResult(sourceId)
        }
        return syncSource(sourceId, credentials)
    }

    suspend fun syncSource(sourceId: String, credentials: XtreamCredentials): XtreamSyncResult =
        performSync(sourceId, credentials)

    private suspend fun performSync(
        sourceId: String,
        credentials: XtreamCredentials,
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
            updateRemoteStatus(sourceId, status, null)
            return XtreamSyncResult.AuthFailed(message, status)
        }
        appendProgress(XtreamSyncStep.AUTHENTICATING, message = "Authenticated")

        return runCatching {
            val liveCategories = xtreamRepository.fetchLiveCategories(credentials)
            appendProgress(XtreamSyncStep.LIVE_CATEGORIES, message = "${liveCategories.size} categories")
            val liveCategoryEntities = XtreamNormalizer.normalizeLiveCategories(sourceId, liveCategories).first
            val liveCategoryNames = liveCategoryEntities.associate { it.id to it.name }

            val liveStreams = xtreamRepository.fetchLiveStreams(credentials)
            appendProgress(XtreamSyncStep.LIVE_STREAMS, message = "${liveStreams.size} channels")
            val liveChannelEntities = XtreamNormalizer.normalizeLiveStreams(
                sourceId = sourceId,
                credentials = credentials,
                serverUrl = serverUrl,
                dtos = liveStreams,
                categoryNames = liveCategoryNames,
            )
            catalogDaoFacade.replaceLiveCatalog(sourceId, liveCategoryEntities, liveChannelEntities)

            val vodCategories = xtreamRepository.fetchVodCategories(credentials)
            appendProgress(XtreamSyncStep.VOD_CATEGORIES, message = "${vodCategories.size} categories")
            val vodCategoryEntities = XtreamNormalizer.normalizeVodCategories(sourceId, vodCategories)
            val vodCategoryNames = vodCategoryEntities.associate { it.id to it.name }

            val vodStreams = xtreamRepository.fetchVodStreams(credentials)
            appendProgress(XtreamSyncStep.VOD_STREAMS, message = "${vodStreams.size} movies")
            val movieEntities = XtreamNormalizer.normalizeVodStreams(
                sourceId = sourceId,
                credentials = credentials,
                serverUrl = serverUrl,
                dtos = vodStreams,
                categoryNames = vodCategoryNames,
            )
            catalogDaoFacade.replaceVodCatalog(sourceId, vodCategoryEntities, movieEntities)

            val seriesCategories = xtreamRepository.fetchSeriesCategories(credentials)
            appendProgress(XtreamSyncStep.SERIES_CATEGORIES, message = "${seriesCategories.size} categories")
            val seriesCategoryEntities = XtreamNormalizer.normalizeSeriesCategories(sourceId, seriesCategories)
            val seriesCategoryNames = seriesCategoryEntities.associate { it.id to it.name }

            val seriesItems = xtreamRepository.fetchSeries(credentials)
            appendProgress(XtreamSyncStep.SERIES, message = "${seriesItems.size} series")
            val seriesEntities = XtreamNormalizer.normalizeSeries(
                sourceId = sourceId,
                dtos = seriesItems,
                categoryNames = seriesCategoryNames,
            )
            catalogDaoFacade.replaceSeriesCatalog(sourceId, seriesCategoryEntities, seriesEntities)

            runCatching {
                episodeCatalogRepository.prefetchTopSeriesEpisodes(sourceId, limit = 5)
            }

            val prefetchedSeriesCount = runCatching {
                watchedSeriesEpisodePrefetcher.prefetchForCurrentSession()
            }.getOrDefault(0)
            appendProgress(
                XtreamSyncStep.WATCHED_SERIES_EPISODES,
                message = if (prefetchedSeriesCount > 0) {
                    "$prefetchedSeriesCount watched series"
                } else {
                    "No watched series to prefetch"
                },
            )

            var epgAvailable = catalogDaoFacade.syncState.get(sourceId)?.epgAvailable ?: false

            val liveCount = catalogDaoFacade.channels.countBySource(sourceId)
            val movieCount = catalogDaoFacade.movies.countBySource(sourceId)
            val seriesCount = catalogDaoFacade.series.countBySource(sourceId)
            val syncedAt = Instant.now()
            val syncedAtMs = syncedAt.toEpochMilli()

            catalogDaoFacade.syncState.upsert(
                LocalSourceSyncStateEntity(
                    sourceId = sourceId,
                    lastSyncedAtEpochMs = syncedAtMs,
                    liveChannelCount = liveCount,
                    movieCount = movieCount,
                    seriesCount = seriesCount,
                    epgAvailable = epgAvailable,
                    lastError = null,
                ),
            )
            updateRemoteStatus(sourceId, SourceStatus.ACTIVE, syncedAt)
            appendProgress(XtreamSyncStep.COMPLETE, message = "Sync complete")

            epgSyncRepository.syncEpgInBackground(
                sourceId = sourceId,
                channels = liveChannelEntities,
                fetchXml = { xtreamRepository.fetchXmltv(credentials) },
            ) { outcome ->
                appendProgress(
                    XtreamSyncStep.EPG,
                    isSuccess = outcome.isSuccess,
                    message = outcome.message,
                )
            }

            XtreamSyncResult.Success(liveCount, movieCount, seriesCount)
        }.getOrElse { error ->
            val message = error.message ?: "Sync failed"
            markFailure(sourceId, message, SourceStatus.FAILED)
            updateRemoteStatus(sourceId, SourceStatus.FAILED, null)
            XtreamSyncResult.Failed(message)
        }
    }

    suspend fun getSyncState(sourceId: String): LocalSourceSyncStateEntity? =
        catalogDaoFacade.syncState.get(sourceId)

    private suspend fun cachedSuccessResult(sourceId: String): XtreamSyncResult {
        val syncState = catalogDaoFacade.syncState.get(sourceId)
        val liveCount = syncState?.liveChannelCount ?: catalogDaoFacade.channels.countBySource(sourceId)
        val movieCount = syncState?.movieCount ?: catalogDaoFacade.movies.countBySource(sourceId)
        val seriesCount = syncState?.seriesCount ?: catalogDaoFacade.series.countBySource(sourceId)
        return XtreamSyncResult.Success(liveCount, movieCount, seriesCount)
    }

    private suspend fun markFailure(sourceId: String, message: String, status: SourceStatus) {
        val existing = catalogDaoFacade.syncState.get(sourceId)
        catalogDaoFacade.syncState.upsert(
            (existing ?: LocalSourceSyncStateEntity(sourceId = sourceId, lastSyncedAtEpochMs = null))
                .copy(lastError = message),
        )
        updateRemoteStatus(sourceId, status, existing?.lastSyncedAtEpochMs?.let(Instant::ofEpochMilli))
    }

    private suspend fun updateRemoteStatus(
        sourceId: String,
        status: SourceStatus,
        lastSyncedAt: Instant?,
    ) {
        runCatching {
            playlistSourcesRepository.updateSyncStatus(sourceId, status, lastSyncedAt)
        }
    }

    private fun appendProgress(step: XtreamSyncStep, isSuccess: Boolean = true, message: String? = null) {
        _progress.value = _progress.value + XtreamSyncProgress(step = step, isSuccess = isSuccess, message = message)
    }
}
