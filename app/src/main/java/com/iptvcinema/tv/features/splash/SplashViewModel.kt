package com.iptvcinema.tv.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val startupSessionBootstrap: StartupSessionBootstrap,
) : ViewModel() {
    private val _startupDestination = MutableStateFlow<StartupDestination?>(null)
    val startupDestination: StateFlow<StartupDestination?> = _startupDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val sessionStateDeferred = async { startupSessionBootstrap.prepareSessionState() }
            val minimumSplashDeferred = async { delay(SPLASH_MIN_DURATION_MS) }

            val sessionState = sessionStateDeferred.await()
            minimumSplashDeferred.await()
            _startupDestination.value = sessionState.resolveStartupDestination()
        }
    }

    companion object {
        private const val SPLASH_MIN_DURATION_MS = 2000L
    }
}
