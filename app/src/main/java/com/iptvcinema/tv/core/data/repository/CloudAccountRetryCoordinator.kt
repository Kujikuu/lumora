package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.data.repository.supabase.SupabaseFavoritesRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseUserSettingsRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseWatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class CloudAccountRetryCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudAccountStatus: CloudAccountStatus,
    private val appSessionRepository: AppSessionRepository,
    private val favoritesRepository: SupabaseFavoritesRepository,
    private val watchHistoryRepository: SupabaseWatchHistoryRepository,
    private val userSettingsRepository: SupabaseUserSettingsRepository,
    private val parentalControlsRepository: SupabaseParentalControlsRepository,
    private val playlistSourcesRepository: SupabasePlaylistSourcesRepository,
) {
    suspend fun retryCloudSync() {
        if (!authRepository.isConfigured() || !authRepository.hasActiveSession()) return

        val profileId = appSessionRepository.sessionState.first().currentProfileId
        var readSuccess = false

        runCatching { userSettingsRepository.refresh() }
            .onSuccess { readSuccess = true }

        if (profileId != null) {
            runCatching { favoritesRepository.refresh(profileId) }
                .onSuccess { readSuccess = true }

            runCatching { watchHistoryRepository.refresh(profileId) }
                .onSuccess { readSuccess = true }

            runCatching { parentalControlsRepository.getControls(profileId) }
                .onSuccess { readSuccess = true }
        }

        runCatching {
            val userId = appSessionRepository.sessionState.first().userId
            if (userId != null) {
                playlistSourcesRepository.getSourcesCached(userId)
            }
        }.onSuccess { readSuccess = true }

        if (readSuccess) {
            cloudAccountStatus.reportCloudReadSuccess()
            cloudAccountStatus.reportCloudWriteSuccess()
        }
    }
}
