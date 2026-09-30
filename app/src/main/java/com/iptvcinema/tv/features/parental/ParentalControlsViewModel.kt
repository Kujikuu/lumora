package com.iptvcinema.tv.features.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsDefaults
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.model.catalog.CatalogContentType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.parental.PinHasher
import com.iptvcinema.tv.core.util.AppStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ParentalUiState {
    data object Loading : ParentalUiState
    data class Ready(
        val profiles: List<UserProfile>,
        val controls: ParentalControls,
        val availableCategories: List<String>,
        /** The profile has a PIN that was not entered in this session: nothing can change yet. */
        val locked: Boolean = false,
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
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ParentalUiState>(ParentalUiState.Loading)
    val uiState: StateFlow<ParentalUiState> = _uiState.asStateFlow()

    private var selectedProfileId: String? = null
    private var pendingNewPin: String? = null
    private val saveMutex = Mutex()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ParentalUiState.Loading
            loadSafely()
        }
    }

    fun selectProfile(profileId: String) {
        selectedProfileId = profileId
        viewModelScope.launch { loadSafely() }
    }

    private suspend fun loadSafely() {
        try {
            loadReadyState()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _uiState.value = ParentalUiState.Error(appStrings.get(R.string.parental_error_load))
        }
    }

    /**
     * Applies a change at once and saves it. Saves run one at a time and each writes the latest
     * controls, so two quick toggles can never land out of order and undo each other.
     */
    fun updateControls(transform: (ParentalControls) -> ParentalControls) {
        val ready = _uiState.value as? ParentalUiState.Ready ?: return
        if (ready.locked || ParentalControlsDefaults.isFallback(ready.controls)) return
        val updated = transform(ready.controls)
        _uiState.value = ready.copy(controls = updated)
        viewModelScope.launch {
            saveMutex.withLock {
                val latest = (_uiState.value as? ParentalUiState.Ready)?.controls
                    ?.takeIf { it.profileId == updated.profileId }
                    ?: return@withLock
                try {
                    parentalControlsRepository.updateControls(latest)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _uiState.value = ParentalUiState.Error(appStrings.get(R.string.parental_error_save))
                }
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
                val check = parentalGate.checkPin(ready.controls, pin)
                val message = check.messageOrNull(appStrings)
                if (message == null) {
                    if (ready.locked) _uiState.value = ready.copy(locked = false)
                    onSuccess()
                } else {
                    onError(message)
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
                    onError(appStrings.get(R.string.pin_mismatch))
                    return
                }
                if (ready.locked) return
                val hash = pinHasher.hashPin(pin)
                updateControls { current -> current.copy(pinHash = hash) }
                parentalGate.markPinVerified(ready.controls.profileId)
                pendingNewPin = null
                onSuccess()
            }
        }
    }

    fun clearPin() {
        if ((_uiState.value as? ParentalUiState.Ready)?.locked != false) return
        updateControls { current -> current.copy(pinHash = null) }
        parentalGate.clearSession()
    }

    private suspend fun loadReadyState() {
        val profiles = profilesRepository.getProfiles()
        val profileId = selectedProfileId ?: profiles.firstOrNull()?.id
        if (profileId == null) {
            _uiState.value = ParentalUiState.Error(appStrings.get(R.string.parental_no_profiles))
            return
        }
        selectedProfileId = profileId
        val controls = parentalControlsRepository.ensureControls(profileId)
        if (ParentalControlsDefaults.isFallback(controls)) {
            // The real controls could not be read. Showing the PIN-less stand-in would unlock
            // the screen, and saving it would wipe the real PIN, so offer Retry instead.
            _uiState.value = ParentalUiState.Error(appStrings.get(R.string.parental_error_load))
            return
        }
        val liveCategories = catalogRepository.getCategoryNames(CatalogContentType.LIVE)
        val vodCategories = catalogRepository.getCategoryNames(CatalogContentType.VOD)
        val seriesCategories = catalogRepository.getCategoryNames(CatalogContentType.SERIES)
        val availableCategories = (liveCategories + vodCategories + seriesCategories).distinct().sorted()
        _uiState.value = ParentalUiState.Ready(
            profiles = profiles,
            controls = controls,
            availableCategories = availableCategories,
            locked = parentalGate.pinEnabled(controls) && !parentalGate.isPinVerified(profileId),
        )
    }
}
