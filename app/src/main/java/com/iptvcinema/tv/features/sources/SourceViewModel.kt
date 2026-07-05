package com.iptvcinema.tv.features.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.iptvcinema.tv.core.xtream.XtreamSyncProgress
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository
import com.iptvcinema.tv.core.xtream.XtreamSyncResult
import com.iptvcinema.tv.core.xtream.XtreamSyncStep
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressMapper
import com.iptvcinema.tv.core.m3u.M3uSyncRepository
import com.iptvcinema.tv.core.m3u.M3uSyncResult
import com.iptvcinema.tv.core.m3u.M3uSyncStep
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
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
                _uiState.value = SourcesUiState.Error(error.message ?: "Unable to load sources")
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
                    _uiState.value = SourcesUiState.Error(error.message ?: "Unable to save demo source")
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
                val message = when (authResult) {
                    is XtreamAuthResult.InvalidCredentials -> authResult.message
                    is XtreamAuthResult.Expired -> authResult.message
                    is XtreamAuthResult.Unreachable -> authResult.message
                    is XtreamAuthResult.Error -> authResult.message
                    else -> "Authentication failed"
                }
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
                            errorMessage = error.message ?: "Unable to activate source",
                        )
                        return@launch
                    }
                } else {
                    runCatching { playlistSourcesRepository.saveXtreamSource(credentials) }
                        .getOrElse { error ->
                            _xtreamConnectState.value = XtreamConnectUiState(
                                isConnecting = false,
                                checklist = _xtreamConnectState.value.checklist,
                                errorMessage = error.message ?: "Unable to save source",
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
            val syncResult = xtreamSyncRepository.syncSourceIfNeeded(source.id, credentials)
            updateChecklistFromSync(syncResult)
            when (syncResult) {
                is XtreamSyncResult.Success -> {
                    _xtreamConnectState.value = XtreamConnectUiState(
                        isConnecting = false,
                        checklist = buildFinalChecklist(syncResult),
                    )
                    loadSources()
                    onComplete()
                }
                is XtreamSyncResult.AuthFailed -> {
                    _xtreamConnectState.value = XtreamConnectUiState(
                        isConnecting = false,
                        checklist = _xtreamConnectState.value.checklist,
                        errorMessage = syncResult.message,
                    )
                }
                is XtreamSyncResult.Failed -> {
                    _xtreamConnectState.value = XtreamConnectUiState(
                        isConnecting = false,
                        checklist = _xtreamConnectState.value.checklist,
                        errorMessage = syncResult.message,
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
                            _syncMessage.value = "Credentials not found for this source"
                            return@launch
                        }
                    _syncMessage.value = "Syncing ${credentials.playlistName}…"
                    val result = m3uSyncRepository.syncSource(sourceId, credentials)
                    _syncMessage.value = when (result) {
                        is M3uSyncResult.Success ->
                            "Synced ${result.liveChannelCount} channels" +
                                if (result.epgAvailable) " with EPG" else ""
                        is M3uSyncResult.Unreachable -> result.message
                        is M3uSyncResult.Failed -> result.message
                    }
                }
                SourceType.XTREAM_CODES -> {
                    val credentials = localCredentialsStore.getXtreamCredentials(sourceId)
                        ?: run {
                            _syncMessage.value = "Credentials not found for this source"
                            return@launch
                        }
                    _syncMessage.value = "Syncing ${credentials.accountName}…"
                    val result = xtreamSyncRepository.syncSource(sourceId, credentials)
                    _syncMessage.value = when (result) {
                        is XtreamSyncResult.Success ->
                            "Synced ${result.liveChannelCount} channels, ${result.movieCount} movies, ${result.seriesCount} series"
                        is XtreamSyncResult.AuthFailed -> result.message
                        is XtreamSyncResult.Failed -> result.message
                    }
                }
                else -> _syncMessage.value = "Credentials not found for this source"
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
                            errorMessage = error.message ?: "Unable to save M3U source",
                        )
                        return@launch
                    }
            }

            persistSource(source, isDemoMode = false) {}
            val syncResult = m3uSyncRepository.syncSourceIfNeeded(source.id, credentials)
            updateChecklistFromM3uSync(syncResult)

            when (syncResult) {
                is M3uSyncResult.Success -> {
                    _m3uConnectState.value = M3uConnectUiState(
                        isConnecting = false,
                        checklist = buildM3uFinalChecklist(syncResult),
                    )
                    loadSources()
                    onComplete()
                }
                is M3uSyncResult.Unreachable,
                is M3uSyncResult.Failed,
                -> {
                    val message = when (syncResult) {
                        is M3uSyncResult.Unreachable -> syncResult.message
                        is M3uSyncResult.Failed -> syncResult.message
                        else -> "Import failed"
                    }
                    _m3uConnectState.value = M3uConnectUiState(
                        isConnecting = false,
                        checklist = _m3uConnectState.value.checklist,
                        errorMessage = message,
                    )
                }
            }
        }
    }

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
                loadSources()
                onComplete()
            }.onFailure { error ->
                _uiState.value = SourcesUiState.Error(error.message ?: "Unable to set active source")
            }
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            runCatching {
                playlistSourcesRepository.deleteSource(sourceId)
            }.onSuccess {
                catalogRepository.purgeSource(sourceId)
                loadSources()
            }.onFailure { error ->
                _uiState.value = SourcesUiState.Error(error.message ?: "Unable to delete source")
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
        } ?: "Never synced"
    }

    private fun updateChecklistFromAuth(result: XtreamAuthResult) {
        val serverReachable = when (result) {
            is XtreamAuthResult.Success,
            is XtreamAuthResult.InvalidCredentials,
            is XtreamAuthResult.Expired,
            -> true
            is XtreamAuthResult.Unreachable -> false
            is XtreamAuthResult.Error -> result.message != "Invalid server URL"
        }
        _xtreamConnectState.value = XtreamConnectUiState(
            isConnecting = true,
            checklist = listOf(
                "Server reachable" to serverReachable,
                "Authentication" to (result is XtreamAuthResult.Success),
            ),
        )
    }

    private fun updateChecklistFromSync(result: XtreamSyncResult) {
        val progress = xtreamSyncRepository.progress.value
        val checklist = progress.map { item ->
            CatalogSyncProgressMapper.xtreamChecklistLabel(item) to item.isSuccess
        }
        _xtreamConnectState.value = _xtreamConnectState.value.copy(checklist = checklist)
    }

    private fun buildFinalChecklist(result: XtreamSyncResult.Success): List<Pair<String, Boolean>> {
        return listOf(
            "Server reachable" to true,
            "Authentication" to true,
            "Live channels" to (result.liveChannelCount > 0),
            "Movies" to (result.movieCount > 0),
            "Series" to (result.seriesCount > 0),
            "Sync complete" to true,
        )
    }

    private fun updateChecklistFromM3uSync(result: M3uSyncResult) {
        val progress = m3uSyncRepository.progress.value
        val checklist = progress.map { item ->
            CatalogSyncProgressMapper.m3uChecklistLabel(item) to item.isSuccess
        }
        _m3uConnectState.value = _m3uConnectState.value.copy(checklist = checklist)
    }

    private fun buildM3uFinalChecklist(result: M3uSyncResult.Success): List<Pair<String, Boolean>> {
        return listOf(
            "Playlist URL valid" to true,
            "Playlist downloaded" to true,
            "Channels parsed" to (result.liveChannelCount > 0),
            "EPG" to result.epgAvailable,
            "Sync complete" to true,
        )
    }
}
