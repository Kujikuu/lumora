package com.iptvcinema.tv.features.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.model.catalog.CatalogContentType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.parental.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ParentalUiState {
    data object Loading : ParentalUiState
    data class Ready(
        val profiles: List<UserProfile>,
        val controls: ParentalControls,
        val availableCategories: List<String>,
    ) : ParentalUiState
    data class Error(val message: String) : ParentalUiState
}

@HiltViewModel
class ParentalControlsViewModel @Inject constructor(
    private val profilesRepository: ProfilesRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val catalogRepository: CatalogRepository,
    private val pinHasher: PinHasher,
    private val parentalGate: ParentalGate,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ParentalUiState>(ParentalUiState.Loading)
    val uiState: StateFlow<ParentalUiState> = _uiState.asStateFlow()

    private var selectedProfileId: String? = null
    private var pendingNewPin: String? = null

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ParentalUiState.Loading
            runCatching {
                loadReadyState()
            }.onFailure { error ->
                _uiState.value = ParentalUiState.Error(error.message ?: "Unable to load parental controls")
            }
        }
    }

    fun selectProfile(profileId: String) {
        selectedProfileId = profileId
        viewModelScope.launch {
            runCatching {
                loadReadyState()
            }.onFailure { error ->
                _uiState.value = ParentalUiState.Error(error.message ?: "Unable to load profile controls")
            }
        }
    }

    fun updateControls(transform: (ParentalControls) -> ParentalControls) {
        val current = (_uiState.value as? ParentalUiState.Ready)?.controls ?: return
        val updated = transform(current)
        val ready = _uiState.value as ParentalUiState.Ready
        _uiState.value = ready.copy(controls = updated)
        viewModelScope.launch {
            runCatching {
                parentalControlsRepository.updateControls(updated)
            }.onFailure { error ->
                _uiState.value = ready.copy(controls = current)
                _uiState.value = ParentalUiState.Error(error.message ?: "Unable to save parental controls")
            }
        }
    }

    fun beginSetPin() {
        pendingNewPin = null
    }

    fun onPinEntered(mode: PinEntryMode, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val ready = _uiState.value as? ParentalUiState.Ready ?: return
        when (mode) {
            PinEntryMode.Verify -> {
                if (parentalGate.verifyPin(ready.controls, pin)) {
                    onSuccess()
                } else {
                    onError("Incorrect PIN")
                }
            }
            PinEntryMode.SetNew -> {
                pendingNewPin = pin
                onSuccess()
            }
            PinEntryMode.ConfirmNew -> {
                val pending = pendingNewPin
                if (pending == null || pending != pin) {
                    pendingNewPin = null
                    onError("PINs do not match")
                    return
                }
                val hash = pinHasher.hashPin(pin)
                updateControls { current -> current.copy(pinHash = hash) }
                pendingNewPin = null
                onSuccess()
            }
        }
    }

    fun clearPin() {
        updateControls { current -> current.copy(pinHash = null) }
        parentalGate.clearSession()
    }

    private suspend fun loadReadyState() {
        val profiles = profilesRepository.getProfiles()
        val profileId = selectedProfileId ?: profiles.firstOrNull()?.id
        if (profileId == null) {
            _uiState.value = ParentalUiState.Error("No profiles available")
            return
        }
        selectedProfileId = profileId
        val controls = parentalControlsRepository.ensureControls(profileId)
        val liveCategories = catalogRepository.getCategoryNames(CatalogContentType.LIVE)
        val vodCategories = catalogRepository.getCategoryNames(CatalogContentType.VOD)
        val seriesCategories = catalogRepository.getCategoryNames(CatalogContentType.SERIES)
        val availableCategories = (liveCategories + vodCategories + seriesCategories).distinct().sorted()
        _uiState.value = ParentalUiState.Ready(
            profiles = profiles,
            controls = controls,
            availableCategories = availableCategories,
        )
    }
}
