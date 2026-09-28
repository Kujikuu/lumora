package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.ParentalControls
import android.util.Log
import com.iptvcinema.tv.core.util.safeSummary
import java.time.Instant
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

    fun reportCloudReadFailure(error: Throwable? = null) {
        Log.w(TAG, "Cloud read failed: ${error.safeSummary()}")
        if (authRepository.isConfigured()) {
            _readDegraded.value = true
        }
    }

    fun reportCloudReadSuccess() {
        _readDegraded.value = false
    }

    fun reportCloudWriteFailure(error: Throwable? = null) {
        Log.w(TAG, "Cloud write failed: ${error.safeSummary()}")
        if (authRepository.isConfigured()) {
            _writeDegraded.value = true
        }
    }

    fun reportCloudWriteSuccess() {
        _writeDegraded.value = false
    }

    private val _lastSyncedAt = MutableStateFlow<Instant?>(null)
    val lastSyncedAt: StateFlow<Instant?> = _lastSyncedAt.asStateFlow()

    fun markSynced(at: Instant = Instant.now()) {
        _lastSyncedAt.value = at
    }

    fun reset() {
        _readDegraded.value = false
        _writeDegraded.value = false
        _lastSyncedAt.value = null
    }
}

private const val TAG = "CloudAccountStatus"


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
