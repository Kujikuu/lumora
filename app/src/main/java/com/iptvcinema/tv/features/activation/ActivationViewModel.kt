package com.iptvcinema.tv.features.activation

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap
import com.iptvcinema.tv.core.model.ActivationSessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ActivationUiState {
    data object Loading : ActivationUiState
    data class Ready(
        val code: String,
        val qrUrl: String,
        val statusMessage: String,
        val expiresAt: Instant?,
        val remainingSeconds: Long?,
    ) : ActivationUiState
    data object Succeeded : ActivationUiState
    data class Error(val message: String) : ActivationUiState
}

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceActivationRepository: DeviceActivationRepository,
    private val startupSessionBootstrap: StartupSessionBootstrap,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ActivationUiState>(ActivationUiState.Loading)
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null
    private var activationCode: String? = null
    private var sessionExpiresAt: Instant? = null
    private var pollingJob: Job? = null
    private var countdownJob: Job? = null

    init {
        startActivation()
    }

    fun startActivation() {
        pollingJob?.cancel()
        countdownJob?.cancel()
        viewModelScope.launch {
            _uiState.value = ActivationUiState.Loading
            if (!authRepository.isConfigured()) {
                _uiState.value = ActivationUiState.Ready(
                    code = "DEV-MODE",
                    qrUrl = "${BuildConfig.ACTIVATION_LINK_BASE}?activation=DEV-MODE",
                    statusMessage = "Supabase not configured. Use Enter Account for local demo auth.",
                    expiresAt = null,
                    remainingSeconds = null,
                )
                return@launch
            }
            runCatching {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val session = deviceActivationRepository.createSession(deviceName)
                sessionId = session.id
                activationCode = session.code
                sessionExpiresAt = session.expiresAt
                updateReadyState(
                    code = session.code,
                    statusMessage = waitingMessage(session.expiresAt),
                )
                startCountdown(session.expiresAt)
                pollForApproval(session.id, session.code)
            }.onFailure { error ->
                _uiState.value = ActivationUiState.Error(
                    message = error.message ?: "Unable to start activation",
                )
            }
        }
    }

    suspend fun resolvePostAuthDestination(): StartupDestination =
        startupSessionBootstrap.prepareSessionState().resolveStartupDestination()

    fun authenticate(onComplete: (StartupDestination) -> Unit) {
        viewModelScope.launch {
            if (!authRepository.isConfigured()) {
                val destination = startupSessionBootstrap.authenticateLocalDev().resolveStartupDestination()
                onComplete(destination)
                return@launch
            }
            if (authRepository.hasActiveSession()) {
                val destination = startupSessionBootstrap.prepareSessionState().resolveStartupDestination()
                onComplete(destination)
                return@launch
            }
            val code = activationCode ?: return@launch
            _uiState.value = (_uiState.value as? ActivationUiState.Ready)?.copy(
                statusMessage = "Signing in…",
            ) ?: _uiState.value
            val result = deviceActivationRepository.exchangeForAuthSession(code)
            result.fold(
                onSuccess = {
                    val destination = startupSessionBootstrap.prepareSessionState().resolveStartupDestination()
                    onComplete(destination)
                },
                onFailure = { error ->
                    updateReadyState(
                        code = code,
                        statusMessage = error.message ?: "Activation failed. Approve the code on your phone first.",
                    )
                },
            )
        }
    }

    fun completeIfApproved(onComplete: (StartupDestination) -> Unit) {
        viewModelScope.launch {
            val id = sessionId ?: return@launch
            val session = deviceActivationRepository.getSession(id) ?: return@launch
            if (session.status == ActivationSessionStatus.APPROVED) {
                authenticate(onComplete)
            }
        }
    }

    private fun startCountdown(expiresAt: Instant?) {
        countdownJob?.cancel()
        if (expiresAt == null) return
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val code = activationCode ?: return@launch
                val remaining = remainingSeconds(expiresAt)
                if (remaining <= 0L) {
                    updateReadyState(code = code, statusMessage = "Code expired. Generating a new code…")
                    break
                }
                updateReadyState(code = code, statusMessage = waitingMessage(expiresAt))
                delay(COUNTDOWN_TICK_MS)
            }
        }
    }

    private fun pollForApproval(sessionId: String, code: String) {
        pollingJob = viewModelScope.launch {
            var pollDelayMs = INITIAL_POLL_INTERVAL_MS
            while (isActive) {
                delay(pollDelayMs)
                pollDelayMs = (pollDelayMs + POLL_BACKOFF_STEP_MS).coerceAtMost(MAX_POLL_INTERVAL_MS)
                val session = deviceActivationRepository.getSession(sessionId) ?: continue
                sessionExpiresAt = session.expiresAt
                when (session.status) {
                    ActivationSessionStatus.APPROVED -> {
                        updateReadyState(
                            code = code,
                            statusMessage = "Approved. Signing in…",
                        )
                        val result = deviceActivationRepository.exchangeForAuthSession(code)
                        result.fold(
                            onSuccess = {
                                _uiState.value = ActivationUiState.Succeeded
                            },
                            onFailure = { error ->
                                if (authRepository.hasActiveSession()) {
                                    _uiState.value = ActivationUiState.Succeeded
                                } else {
                                    updateReadyState(
                                        code = code,
                                        statusMessage = error.message ?: "Sign-in failed. Press Enter Account to retry.",
                                    )
                                }
                            },
                        )
                        return@launch
                    }
                    ActivationSessionStatus.EXPIRED -> {
                        if (authRepository.hasActiveSession()) {
                            _uiState.value = ActivationUiState.Succeeded
                        } else {
                            startActivation()
                        }
                        return@launch
                    }
                    ActivationSessionStatus.PENDING -> {
                        updateReadyState(code = code, statusMessage = waitingMessage(session.expiresAt))
                    }
                }
            }
        }
    }

    private fun updateReadyState(code: String, statusMessage: String) {
        val expiresAt = sessionExpiresAt
        _uiState.value = ActivationUiState.Ready(
            code = code,
            qrUrl = deviceActivationRepository.buildActivationUrl(code),
            statusMessage = statusMessage,
            expiresAt = expiresAt,
            remainingSeconds = expiresAt?.let(::remainingSeconds),
        )
    }

    private fun waitingMessage(expiresAt: Instant?): String {
        val remaining = expiresAt?.let(::remainingSeconds)
        return if (remaining != null && remaining > 0L) {
            "Waiting for approval… Code expires in ${formatRemaining(remaining)}"
        } else {
            "Waiting for approval…"
        }
    }

    private fun remainingSeconds(expiresAt: Instant): Long =
        Duration.between(Instant.now(), expiresAt).seconds.coerceAtLeast(0L)

    private fun formatRemaining(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        countdownJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val INITIAL_POLL_INTERVAL_MS = 2_000L
        private const val POLL_BACKOFF_STEP_MS = 2_000L
        private const val MAX_POLL_INTERVAL_MS = 8_000L
        private const val COUNTDOWN_TICK_MS = 1_000L
    }
}
