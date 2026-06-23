package com.iptvcinema.tv.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class SessionViewModel @Inject constructor(
    appSessionRepository: AppSessionRepository,
) : ViewModel() {
    private val _isHydrated = MutableStateFlow(false)
    val isHydrated: StateFlow<Boolean> = _isHydrated.asStateFlow()

    val sessionState: StateFlow<AppSessionState> = appSessionRepository.sessionState
        .onEach { _isHydrated.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSessionState(),
        )
}
