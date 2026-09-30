package com.iptvcinema.tv.features.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.PlaylistSourceItem
import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.xtream.XtreamAuthResult
import com.iptvcinema.tv.core.xtream.XtreamRepository
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressMapper
import com.iptvcinema.tv.core.catalog.CatalogSyncCoordinator
import com.iptvcinema.tv.core.catalog.CatalogSyncCoordinatorResult
import com.iptvcinema.tv.core.catalog.CatalogSyncMessageFormatter
import com.iptvcinema.tv.core.catalog.CatalogSyncTrigger
import com.iptvcinema.tv.core.sync.CatalogSyncScheduler
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.m3u.M3uSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SourcesUiState {
    data object Loading : SourcesUiState
    data class Ready(val sources: List<PlaylistSourceItem>) : SourcesUiState
    data class Error(val message: String) : SourcesUiState
}

data class XtreamConnectUiState(
    val isConnecting: Boolean = false,
    val checklist: List<Pair<String, Boolean>> = emptyList(),
    val errorMessage: String? = null,
)

data class M3uConnectUiState(
    val isConnecting: Boolean = false,
    val checklist: List<Pair<String, Boolean>> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class SourceViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val playlistSourcesRepository: PlaylistSourcesRepository,
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
    private val xtreamRepository: XtreamRepository,
    private val xtreamSyncRepository: XtreamSyncRepository,
    private val m3uSyncRepository: M3uSyncRepository,
    private val localCredentialsStore: LocalCredentialsStore,
    private val catalogSyncCoordinator: CatalogSyncCoordinator,
    private val catalogSyncScheduler: CatalogSyncScheduler,
    private val appStrings: AppStrings,
    private val syncMessageFormatter: CatalogSyncMessageFormatter,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SourcesUiState>(SourcesUiState.Loading)
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    private val _xtreamConnectState = MutableStateFlow(XtreamConnectUiState())
    val xtreamConnectState: StateFlow<XtreamConnectUiState> = _xtreamConnectState.asStateFlow()

    private val _m3uConnectState = MutableStateFlow(M3uConnectUiState())
    val m3uConnectState: StateFlow<M3uConnectUiState> = _m3uConnectState.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun loadSources() {
        viewModelScope.launch {
            _uiState.value = SourcesUiState.Loading
            if (!authRepository.isConfigured()) {
                _uiState.value = SourcesUiState.Ready(emptyList())
                return@launch
            }
            runCatching {
                val sources = playlistSourcesRepository.getSources().map { it.toUiItem() }
                _uiState.value = SourcesUiState.Ready(sources)
            }.onFailure { error ->
                _uiState.value = SourcesUiState.Error(appStrings.get(R.string.source_error_load))
            }
        }
    }

    fun saveDemoSource(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (authRepository.isConfigured()) {
                runCatching {
                    val source = playlistSourcesRepository.saveDemoSource()
                    persistSource(source, isDemoMode = true, onComplete)
                }.onFailure { error ->
                    _uiState.value = SourcesUiState.Error(appStrings.get(R.string.source_error_save))
                }
            } else {
                appSessionRepository.setSource(
                    sourceId = "local-demo",
                    sourceType = SourceType.DEMO,
                    isDemoMode = true,
                )
                appSessionRepository.sessionState.first { it.hasSource }
                onComplete()
            }
        }
    }

    fun connectXtreamSource(
        credentials: XtreamCredentials,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _xtreamConnectState.value = XtreamConnectUiState(isConnecting = true)
            val authResult = xtreamRepository.validateAndAuthenticate(credentials)
            updateChecklistFromAuth(authResult)
            if (authResult !is XtreamAuthResult.Success) {
                // Server-provided text is often English or cryptic; show our own message.
                val message = appStrings.get(
                    when (authResult) {
                        is XtreamAuthResult.InvalidCredentials -> R.string.source_error_invalid_credentials
                        is XtreamAuthResult.Expired -> R.string.source_error_expired
                        is XtreamAuthResult.Unreachable -> R.string.source_error_unreachable
                        is XtreamAuthResult.Error ->
                            if (authResult.message == INVALID_SERVER_URL) {
                                R.string.source_error_invalid_url
                            } else {
                                R.string.source_error_generic
                            }
                        else -> R.string.source_error_generic
                    },
                )
                _xtreamConnectState.value = XtreamConnectUiState(
                    isConnecting = false,
                    checklist = _xtreamConnectState.value.checklist,
                    errorMessage = message,
                )
                return@launch
            }

            val source = if (authRepository.isConfigured()) {
                val existing = playlistSourcesRepository.findMatchingXtreamSource(credentials)
                if (existing != null) {
                    runCatching {
                        playlistSourcesRepository.activateXtreamSource(existing.id, credentials)
                    }.getOrElse { error ->
                        _xtreamConnectState.value = XtreamConnectUiState(
                            isConnecting = false,
                            checklist = _xtreamConnectState.value.checklist,
                            errorMessage = appStrings.get(R.string.source_error_save),
                        )
                        return@launch
                    }
                } else {
                    runCatching { playlistSourcesRepository.saveXtreamSource(credentials) }
                        .getOrElse { error ->
                            _xtreamConnectState.value = XtreamConnectUiState(
                                isConnecting = false,
                                checklist = _xtreamConnectState.value.checklist,
                                errorMessage = appStrings.get(R.string.source_error_save),
                            )
                            return@launch
                        }
                }
            } else {
                val sourceId = "local-xtream-${UUID.randomUUID()}"
                localCredentialsStore.saveXtreamCredentials(sourceId, credentials)
                PlaylistSourceRecord(
                    id = sourceId,
                    userId = "local",
                    name = credentials.accountName.ifBlank { "Xtream Codes" },
                    type = SourceType.XTREAM_CODES,
                    serverUrl = credentials.serverUrl,
                    playlistUrl = null,
                    epgUrl = null,
                    isActive = true,
                    status = com.iptvcinema.tv.core.model.SourceStatus.ACTIVE,
                    lastSyncedAt = null,
                )
            }

            persistSource(source, isDemoMode = false) {}
            val syncResult = catalogSyncCoordinator.sync(
                source.id,
                SourceType.XTREAM_CODES,
                CatalogSyncTrigger.INITIAL_CONNECTION,
            )
            updateChecklistFromSync()
            when (syncResult) {
                is CatalogSyncCoordinatorResult.Success -> {
                    _xtreamConnectState.value = XtreamConnectUiState(
                        isConnecting = false,
                        checklist = buildFinalChecklist(syncResult),
                    )
                    loadSources()
                    onComplete()
                }
                is CatalogSyncCoordinatorResult.Failed -> {
                    _xtreamConnectState.value = XtreamConnectUiState(
                        isConnecting = false,
                        checklist = _xtreamConnectState.value.checklist,
                        errorMessage = syncMessageFormatter.failure(syncResult),
                    )
                }
            }
        }
    }

    fun saveXtreamSource(
        credentials: XtreamCredentials,
        onComplete: () -> Unit,
    ) {
        connectXtreamSource(credentials, onComplete)
    }

    fun resyncSource(sourceId: String) {
        viewModelScope.launch {
            val sourceType = runCatching {
                playlistSourcesRepository.getSources().firstOrNull { it.id == sourceId }?.type
            }.getOrNull() ?: when {
                localCredentialsStore.getM3uCredentials(sourceId) != null -> SourceType.M3U
                localCredentialsStore.getXtreamCredentials(sourceId) != null -> SourceType.XTREAM_CODES
                else -> null
            }

            when (sourceType) {
                SourceType.M3U -> {
                    val credentials = localCredentialsStore.getM3uCredentials(sourceId)
                        ?: run {
                            _syncMessage.value = appStrings.get(R.string.refresh_credentials_missing)
                            return@launch
                        }
                    _syncMessage.value = appStrings.get(R.string.refresh_syncing_source, credentials.playlistName)
                    val result = catalogSyncCoordinator.sync(sourceId, SourceType.M3U, CatalogSyncTrigger.MANUAL)
                    _syncMessage.value = when (result) {
                        is CatalogSyncCoordinatorResult.Success -> formatChangeMessage(result)
                        is CatalogSyncCoordinatorResult.Failed -> syncMessageFormatter.failure(result)
                    }
                }
                SourceType.XTREAM_CODES -> {
                    val credentials = localCredentialsStore.getXtreamCredentials(sourceId)
                        ?: run {
                            _syncMessage.value = appStrings.get(R.string.refresh_credentials_missing)
                            return@launch
                        }
                    _syncMessage.value = appStrings.get(R.string.refresh_syncing_source, credentials.accountName)
                    val result = catalogSyncCoordinator.sync(sourceId, SourceType.XTREAM_CODES, CatalogSyncTrigger.MANUAL)
                    _syncMessage.value = when (result) {
                        is CatalogSyncCoordinatorResult.Success -> formatChangeMessage(result)
                        is CatalogSyncCoordinatorResult.Failed -> syncMessageFormatter.failure(result)
                    }
                }
                else -> _syncMessage.value = appStrings.get(R.string.refresh_credentials_missing)
            }
            loadSources()
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun saveM3uSource(
        credentials: M3uCredentials,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _m3uConnectState.value = M3uConnectUiState(isConnecting = true)

            val source = if (!authRepository.isConfigured()) {
                val sourceId = "local-m3u-${UUID.randomUUID()}"
                localCredentialsStore.saveM3uCredentials(sourceId, credentials)
                PlaylistSourceRecord(
                    id = sourceId,
                    userId = "local",
                    name = credentials.playlistName.ifBlank { "M3U Playlist" },
                    type = SourceType.M3U,
                    serverUrl = null,
                    playlistUrl = credentials.playlistUrl,
                    epgUrl = credentials.epgUrl,
                    isActive = true,
                    status = com.iptvcinema.tv.core.model.SourceStatus.ACTIVE,
                    lastSyncedAt = null,
                )
            } else {
                runCatching { playlistSourcesRepository.saveM3uSource(credentials) }
                    .getOrElse { error ->
                        _m3uConnectState.value = M3uConnectUiState(
                            isConnecting = false,
                            errorMessage = appStrings.get(R.string.source_error_save),
                        )
                        return@launch
                    }
            }

            persistSource(source, isDemoMode = false) {}
            val syncResult = catalogSyncCoordinator.sync(
                source.id,
                SourceType.M3U,
                CatalogSyncTrigger.INITIAL_CONNECTION,
            )
            updateChecklistFromM3uSync()

            when (syncResult) {
                is CatalogSyncCoordinatorResult.Success -> {
                    _m3uConnectState.value = M3uConnectUiState(
                        isConnecting = false,
                        checklist = buildM3uFinalChecklist(syncResult),
                    )
                    loadSources()
                    onComplete()
                }
                is CatalogSyncCoordinatorResult.Failed -> {
                    val message = syncMessageFormatter.failure(syncResult)
                    _m3uConnectState.value = M3uConnectUiState(
                        isConnecting = false,
                        checklist = _m3uConnectState.value.checklist,
                        errorMessage = message,
                    )
                }
            }
        }
    }

    private val deletingSources = mutableSetOf<String>()

    fun setActiveSource(sourceId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                playlistSourcesRepository.setActiveSource(sourceId)
                val source = playlistSourcesRepository.getSources().first { it.id == sourceId }
                appSessionRepository.setSource(
                    sourceId = source.id,
                    sourceType = source.type,
                    isDemoMode = source.type == SourceType.DEMO,
                )
                if (source.type != SourceType.DEMO) catalogSyncScheduler.enqueueStartupCheck()
                loadSources()
                onComplete()
            }.onFailure { error ->
                _uiState.value = SourcesUiState.Error(appStrings.get(R.string.source_error_save))
            }
        }
    }

    /**
     * Deletes a playlist. When it was the active one, the next playlist becomes active (or none
     * is), so the app never keeps browsing and refreshing a playlist that no longer exists.
     * A second press while the first delete runs is ignored.
     */
    fun deleteSource(sourceId: String) {
        if (!deletingSources.add(sourceId)) return
        viewModelScope.launch {
            try {
                playlistSourcesRepository.deleteSource(sourceId)
                catalogRepository.purgeSource(sourceId)
                if (appSessionRepository.sessionState.first().currentSourceId == sourceId) {
                    val next = playlistSourcesRepository.getSources().firstOrNull { it.id != sourceId }
                    if (next == null) {
                        appSessionRepository.clearSource()
                    } else {
                        playlistSourcesRepository.setActiveSource(next.id)
                        appSessionRepository.setSource(
                            sourceId = next.id,
                            sourceType = next.type,
                            isDemoMode = next.type == SourceType.DEMO,
                        )
                        if (next.type != SourceType.DEMO) catalogSyncScheduler.enqueueStartupCheck()
                    }
                }
                loadSources()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = SourcesUiState.Error(appStrings.get(R.string.source_error_save))
            } finally {
                deletingSources.remove(sourceId)
            }
        }
    }

    private suspend fun persistSource(
        source: PlaylistSourceRecord,
        isDemoMode: Boolean,
        onComplete: () -> Unit,
    ) {
        appSessionRepository.setSource(
            sourceId = source.id,
            sourceType = source.type,
            isDemoMode = isDemoMode,
        )
        appSessionRepository.sessionState.first { it.hasSource && it.currentSourceId == source.id }
        onComplete()
    }

    private suspend fun PlaylistSourceRecord.toUiItem(): PlaylistSourceItem {
        val syncState = when (type) {
            SourceType.M3U -> m3uSyncRepository.getSyncState(id)
            else -> xtreamSyncRepository.getSyncState(id)
        }
        return PlaylistSourceItem(
            id = id,
            name = name,
            type = type,
            status = status,
            channelCount = syncState?.liveChannelCount,
            movieCount = syncState?.movieCount,
            seriesCount = syncState?.seriesCount,
            lastSynced = formatLastSynced(syncState?.lastSyncedAtEpochMs, lastSyncedAt),
            epgAvailable = syncState?.epgAvailable ?: (epgUrl != null),
        )
    }

    private fun formatLastSynced(localEpochMs: Long?, remoteSyncedAt: Instant?): String {
        localEpochMs?.let {
            return DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(it))
        }
        return remoteSyncedAt?.let {
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(it)
        } ?: appStrings.get(R.string.source_never_synced)
    }

    private fun updateChecklistFromAuth(result: XtreamAuthResult) {
        val serverReachable = when (result) {
            is XtreamAuthResult.Success,
            is XtreamAuthResult.InvalidCredentials,
            is XtreamAuthResult.Expired,
            -> true
            is XtreamAuthResult.Unreachable -> false
            is XtreamAuthResult.Error -> result.message != INVALID_SERVER_URL
        }
        _xtreamConnectState.value = XtreamConnectUiState(
            isConnecting = true,
            checklist = listOf(
                appStrings.get(R.string.source_check_server) to serverReachable,
                appStrings.get(R.string.source_check_auth) to (result is XtreamAuthResult.Success),
            ),
        )
    }

    private fun updateChecklistFromSync() {
        val progress = xtreamSyncRepository.progress.value
        val checklist = progress.map { item ->
            CatalogSyncProgressMapper.xtreamChecklistLabel(item) to item.isSuccess
        }
        _xtreamConnectState.value = _xtreamConnectState.value.copy(checklist = checklist)
    }

    private fun buildFinalChecklist(result: CatalogSyncCoordinatorResult.Success): List<Pair<String, Boolean>> {
        return listOf(
            "Server reachable" to true,
            "Authentication" to true,
            "Live channels" to (result.liveChannelCount > 0),
            "Movies" to (result.movieCount > 0),
            "Series" to (result.seriesCount > 0),
            "Sync complete" to true,
        )
    }

    private fun updateChecklistFromM3uSync() {
        val progress = m3uSyncRepository.progress.value
        val checklist = progress.map { item ->
            CatalogSyncProgressMapper.m3uChecklistLabel(item) to item.isSuccess
        }
        _m3uConnectState.value = _m3uConnectState.value.copy(checklist = checklist)
    }

    private fun buildM3uFinalChecklist(result: CatalogSyncCoordinatorResult.Success): List<Pair<String, Boolean>> {
        return listOf(
            "Playlist URL valid" to true,
            "Playlist downloaded" to true,
            "Channels parsed" to (result.liveChannelCount > 0),
            "EPG" to result.epgAvailable,
            "Sync complete" to true,
        )
    }

    private fun formatChangeMessage(result: CatalogSyncCoordinatorResult.Success): String =
        if (result.changes.hasChanges) {
            appStrings.get(
                R.string.refresh_catalog_changes,
                result.changes.added,
                result.changes.updated,
                result.changes.removed,
            )
        } else {
            appStrings.get(R.string.refresh_catalog_up_to_date)
        }
}

// Message XtreamUrlNormalizer throws for a malformed server address.
private const val INVALID_SERVER_URL = "Invalid server URL"
