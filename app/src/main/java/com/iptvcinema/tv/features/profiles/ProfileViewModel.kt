package com.iptvcinema.tv.features.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.parental.ParentalGate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProfilesUiState {
    data object Loading : ProfilesUiState
    data class Ready(val profiles: List<UserProfile>) : ProfilesUiState
    data object Error : ProfilesUiState
}

/** The create/edit dialog. [editingId] is null when creating a new profile. */
data class ProfileEditorState(
    val editingId: String? = null,
    val name: String = "",
    val type: ProfileType = ProfileType.FAMILY,
    val nameError: ProfileNameError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val canDelete: Boolean = false,
)

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

    private val _editor = MutableStateFlow<ProfileEditorState?>(null)
    val editor: StateFlow<ProfileEditorState?> = _editor.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ProfilesUiState.Loading
            _uiState.value = try {
                profilesRepository.ensureDefaultProfile()
                ProfilesUiState.Ready(profilesRepository.getProfiles())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                ProfilesUiState.Error
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

    fun canAddProfile(): Boolean = ProfileRules.canAddProfile(currentProfiles())

    fun startCreate() {
        _editor.value = ProfileEditorState()
    }

    fun startEdit(profile: UserProfile) {
        _editor.value = ProfileEditorState(
            editingId = profile.id,
            name = profile.name,
            type = profile.type,
            canDelete = ProfileRules.canDelete(
                existing = currentProfiles(),
                profileId = profile.id,
                activeProfileId = sessionState.value.currentProfileId,
            ),
        )
    }

    fun updateEditorName(name: String) {
        _editor.update { it?.copy(name = name.take(ProfileRules.MAX_NAME_LENGTH + 1), nameError = null) }
    }

    fun updateEditorType(type: ProfileType) {
        _editor.update { it?.copy(type = type) }
    }

    fun dismissEditor() {
        if (_editor.value?.isSaving == true) return
        _editor.value = null
    }

    fun saveEditor() {
        val state = _editor.value ?: return
        if (state.isSaving) return
        val nameError = ProfileRules.validateName(state.name, currentProfiles(), state.editingId)
        if (nameError != null) {
            _editor.value = state.copy(nameError = nameError)
            return
        }
        runEditorAction(state) {
            val name = state.name.trim()
            if (state.editingId == null) {
                profilesRepository.createProfile(name, state.type.name)
            } else {
                profilesRepository.updateProfile(state.editingId, name, state.type.name)
            }
        }
    }

    fun deleteEditingProfile() {
        val state = _editor.value ?: return
        val profileId = state.editingId ?: return
        if (!state.canDelete || state.isSaving) return
        runEditorAction(state) { profilesRepository.deleteProfile(profileId) }
    }

    private fun runEditorAction(state: ProfileEditorState, action: suspend () -> Unit) {
        viewModelScope.launch {
            _editor.value = state.copy(isSaving = true, saveFailed = false)
            try {
                action()
                _editor.value = null
                _uiState.value = ProfilesUiState.Ready(profilesRepository.getProfiles())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _editor.value = state.copy(isSaving = false, saveFailed = true)
            }
        }
    }

    private fun currentProfiles(): List<UserProfile> =
        (_uiState.value as? ProfilesUiState.Ready)?.profiles.orEmpty()
}
