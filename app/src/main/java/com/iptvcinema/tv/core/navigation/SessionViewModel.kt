package com.iptvcinema.tv.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.CloudAccountRetryCoordinator
import com.iptvcinema.tv.core.data.repository.CloudAccountStatus
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.tvhome.PendingDeepLinkStore
import com.iptvcinema.tv.core.tvhome.PlaybackDeepLink
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity-wide session state for the route guards and the shell.
 *
 * It is created during the very first composition, so the cloud-backed dependencies are
 * Lazy and fetched off the main thread: building them builds the Supabase client, which
 * would otherwise delay the first frame.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val cloudAccountStatus: Lazy<CloudAccountStatus>,
    private val cloudAccountRetryCoordinator: Lazy<CloudAccountRetryCoordinator>,
    private val authRepository: Lazy<AuthRepository>,
    private val profilesRepository: Lazy<ProfilesRepository>,
    private val pendingDeepLinkStore: PendingDeepLinkStore,
) : ViewModel() {
    val pendingDeepLink: StateFlow<PlaybackDeepLink?> = pendingDeepLinkStore.pending

    fun consumeDeepLink(link: PlaybackDeepLink) = pendingDeepLinkStore.consume(link)

    val isCloudDegraded: StateFlow<Boolean> = flow {
        emitAll(offMain(cloudAccountStatus).isDegraded)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isHydrated = MutableStateFlow(false)
    val isHydrated: StateFlow<Boolean> = _isHydrated.asStateFlow()

    // Declared before sessionState: its eager collector can emit synchronously during
    // construction and would otherwise write to these before they are initialized.
    private val _accountDisplayName = MutableStateFlow("")
    val accountDisplayName: StateFlow<String> = _accountDisplayName.asStateFlow()

    private val _activeProfileName = MutableStateFlow<String?>(null)
    val activeProfileName: StateFlow<String?> = _activeProfileName.asStateFlow()

    // Only the hydration flag is set here. Identity refresh does network work and runs in its
    // own collector, so it never delays session changes reaching the route guards.
    val sessionState: StateFlow<AppSessionState> = appSessionRepository.sessionState
        .onEach { _isHydrated.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSessionState(),
        )

    init {
        viewModelScope.launch {
            appSessionRepository.sessionState
                .distinctUntilChangedBy { it.userId to it.currentProfileId }
                .collectLatest { session -> refreshShellIdentity(session) }
        }
    }

    fun retryCloudSync() {
        viewModelScope.launch {
            offMain(cloudAccountRetryCoordinator).retryCloudSync()
        }
    }

    private suspend fun refreshShellIdentity(session: AppSessionState) {
        if (!session.isAuthenticated) {
            _accountDisplayName.value = ""
            _activeProfileName.value = null
            return
        }
        _accountDisplayName.value = offMain(authRepository).currentUserDisplayName().orEmpty()
        val profileId = session.currentProfileId
        _activeProfileName.value = if (profileId == null) {
            null
        } else {
            runCatching {
                offMain(profilesRepository).getProfiles().firstOrNull { it.id == profileId }?.name
            }.getOrNull()
        }
    }

    private suspend fun <T : Any> offMain(dependency: Lazy<T>): T =
        withContext(Dispatchers.Default) { dependency.get() }
}
