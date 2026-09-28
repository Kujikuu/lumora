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
    /**
     * Re-reads the account's cloud data. Returns true when every read succeeded.
     *
     * It only reads, so it clears the read warning but never the write warning: a failed
     * write is not replayed and must not be hidden by a later successful read.
     */
    suspend fun retryCloudSync(): Boolean {
        if (!authRepository.isConfigured() || !authRepository.hasActiveSession()) return true

        val session = appSessionRepository.sessionState.first()
        val profileId = session.currentProfileId
        val results = buildList {
            add(runCatching { userSettingsRepository.refresh() })
            if (profileId != null) {
                add(runCatching { favoritesRepository.refresh(profileId) })
                add(runCatching { watchHistoryRepository.refresh(profileId) })
                add(runCatching { parentalControlsRepository.getControls(profileId) })
            }
            session.userId?.let { userId ->
                add(runCatching { playlistSourcesRepository.getSourcesCached(userId) })
            }
        }

        val allSucceeded = results.all { it.isSuccess }
        if (allSucceeded) {
            cloudAccountStatus.reportCloudReadSuccess()
            cloudAccountStatus.markSynced()
        } else {
            cloudAccountStatus.reportCloudReadFailure(results.firstNotNullOfOrNull { it.exceptionOrNull() })
        }
        return allSucceeded
    }
}
