package com.iptvcinema.tv.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap
import com.iptvcinema.tv.core.datastore.canRouteFromLocalSession
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class SplashViewModel @Inject constructor(
    // Lazy: building it pulls in the Supabase client and encrypted storage, which must not
    // happen on the main thread while the splash is drawing.
    private val startupSessionBootstrap: Lazy<StartupSessionBootstrap>,
    private val appSessionRepository: AppSessionRepository,
) : ViewModel() {
    private val _startupDestination = MutableStateFlow<StartupDestination?>(null)
    val startupDestination: StateFlow<StartupDestination?> = _startupDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = appSessionRepository.sessionState.first()
            _startupDestination.value = if (canRouteFromLocalSession(saved)) {
                // A returning user opens straight from the saved session. Waiting for auth
                // and the sources list here cost seconds on a weak TV; the check now runs
                // in the background and the route guards react to whatever it changes.
                bootstrap().prepareInBackground()
                saved.resolveStartupDestination()
            } else {
                // On a slow or dead network, fall back to the locally saved session instead
                // of keeping the splash up forever.
                val sessionState = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                    bootstrap().prepareSessionState()
                } ?: appSessionRepository.sessionState.first()
                sessionState.resolveStartupDestination()
            }
        }
    }

    private suspend fun bootstrap(): StartupSessionBootstrap =
        withContext(Dispatchers.Default) { startupSessionBootstrap.get() }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 15_000L
    }
}
