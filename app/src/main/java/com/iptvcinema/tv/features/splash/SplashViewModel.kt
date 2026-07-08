package com.iptvcinema.tv.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
            val sessionState = startupSessionBootstrap.prepareSessionState()
            _startupDestination.value = sessionState.resolveStartupDestination()
        }
    }
}
