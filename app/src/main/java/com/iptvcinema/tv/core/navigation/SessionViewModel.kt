package com.iptvcinema.tv.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.CloudAccountRetryCoordinator
import com.iptvcinema.tv.core.data.repository.CloudAccountStatus
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    appSessionRepository: AppSessionRepository,
    cloudAccountStatus: CloudAccountStatus,
    private val cloudAccountRetryCoordinator: CloudAccountRetryCoordinator,
    private val authRepository: AuthRepository,
    private val profilesRepository: ProfilesRepository,
) : ViewModel() {
    val isCloudDegraded: StateFlow<Boolean> = cloudAccountStatus.isDegraded
    private val _isHydrated = MutableStateFlow(false)
    val isHydrated: StateFlow<Boolean> = _isHydrated.asStateFlow()

    val sessionState: StateFlow<AppSessionState> = appSessionRepository.sessionState
        .onEach { session ->
            _isHydrated.value = true
            refreshShellIdentity(session)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSessionState(),
        )

    private val _accountDisplayName = MutableStateFlow("")
    val accountDisplayName: StateFlow<String> = _accountDisplayName.asStateFlow()

    private val _activeProfileName = MutableStateFlow<String?>(null)
    val activeProfileName: StateFlow<String?> = _activeProfileName.asStateFlow()

    init {
        viewModelScope.launch {
            refreshShellIdentity(sessionState.value)
        }
    }

    fun retryCloudSync() {
        viewModelScope.launch {
            cloudAccountRetryCoordinator.retryCloudSync()
        }
    }

    private suspend fun refreshShellIdentity(session: AppSessionState) {
        _accountDisplayName.value = authRepository.currentUserDisplayName().orEmpty()
        val profileId = session.currentProfileId
        _activeProfileName.value = if (profileId == null) {
            null
        } else {
            runCatching {
                profilesRepository.getProfiles().firstOrNull { it.id == profileId }?.name
            }.getOrNull()
        }
    }
}
