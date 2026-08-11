package com.iptvcinema.tv.core.m3u

import com.iptvcinema.tv.core.catalog.CatalogChangeSummary
import com.iptvcinema.tv.core.catalog.CatalogFingerprint
import com.iptvcinema.tv.core.catalog.CatalogSyncResource
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.database.entity.CatalogSyncMetadataEntity
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import com.iptvcinema.tv.core.epg.EpgSyncRepository
import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.network.ConditionalFetchResult
import com.iptvcinema.tv.core.network.HttpValidators
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class M3uSyncStep { VALIDATING_URL, DOWNLOADING, PARSING, NORMALIZING, EPG, COMPLETE }

data class M3uSyncProgress(val step: M3uSyncStep, val isSuccess: Boolean = true, val message: String? = null)

sealed class M3uSyncResult {
    data class Success(
        val liveChannelCount: Int,
        val epgAvailable: Boolean,
        val changes: CatalogChangeSummary = CatalogChangeSummary(),
        val checkedProvider: Boolean = true,
    ) : M3uSyncResult()

    data class Unreachable(val message: String) : M3uSyncResult()
    data class Failed(val message: String, val retryable: Boolean = false) : M3uSyncResult()
}

@Singleton
class M3uSyncRepository @Inject constructor(
    private val m3uDownloader: M3uDownloader,
    private val catalogDaoFacade: CatalogDaoFacade,
    private val playlistSourcesRepository: PlaylistSourcesRepository,
    private val epgSyncRepository: EpgSyncRepository,
) {
    private val _progress = MutableStateFlow<List<M3uSyncProgress>>(emptyList())
    val progress: StateFlow<List<M3uSyncProgress>> = _progress.asStateFlow()

    suspend fun syncSource(
        sourceId: String,
        credentials: M3uCredentials,
        refreshEpgWhenUnchanged: Boolean = false,
        useConditionalRequests: Boolean = true,
        awaitEpgCompletion: Boolean = false,
    ): M3uSyncResult = performSync(
        sourceId,
        credentials,
        refreshEpgWhenUnchanged,
        useConditionalRequests,
        awaitEpgCompletion,
    )

    suspend fun getSyncState(sourceId: String): LocalSourceSyncStateEntity? = catalogDaoFacade.syncState.get(sourceId)

    private suspend fun performSync(
        sourceId: String,
        credentials: M3uCredentials,
        refreshEpgWhenUnchanged: Boolean,
        useConditionalRequests: Boolean,
        awaitEpgCompletion: Boolean,
    ): M3uSyncResult {
        _progress.value = emptyList()
        updateRemoteStatus(sourceId, SourceStatus.SYNCING, null)
        val playlistUrl = credentials.playlistUrl.trim()
        if (!isValidM3uHttpUrl(playlistUrl)) {
            val message = "Invalid playlist URL"
            markFailure(sourceId, message, SourceStatus.FAILED)
            appendProgress(M3uSyncStep.VALIDATING_URL, false, message)
            return M3uSyncResult.Failed(message)
        }
        appendProgress(M3uSyncStep.VALIDATING_URL, message = "Playlist URL validated")

        val options = M3uRequestOptions(credentials.userAgent, credentials.referer, credentials.customHeaders)
        val previous = catalogDaoFacade.syncMetadata.get(sourceId, CatalogSyncResource.M3U_PLAYLIST)
        val validators = if (useConditionalRequests) {
            previous?.let { HttpValidators(it.etag, it.lastModified) } ?: HttpValidators()
        } else {
            HttpValidators()
        }
        val response = try {
            appendProgress(M3uSyncStep.DOWNLOADING, message = "Downloading playlist…")
            m3uDownloader.downloadConditional(playlistUrl, options, validators)
        } catch (error: M3uDownloadException.Unreachable) {
            val message = error.message ?: "Unable to reach playlist"
            markFailure(sourceId, message, SourceStatus.NEEDS_ATTENTION)
            appendProgress(M3uSyncStep.DOWNLOADING, false, message)
            return M3uSyncResult.Unreachable(message)
        } catch (error: M3uDownloadException) {
            val message = error.message ?: "Playlist download failed"
            markFailure(sourceId, message, SourceStatus.FAILED)
            appendProgress(M3uSyncStep.DOWNLOADING, false, message)
            val retryable = error is M3uDownloadException.HttpError && (error.code == 408 || error.code == 429 || error.code >= 500)
            return M3uSyncResult.Failed(message, retryable)
        }

        return try {
            val now = System.currentTimeMillis()
            var normalizedCategories: List<com.iptvcinema.tv.core.database.entity.LocalCategoryEntity>? = null
            var normalizedChannels: List<com.iptvcinema.tv.core.database.entity.LocalChannelEntity>? = null
            val fingerprint: String?
            when (response) {
                is ConditionalFetchResult.Modified -> {
                    appendProgress(M3uSyncStep.DOWNLOADING, message = "Playlist downloaded")
                    val entries = M3uParser.parse(response.body)
                    if (entries.isEmpty()) throw M3uDownloadException.EmptyResponse("No valid channels found in playlist")
                    appendProgress(M3uSyncStep.PARSING, message = "${entries.size} channels parsed")
                    val (categories, channels) = M3uNormalizer.normalizeLiveCatalog(sourceId, entries)
                    val nextFingerprint = CatalogFingerprint.of(categories + channels)
                    fingerprint = nextFingerprint
                    if (nextFingerprint != previous?.contentFingerprint) {
                        normalizedCategories = categories
                        normalizedChannels = channels
                    }
                }
                is ConditionalFetchResult.NotModified -> {
                    fingerprint = previous?.contentFingerprint
                        ?: throw M3uDownloadException.EmptyResponse(
                            "Provider returned not modified without cached playlist metadata",
                        )
                    appendProgress(M3uSyncStep.DOWNLOADING, message = "Playlist unchanged")
                    appendProgress(M3uSyncStep.PARSING, message = "Cached playlist is current")
                }
            }

            val metadata = CatalogSyncMetadataEntity(
                sourceId = sourceId,
                resourceKey = CatalogSyncResource.M3U_PLAYLIST,
                etag = response.validators.etag,
                lastModified = response.validators.lastModified,
                contentFingerprint = fingerprint,
                lastCheckedAtEpochMs = now,
            )
            val syncedAt = Instant.ofEpochMilli(now)
            val reconciliation = catalogDaoFacade.reconcileCatalog(
                sourceId,
                normalizedCategories,
                normalizedChannels,
                null,
                null,
                null,
                null,
                listOf(metadata),
                syncedAt.toEpochMilli(),
            )
            val changes = reconciliation.changes
            appendProgress(M3uSyncStep.NORMALIZING, message = if (changes.hasChanges) "${changes.totalChanges} changes saved" else "Catalog up to date")

            updateRemoteStatus(sourceId, SourceStatus.ACTIVE, syncedAt)
            appendProgress(M3uSyncStep.COMPLETE, message = "Sync complete")

            currentCoroutineContext().ensureActive()
            val epgUrl = credentials.epgUrl?.trim().orEmpty()
            if (epgUrl.isNotBlank() && (changes.hasChanges || refreshEpgWhenUnchanged)) {
                val channelsForEpg = normalizedChannels ?: catalogDaoFacade.channels.getAllOrdered(sourceId)
                val onComplete: suspend (com.iptvcinema.tv.core.epg.EpgSyncOutcome) -> Unit = { outcome ->
                    appendProgress(M3uSyncStep.EPG, outcome.isSuccess, outcome.message)
                }
                if (awaitEpgCompletion) {
                    epgSyncRepository.syncEpgAndWait(
                        sourceId = sourceId,
                        channels = channelsForEpg,
                        fetchXml = { m3uDownloader.download(epgUrl, options) },
                        onComplete = onComplete,
                    )
                } else {
                    epgSyncRepository.syncEpgInBackground(
                        sourceId = sourceId,
                        channels = channelsForEpg,
                        fetchXml = { m3uDownloader.download(epgUrl, options) },
                        onComplete = onComplete,
                    )
                }
            } else if (epgUrl.isBlank()) {
                appendProgress(M3uSyncStep.EPG, message = "No EPG URL provided")
            }
            M3uSyncResult.Success(
                liveChannelCount = reconciliation.liveChannelCount,
                epgAvailable = reconciliation.epgAvailable,
                changes = changes,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val message = error.message ?: "Sync failed"
            markFailure(sourceId, message, SourceStatus.FAILED)
            M3uSyncResult.Failed(message, retryable = isRetryable(error))
        }
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

    private fun appendProgress(step: M3uSyncStep, isSuccess: Boolean = true, message: String? = null) {
        _progress.value += M3uSyncProgress(step, isSuccess, message)
    }

    private fun isRetryable(error: Throwable): Boolean = when (error) {
        is M3uDownloadException.Unreachable -> true
        is M3uDownloadException.HttpError -> error.code == 408 || error.code == 429 || error.code >= 500
        else -> false
    }
}
