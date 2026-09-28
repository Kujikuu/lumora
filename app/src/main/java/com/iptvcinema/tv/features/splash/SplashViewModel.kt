package com.iptvcinema.tv.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val startupSessionBootstrap: StartupSessionBootstrap,
    private val appSessionRepository: AppSessionRepository,
) : ViewModel() {
    private val _startupDestination = MutableStateFlow<StartupDestination?>(null)
    val startupDestination: StateFlow<StartupDestination?> = _startupDestination.asStateFlow()

    init {
        viewModelScope.launch {
            // On a slow or dead network, fall back to the locally saved session instead of
            // keeping the splash up forever.
            val sessionState = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                startupSessionBootstrap.prepareSessionState()
            } ?: appSessionRepository.sessionState.first()
            _startupDestination.value = sessionState.resolveStartupDestination()
        }
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 15_000L
    }
}
