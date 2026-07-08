package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.ParentalControls
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Singleton
class CloudAccountStatus @Inject constructor(
    private val authRepository: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _readDegraded = MutableStateFlow(false)
    private val _writeDegraded = MutableStateFlow(false)

    val isReadDegraded: StateFlow<Boolean> = _readDegraded.asStateFlow()
    val isWriteDegraded: StateFlow<Boolean> = _writeDegraded.asStateFlow()
    val isDegraded: StateFlow<Boolean> = combine(_readDegraded, _writeDegraded) { read, write ->
        read || write
    }.stateIn(scope, SharingStarted.Eagerly, false)

    fun reportCloudReadFailure() {
        if (authRepository.isConfigured()) {
            _readDegraded.value = true
        }
    }

    fun reportCloudReadSuccess() {
        _readDegraded.value = false
    }

    fun reportCloudWriteFailure() {
        if (authRepository.isConfigured()) {
            _writeDegraded.value = true
        }
    }

    fun reportCloudWriteSuccess() {
        _writeDegraded.value = false
    }
}

object ParentalControlsDefaults {
    fun restrictiveFallback(profileId: String): ParentalControls = ParentalControls(
        id = "fallback-parental-$profileId",
        userId = "",
        profileId = profileId,
        pinHash = null,
        hideAdultCategories = true,
        lockPlaylistSettings = false,
        lockLiveCategories = false,
        maxRating = "PG",
        blockedCategories = emptyList(),
    )
}
