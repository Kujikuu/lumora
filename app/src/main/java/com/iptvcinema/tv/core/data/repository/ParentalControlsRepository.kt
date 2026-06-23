package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.ParentalControls
import kotlinx.coroutines.flow.Flow

interface ParentalControlsRepository {
    fun observeControls(profileId: String): Flow<ParentalControls?>
    suspend fun getControls(profileId: String): ParentalControls?
    suspend fun updateControls(controls: ParentalControls)
    suspend fun ensureControls(profileId: String): ParentalControls
}
