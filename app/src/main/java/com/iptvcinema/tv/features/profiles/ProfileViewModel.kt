package com.iptvcinema.tv.features.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.parental.ParentalGate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfilesUiState {
    data object Loading : ProfilesUiState
    data class Ready(val profiles: List<UserProfile>) : ProfilesUiState
    data class Error(val message: String) : ProfilesUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val profilesRepository: ProfilesRepository,
    private val parentalGate: ParentalGate,
) : ViewModel() {
    val sessionState: StateFlow<AppSessionState> = appSessionRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSessionState(),
        )

    private val _uiState = MutableStateFlow<ProfilesUiState>(ProfilesUiState.Loading)
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ProfilesUiState.Loading
            runCatching {
                profilesRepository.ensureDefaultProfile()
                val profiles = profilesRepository.getProfiles()
                _uiState.value = ProfilesUiState.Ready(profiles)
            }.onFailure { error ->
                _uiState.value = ProfilesUiState.Error(error.message ?: "Unable to load profiles")
            }
        }
    }

    fun selectProfile(profileId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            parentalGate.clearSession()
            appSessionRepository.selectProfile(profileId)
            appSessionRepository.sessionState.first { it.currentProfileId == profileId }
            onComplete()
        }
    }

    fun createProfile(name: String, type: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                profilesRepository.createProfile(name, type)
                loadProfiles()
                onComplete()
            }.onFailure { error ->
                _uiState.value = ProfilesUiState.Error(error.message ?: "Unable to create profile")
            }
        }
    }
}
