package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun observeSettings(): Flow<UserSettings?>
    suspend fun getSettings(): UserSettings?
    suspend fun updateSettings(settings: UserSettings)
}
