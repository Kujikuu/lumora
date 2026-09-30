package com.iptvcinema.tv.features.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.CloudAccountRetryCoordinator
import com.iptvcinema.tv.core.data.repository.CloudAccountStatus
import com.iptvcinema.tv.core.data.repository.SessionTeardown
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.device.DeviceIdentity
import com.iptvcinema.tv.core.parental.PinCheck
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.player.StreamingQualityOption
import com.iptvcinema.tv.core.util.safeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val authRepository: AuthRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val catalogRefreshController: CatalogRefreshController,
    private val catalogSyncProgressTracker: CatalogSyncProgressTracker,
    private val sessionTeardown: SessionTeardown,
    private val cloudAccountStatus: CloudAccountStatus,
    private val cloudAccountRetryCoordinator: CloudAccountRetryCoordinator,
    deviceIdentity: DeviceIdentity,
) : ViewModel() {
    val sessionState: StateFlow<AppSessionState> = appSessionRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSessionState(),
        )

    private val _account = MutableStateFlow(SettingsAccountUi(deviceName = deviceIdentity.deviceName()))
    val account: StateFlow<SettingsAccountUi> = _account.asStateFlow()

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val isCloudDegraded: StateFlow<Boolean> = cloudAccountStatus.isDegraded
    val isCloudWriteDegraded: StateFlow<Boolean> = cloudAccountStatus.isWriteDegraded
    val lastSyncedAt: StateFlow<Instant?> = cloudAccountStatus.lastSyncedAt

    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()

    private val _parentalControls = MutableStateFlow<ParentalControls?>(null)
    val parentalControls: StateFlow<ParentalControls?> = _parentalControls.asStateFlow()
    private val _parentalControlsLoaded = MutableStateFlow(false)

    private val _refreshState = MutableStateFlow<CatalogRefreshState>(CatalogRefreshState.Idle)
    val refreshState: StateFlow<CatalogRefreshState> = _refreshState.asStateFlow()

    val parentalGateInstance: ParentalGate get() = parentalGate

    init {
        viewModelScope.launch {
            userSettingsRepository.observeSettings().collect { settings ->
                _userSettings.value = settings
            }
        }
        // This view model is activity scoped, so it outlives a sign-out. Reload whenever the
        // account or profile changes, and drop the previous account's data first.
        viewModelScope.launch {
            appSessionRepository.sessionState
                .distinctUntilChangedBy { it.userId to it.currentProfileId }
                .collect { session ->
                    _parentalControls.value = null
                    _parentalControlsLoaded.value = false
                    if (session.isAuthenticated) {
                        loadAccountAndParentalControls()
                    } else {
                        // Guest profiles keep parental controls on the device; they apply too.
                        clearAccount()
                        viewModelScope.launch { loadParentalControls() }
                    }
                }
        }
    }

    fun loadAccountAndParentalControls() {
        viewModelScope.launch {
            _account.value = _account.value.copy(
                displayName = authRepository.currentUserDisplayName(),
                email = authRepository.currentUserEmail(),
                isCloudAccount = authRepository.isConfigured(),
            )
            loadParentalControls()
        }
    }

    private suspend fun loadParentalControls() {
        val profileId = appSessionRepository.sessionState.first().currentProfileId
        if (profileId == null) {
            // No profile, nothing to protect.
            _parentalControls.value = null
            _parentalControlsLoaded.value = true
            return
        }
        try {
            _parentalControls.value = parentalControlsRepository.getControls(profileId)
            _parentalControlsLoaded.value = true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Left unloaded: protected settings keep asking for the PIN until a reload works.
            Log.w(TAG, "Parental controls failed to load: ${error.safeSummary()}")
        }
    }

    private fun clearAccount() {
        _account.value = SettingsAccountUi(deviceName = _account.value.deviceName)
    }

    /**
     * Checks a PIN for protected settings. Fails closed when parental controls could not be
     * loaded, and locks out after repeated wrong tries.
     */
    fun checkParentalPin(pin: String): PinCheck {
        val controls = _parentalControls.value.takeIf { _parentalControlsLoaded.value }
        return parentalGate.checkPin(controls, pin)
    }

    fun requiresPlaylistPin(): Boolean {
        // Until the profile's controls load (they reset on every profile or account change),
        // ask for the PIN rather than let protected settings open unchecked.
        if (!_parentalControlsLoaded.value) return true
        val controls = _parentalControls.value ?: return false
        return parentalGate.requiresPinForSettings(controls) &&
            !parentalGate.isPinVerified(controls.profileId)
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                cloudAccountRetryCoordinator.retryCloudSync()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun updateAutoplayNextEpisode(enabled: Boolean) {
        updateSettings { it.copy(autoplayNextEpisode = enabled) }
    }

    fun updateContinueWatching(enabled: Boolean) {
        updateSettings { it.copy(continueWatchingEnabled = enabled) }
    }

    fun updateDefaultAudioLanguage(language: String) {
        updateSettings { it.copy(defaultAudioLanguage = language) }
    }

    fun updateSubtitlesEnabled(enabled: Boolean) {
        updateSettings { settings ->
            settings.copy(
                subtitlesEnabled = enabled,
                defaultSubtitleLanguage = if (enabled) {
                    settings.defaultSubtitleLanguage ?: settings.defaultAudioLanguage
                } else {
                    settings.defaultSubtitleLanguage
                },
            )
        }
    }

    fun updateDefaultSubtitleLanguage(language: String) {
        updateSettings { it.copy(defaultSubtitleLanguage = language) }
    }

    fun updateStreamingQuality(quality: String) {
        updateSettings { it.copy(streamingQuality = StreamingQualityOption.normalize(quality)) }
    }

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            val current = _userSettings.value ?: return@launch
            val updated = transform(current)
            _userSettings.value = updated
            runCatching {
                userSettingsRepository.updateSettings(updated)
            }.onFailure {
                _userSettings.value = current
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        if (_isSigningOut.value) return
        viewModelScope.launch {
            _isSigningOut.value = true
            try {
                sessionTeardown.signOut()
            } finally {
                _isSigningOut.value = false
            }
            onComplete()
        }
    }

    fun refreshCurrentSource() {
        CatalogRefreshSupport.runCatalogRefresh(
            scope = viewModelScope,
            getRefreshState = { _refreshState.value },
            setRefreshState = { refreshState -> _refreshState.value = refreshState },
            catalogRefreshController = catalogRefreshController,
            catalogSyncProgressTracker = catalogSyncProgressTracker,
            appSessionRepository = appSessionRepository,
        )
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
