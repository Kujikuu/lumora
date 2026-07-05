package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.ParentalControls
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CloudAccountStatus @Inject constructor(
    private val authRepository: AuthRepository,
) {
    private val _isDegraded = MutableStateFlow(false)
    val isDegraded: StateFlow<Boolean> = _isDegraded.asStateFlow()

    fun reportCloudReadFailure() {
        if (authRepository.isConfigured()) {
            _isDegraded.value = true
        }
    }

    fun reportCloudReadSuccess() {
        _isDegraded.value = false
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
