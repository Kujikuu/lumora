package com.iptvcinema.tv.features.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository
import com.iptvcinema.tv.core.datastore.SessionPreparer
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.device.DeviceIdentity
import com.iptvcinema.tv.core.model.ActivationSessionStatus
import com.iptvcinema.tv.core.model.DeviceActivationSession
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the TV is doing with the current code. The screen maps each value to a string resource. */
enum class ActivationStatus {
    Waiting,
    NotApprovedYet,
    SigningIn,
    SignInFailed,
    ConnectionProblem,
    DevMode,
}

enum class ActivationError {
    Network,
    Server,
}

sealed interface ActivationUiState {
    data object Loading : ActivationUiState

    data class Ready(
        val code: String,
        val qrUrl: String,
        val deviceName: String,
        val status: ActivationStatus,
        val remainingSeconds: Long?,
        val totalSeconds: Long?,
    ) : ActivationUiState

    data class Succeeded(val destination: StartupDestination) : ActivationUiState

    data class Error(val kind: ActivationError) : ActivationUiState
}

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceActivationRepository: DeviceActivationRepository,
    private val sessionPreparer: SessionPreparer,
    private val deviceIdentity: DeviceIdentity,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ActivationUiState>(ActivationUiState.Loading)
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private val signInMutex = Mutex()
    private var session: DeviceActivationSession? = null
    private var sessionJob: Job? = null
    private var signingIn = false

    init {
        startActivation()
    }

    /** Creates a fresh code. Also used by the "Get new code" and "Try again" buttons. */
    fun startActivation() {
        if (_uiState.value is ActivationUiState.Succeeded) return
        sessionJob?.cancel()
        session = null
        sessionJob = viewModelScope.launch {
            _uiState.value = ActivationUiState.Loading
            val deviceName = deviceIdentity.deviceName()
            if (!authRepository.isConfigured()) {
                _uiState.value = readyState(DEV_MODE_CODE, deviceName, ActivationStatus.DevMode, expiresAt = null)
                return@launch
            }
            val created = try {
                deviceActivationRepository.createSession(deviceName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.value = ActivationUiState.Error(error.toActivationError())
                return@launch
            }
            session = created
            _uiState.value = readyState(created.code, deviceName, ActivationStatus.Waiting, created.expiresAt)
            launch { runCountdown(created.expiresAt) }
            launch { pollForApproval(created) }
        }
    }

    // Called from inside the session job, which startActivation() cancels; hop out of it
    // first so the restart never runs under a cancelled parent.
    private fun restartActivation() {
        viewModelScope.launch { startActivation() }
    }

    /** "I've approved it" button: checks right away instead of waiting for the next poll. */
    fun checkApprovalNow() {
        viewModelScope.launch {
            if (!authRepository.isConfigured()) {
                finish(sessionPreparer.authenticateLocalDev().resolveStartupDestination())
                return@launch
            }
            if (authRepository.hasActiveSession()) {
                finish(sessionPreparer.prepareSessionState().resolveStartupDestination())
                return@launch
            }
            val current = session ?: return@launch
            val snapshot = try {
                deviceActivationRepository.checkStatus(current)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setStatus(ActivationStatus.ConnectionProblem)
                return@launch
            }
            when (snapshot.status) {
                ActivationSessionStatus.APPROVED -> completeSignIn(current)
                ActivationSessionStatus.PENDING -> setStatus(ActivationStatus.NotApprovedYet)
                ActivationSessionStatus.EXPIRED -> restartActivation()
            }
        }
    }

    private suspend fun runCountdown(expiresAt: Instant) {
        while (true) {
            val remaining = remainingSeconds(expiresAt)
            if (remaining <= 0L) {
                if (!signingIn) restartActivation()
                return
            }
            _uiState.update { state ->
                if (state is ActivationUiState.Ready) state.copy(remainingSeconds = remaining) else state
            }
            delay(COUNTDOWN_TICK_MS)
        }
    }

    private suspend fun pollForApproval(current: DeviceActivationSession) {
        var pollDelayMs = INITIAL_POLL_INTERVAL_MS
        var consecutiveFailures = 0
        while (true) {
            delay(pollDelayMs)
            pollDelayMs = (pollDelayMs + POLL_BACKOFF_STEP_MS).coerceAtMost(MAX_POLL_INTERVAL_MS)
            val snapshot = try {
                deviceActivationRepository.checkStatus(current)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                consecutiveFailures++
                if (consecutiveFailures >= FAILURES_BEFORE_WARNING) setStatus(ActivationStatus.ConnectionProblem)
                continue
            }
            if (consecutiveFailures > 0) {
                consecutiveFailures = 0
                setStatus(ActivationStatus.Waiting, onlyIf = ActivationStatus.ConnectionProblem)
            }
            when (snapshot.status) {
                ActivationSessionStatus.APPROVED -> {
                    completeSignIn(current)
                    return
                }
                ActivationSessionStatus.EXPIRED -> {
                    restartActivation()
                    return
                }
                ActivationSessionStatus.PENDING -> Unit
            }
        }
    }

    // The poll and the button can both see APPROVED; the lock makes sure the code is only
    // exchanged once, because the server burns it on the first exchange.
    private suspend fun completeSignIn(current: DeviceActivationSession) {
        signInMutex.withLock {
            if (_uiState.value is ActivationUiState.Succeeded) return
            signingIn = true
            setStatus(ActivationStatus.SigningIn)
            val result = deviceActivationRepository.exchangeForAuthSession(current)
            if (result.isSuccess || authRepository.hasActiveSession()) {
                finish(sessionPreparer.prepareSessionState().resolveStartupDestination())
            } else {
                signingIn = false
                setStatus(ActivationStatus.SignInFailed)
            }
        }
    }

    private fun finish(destination: StartupDestination) {
        _uiState.value = ActivationUiState.Succeeded(destination)
        sessionJob?.cancel()
    }

    private fun setStatus(status: ActivationStatus, onlyIf: ActivationStatus? = null) {
        _uiState.update { state ->
            if (state is ActivationUiState.Ready && (onlyIf == null || state.status == onlyIf)) {
                state.copy(status = status)
            } else {
                state
            }
        }
    }

    private fun readyState(
        code: String,
        deviceName: String,
        status: ActivationStatus,
        expiresAt: Instant?,
    ): ActivationUiState.Ready {
        val remaining = expiresAt?.let(::remainingSeconds)
        return ActivationUiState.Ready(
            code = code,
            qrUrl = if (status == ActivationStatus.DevMode) {
                "${BuildConfig.ACTIVATION_LINK_BASE}?activation=$code"
            } else {
                deviceActivationRepository.buildActivationUrl(code)
            },
            deviceName = deviceName,
            status = status,
            remainingSeconds = remaining,
            totalSeconds = remaining,
        )
    }

    private fun remainingSeconds(expiresAt: Instant): Long =
        Duration.between(Instant.now(), expiresAt).seconds.coerceAtLeast(0L)

    override fun onCleared() {
        sessionJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val DEV_MODE_CODE = "DEV-MODE"
        const val INITIAL_POLL_INTERVAL_MS = 2_000L
        const val POLL_BACKOFF_STEP_MS = 2_000L
        const val MAX_POLL_INTERVAL_MS = 8_000L
        const val COUNTDOWN_TICK_MS = 1_000L
        const val FAILURES_BEFORE_WARNING = 3
    }
}
