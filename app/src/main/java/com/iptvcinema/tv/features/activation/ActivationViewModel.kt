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
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ActivationUiState {
    data object Loading : ActivationUiState
    data class Ready(
        val code: String,
        val qrUrl: String,
        val statusMessage: String,
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
    private var pollingJob: Job? = null

    init {
        startActivation()
    }

    fun startActivation() {
        pollingJob?.cancel()
        viewModelScope.launch {
            _uiState.value = ActivationUiState.Loading
            if (!authRepository.isConfigured()) {
                _uiState.value = ActivationUiState.Ready(
                    code = "DEV-MODE",
                    qrUrl = "${BuildConfig.ACTIVATION_LINK_BASE}?activation=DEV-MODE",
                    statusMessage = "Supabase not configured. Use Enter Account for local demo auth.",
                )
                return@launch
            }
            runCatching {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val session = deviceActivationRepository.createSession(deviceName)
                sessionId = session.id
                activationCode = session.code
                _uiState.value = ActivationUiState.Ready(
                    code = session.code,
                    qrUrl = deviceActivationRepository.buildActivationUrl(session.code),
                    statusMessage = "Waiting for approval…",
                )
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
                    _uiState.value = ActivationUiState.Ready(
                        code = code,
                        qrUrl = deviceActivationRepository.buildActivationUrl(code),
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

    private fun pollForApproval(sessionId: String, code: String) {
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                val session = deviceActivationRepository.getSession(sessionId) ?: continue
                when (session.status) {
                    ActivationSessionStatus.APPROVED -> {
                        _uiState.value = ActivationUiState.Ready(
                            code = code,
                            qrUrl = deviceActivationRepository.buildActivationUrl(code),
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
                                    _uiState.value = ActivationUiState.Ready(
                                        code = code,
                                        qrUrl = deviceActivationRepository.buildActivationUrl(code),
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
                    ActivationSessionStatus.PENDING -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 3_000L
    }
}
