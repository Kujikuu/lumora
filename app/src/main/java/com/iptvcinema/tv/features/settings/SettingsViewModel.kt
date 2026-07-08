package com.iptvcinema.tv.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.model.AccountSummary
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.player.StreamingQualityOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val localCredentialsStore: LocalCredentialsStore,
    private val catalogRefreshController: CatalogRefreshController,
    private val catalogSyncProgressTracker: CatalogSyncProgressTracker,
) : ViewModel() {
    val sessionState: StateFlow<AppSessionState> = appSessionRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSessionState(),
        )

    private val _accountSummary = MutableStateFlow<AccountSummary?>(null)
    val accountSummary: StateFlow<AccountSummary?> = _accountSummary.asStateFlow()

    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()

    private val _parentalControls = MutableStateFlow<ParentalControls?>(null)
    val parentalControls: StateFlow<ParentalControls?> = _parentalControls.asStateFlow()

    private val _refreshState = MutableStateFlow<CatalogRefreshState>(CatalogRefreshState.Idle)
    val refreshState: StateFlow<CatalogRefreshState> = _refreshState.asStateFlow()

    val parentalGateInstance: ParentalGate get() = parentalGate

    init {
        viewModelScope.launch {
            userSettingsRepository.observeSettings().collect { settings ->
                _userSettings.value = settings
            }
        }
        loadAccountAndParentalControls()
    }

    fun loadAccountAndParentalControls() {
        viewModelScope.launch {
            val email = authRepository.currentUserEmail()
            val displayName = authRepository.currentUserDisplayName()
                ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "Guest"
            _accountSummary.value = AccountSummary(
                name = displayName,
                email = email ?: "—",
                plan = "Lumora Play",
                renewalDate = "—",
            )
            val profileId = appSessionRepository.sessionState.first().currentProfileId
            if (profileId != null) {
                runCatching {
                    _parentalControls.value = parentalControlsRepository.getControls(profileId)
                }
            }
        }
    }

    fun verifyParentalPin(pin: String): Boolean {
        val controls = _parentalControls.value ?: return true
        return parentalGate.verifyPin(controls, pin)
    }

    fun requiresPlaylistPin(): Boolean {
        val controls = _parentalControls.value ?: return false
        return parentalGate.requiresPinForSettings(controls) &&
            !parentalGate.isPinVerified(controls.profileId)
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
        viewModelScope.launch {
            localCredentialsStore.clearAll()
            if (authRepository.isConfigured()) {
                authRepository.signOut()
            } else {
                appSessionRepository.clearSession()
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
}
