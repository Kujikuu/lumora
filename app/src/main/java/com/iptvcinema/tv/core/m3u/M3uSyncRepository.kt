package com.iptvcinema.tv.core.m3u

import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import com.iptvcinema.tv.core.epg.EpgSyncRepository
import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository
import java.net.URI
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class M3uSyncStep {
    VALIDATING_URL,
    DOWNLOADING,
    PARSING,
    NORMALIZING,
    EPG,
    COMPLETE,
}

data class M3uSyncProgress(
    val step: M3uSyncStep,
    val isSuccess: Boolean = true,
    val message: String? = null,
)

sealed class M3uSyncResult {
    data class Success(val liveChannelCount: Int, val epgAvailable: Boolean) : M3uSyncResult()
    data class Unreachable(val message: String) : M3uSyncResult()
    data class Failed(val message: String) : M3uSyncResult()
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

    suspend fun hasCachedCatalog(sourceId: String): Boolean =
        XtreamSyncRepository.hasCachedCatalogData(
            syncState = catalogDaoFacade.syncState.get(sourceId),
            channelRowCount = catalogDaoFacade.channels.countBySource(sourceId),
        )

    suspend fun syncSourceIfNeeded(
        sourceId: String,
        credentials: M3uCredentials,
        force: Boolean = false,
    ): M3uSyncResult {
        if (!force && hasCachedCatalog(sourceId)) {
            return cachedSuccessResult(sourceId)
        }
        return syncSource(sourceId, credentials)
    }

    suspend fun syncSource(sourceId: String, credentials: M3uCredentials): M3uSyncResult =
        performSync(sourceId, credentials)

    suspend fun getSyncState(sourceId: String): LocalSourceSyncStateEntity? =
        catalogDaoFacade.syncState.get(sourceId)

    private suspend fun performSync(
        sourceId: String,
        credentials: M3uCredentials,
    ): M3uSyncResult {
        _progress.value = emptyList()
        updateRemoteStatus(sourceId, SourceStatus.SYNCING, null)

        val playlistUrl = credentials.playlistUrl.trim()
        if (!isValidHttpUrl(playlistUrl)) {
            val message = "Invalid playlist URL"
            markFailure(sourceId, message, SourceStatus.FAILED)
            appendProgress(M3uSyncStep.VALIDATING_URL, isSuccess = false, message = message)
            return M3uSyncResult.Failed(message)
        }
        appendProgress(M3uSyncStep.VALIDATING_URL, message = "Playlist URL validated")

        val requestOptions = M3uRequestOptions(
            userAgent = credentials.userAgent,
            referer = credentials.referer,
            customHeaders = credentials.customHeaders,
        )

        val playlistContent = try {
            appendProgress(M3uSyncStep.DOWNLOADING, message = "Downloading playlist…")
            m3uDownloader.download(playlistUrl, requestOptions)
        } catch (error: M3uDownloadException.Unreachable) {
            val message = error.message ?: "Unable to reach playlist"
            markFailure(sourceId, message, SourceStatus.FAILED)
            appendProgress(M3uSyncStep.DOWNLOADING, isSuccess = false, message = message)
            return M3uSyncResult.Unreachable(message)
        } catch (error: M3uDownloadException) {
            val message = error.message ?: "Playlist download failed"
            markFailure(sourceId, message, SourceStatus.FAILED)
            appendProgress(M3uSyncStep.DOWNLOADING, isSuccess = false, message = message)
            return M3uSyncResult.Failed(message)
        }
        appendProgress(M3uSyncStep.DOWNLOADING, message = "Playlist downloaded")

        val entries = M3uParser.parse(playlistContent)
        if (entries.isEmpty()) {
            val message = "No valid channels found in playlist"
            markFailure(sourceId, message, SourceStatus.FAILED)
            appendProgress(M3uSyncStep.PARSING, isSuccess = false, message = message)
            return M3uSyncResult.Failed(message)
        }
        appendProgress(M3uSyncStep.PARSING, message = "${entries.size} channels parsed")

        return runCatching {
            val (categories, channels) = M3uNormalizer.normalizeLiveCatalog(sourceId, entries)
            catalogDaoFacade.replaceLiveCatalog(sourceId, categories, channels)
            appendProgress(M3uSyncStep.NORMALIZING, message = "${channels.size} channels saved")

            var epgAvailable = catalogDaoFacade.syncState.get(sourceId)?.epgAvailable ?: false
            val epgUrl = credentials.epgUrl?.trim().orEmpty()

            val liveCount = catalogDaoFacade.channels.countBySource(sourceId)
            val syncedAtMs = Instant.now().toEpochMilli()
            catalogDaoFacade.syncState.upsert(
                LocalSourceSyncStateEntity(
                    sourceId = sourceId,
                    lastSyncedAtEpochMs = syncedAtMs,
                    liveChannelCount = liveCount,
                    movieCount = 0,
                    seriesCount = 0,
                    epgAvailable = epgAvailable,
                    lastError = null,
                ),
            )
            updateRemoteStatus(sourceId, SourceStatus.ACTIVE, Instant.ofEpochMilli(syncedAtMs))
            appendProgress(M3uSyncStep.COMPLETE, message = "Sync complete")

            if (epgUrl.isNotBlank()) {
                epgSyncRepository.syncEpgInBackground(
                    sourceId = sourceId,
                    channels = channels,
                    fetchXml = { m3uDownloader.download(epgUrl, requestOptions) },
                ) { outcome ->
                    appendProgress(
                        M3uSyncStep.EPG,
                        isSuccess = outcome.isSuccess,
                        message = outcome.message,
                    )
                }
            } else {
                appendProgress(M3uSyncStep.EPG, message = "No EPG URL provided")
            }

            M3uSyncResult.Success(liveCount, epgAvailable)
        }.getOrElse { error ->
            val message = error.message ?: "Sync failed"
            markFailure(sourceId, message, SourceStatus.FAILED)
            updateRemoteStatus(sourceId, SourceStatus.FAILED, null)
            M3uSyncResult.Failed(message)
        }
    }

    private suspend fun cachedSuccessResult(sourceId: String): M3uSyncResult {
        val syncState = catalogDaoFacade.syncState.get(sourceId)
        val liveCount = syncState?.liveChannelCount ?: catalogDaoFacade.channels.countBySource(sourceId)
        return M3uSyncResult.Success(
            liveChannelCount = liveCount,
            epgAvailable = syncState?.epgAvailable ?: false,
        )
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

    private fun appendProgress(step: M3uSyncStep, isSuccess: Boolean = true, message: String? = null) {
        _progress.value = _progress.value + M3uSyncProgress(step = step, isSuccess = isSuccess, message = message)
    }

    private fun isValidHttpUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        val scheme = uri.scheme?.lowercase()
        scheme == "http" || scheme == "https"
    }.getOrDefault(false)
}
