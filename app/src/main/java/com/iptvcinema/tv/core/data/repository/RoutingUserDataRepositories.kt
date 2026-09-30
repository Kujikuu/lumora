package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.data.repository.local.LocalFavoritesRepository
import com.iptvcinema.tv.core.data.repository.local.LocalParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.local.LocalProfilesRepository
import com.iptvcinema.tv.core.data.repository.local.LocalUserSettingsRepository
import com.iptvcinema.tv.core.data.repository.local.LocalWatchHistoryRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseFavoritesRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseProfilesRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseUserSettingsRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseWatchHistoryRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Singleton
class RoutingProfilesRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val supabase: SupabaseProfilesRepository,
    private val local: LocalProfilesRepository,
) : ProfilesRepository {
    private suspend fun resolveBackend(): ProfilesRepository =
        if (authRepository.isConfigured() && authRepository.hasActiveSession()) supabase else local

    override suspend fun getProfiles(): List<UserProfile> = resolveBackend().getProfiles()

    override suspend fun ensureDefaultProfile(): UserProfile? = resolveBackend().ensureDefaultProfile()

    override suspend fun createProfile(name: String, type: String): UserProfile =
        resolveBackend().createProfile(name, type)

    override suspend fun updateProfile(profileId: String, name: String, type: String): UserProfile =
        resolveBackend().updateProfile(profileId, name, type)

    override suspend fun deleteProfile(profileId: String) = resolveBackend().deleteProfile(profileId)
}

@Singleton
class RoutingFavoritesRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudAccountStatus: CloudAccountStatus,
    private val supabase: SupabaseFavoritesRepository,
    private val local: LocalFavoritesRepository,
) : FavoritesRepository {
    private suspend fun resolveBackend(): FavoritesRepository =
        if (authRepository.isConfigured() && authRepository.hasActiveSession()) supabase else local

    override fun observeFavorites(profileId: String): Flow<List<FavoriteItem>> = flow {
        emitAll(resolveBackend().observeFavorites(profileId))
    }

    override suspend fun isFavorite(
        profileId: String,
        contentId: String,
        contentType: FavoriteContentType,
    ): Boolean {
        val backend = resolveBackend()
        return cloudAccountStatus.cloudRead({ false }) { backend.isFavorite(profileId, contentId, contentType) }
    }

    override suspend fun toggleFavorite(
        profileId: String,
        contentId: String,
        contentType: FavoriteContentType,
        title: String,
        posterUrl: String?,
        sourceId: String?,
        currentlyFavorite: Boolean?,
    ): Boolean {
        val backend = resolveBackend()
        // Rethrows: returning a guess would tell the caller the title was removed when the save
        // simply failed (offline), and the screen would show the wrong state.
        return cloudAccountStatus.cloudWrite {
            backend.toggleFavorite(profileId, contentId, contentType, title, posterUrl, sourceId, currentlyFavorite)
        }
    }

    override suspend fun removeFavorite(profileId: String, favorite: FavoriteItem) {
        val backend = resolveBackend()
        cloudAccountStatus.cloudWrite { backend.removeFavorite(profileId, favorite) }
    }
}

@Singleton
class RoutingWatchHistoryRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudAccountStatus: CloudAccountStatus,
    private val supabase: SupabaseWatchHistoryRepository,
    private val local: LocalWatchHistoryRepository,
) : WatchHistoryRepository {
    private suspend fun resolveBackend(): WatchHistoryRepository =
        if (authRepository.isConfigured() && authRepository.hasActiveSession()) supabase else local

    override fun observeHistory(profileId: String, limit: Int): Flow<List<WatchHistoryItem>> = flow {
        emitAll(resolveBackend().observeHistory(profileId, limit))
    }

    override fun observeContinueWatching(profileId: String, limit: Int): Flow<List<WatchHistoryItem>> = flow {
        emitAll(resolveBackend().observeContinueWatching(profileId, limit))
    }

    override suspend fun getProgress(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
    ): WatchHistoryItem? {
        val backend = resolveBackend()
        return cloudAccountStatus.cloudRead({ null }) { backend.getProgress(profileId, contentId, contentType) }
    }

    override suspend fun upsertProgress(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
        title: String,
        posterUrl: String?,
        positionMs: Long,
        durationMs: Long?,
        sourceId: String?,
        seriesId: String?,
    ) {
        val backend = resolveBackend()
        // Progress saves are best effort: a failed one is reported, not thrown.
        cloudAccountStatus.cloudWriteBestEffort {
            backend.upsertProgress(
                profileId,
                contentId,
                contentType,
                title,
                posterUrl,
                positionMs,
                durationMs,
                sourceId,
                seriesId,
            )
        }
    }

    override suspend fun getDistinctSeriesIds(profileId: String): List<String> {
        val backend = resolveBackend()
        return cloudAccountStatus.cloudRead({ emptyList() }) { backend.getDistinctSeriesIds(profileId) }
    }

    override suspend fun getEpisodeHistoryForSeries(
        profileId: String,
        sourceId: String,
        seriesId: String,
    ): List<WatchHistoryItem> {
        val backend = resolveBackend()
        return cloudAccountStatus.cloudRead({ emptyList() }) {
            backend.getEpisodeHistoryForSeries(profileId, sourceId, seriesId)
        }
    }

    override suspend fun remove(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
    ) {
        val backend = resolveBackend()
        cloudAccountStatus.cloudWriteBestEffort { backend.remove(profileId, contentId, contentType) }
    }

    override suspend fun removeSeries(profileId: String, seriesId: String) {
        val backend = resolveBackend()
        cloudAccountStatus.cloudWriteBestEffort { backend.removeSeries(profileId, seriesId) }
    }

    override fun invalidate() {
        supabase.invalidate()
        local.invalidate()
    }
}

@Singleton
class RoutingUserSettingsRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudAccountStatus: CloudAccountStatus,
    private val supabase: SupabaseUserSettingsRepository,
    private val local: LocalUserSettingsRepository,
) : UserSettingsRepository {
    private suspend fun resolveBackend(): UserSettingsRepository =
        if (authRepository.isConfigured() && authRepository.hasActiveSession()) supabase else local

    override fun observeSettings(): Flow<UserSettings?> = flow {
        emitAll(resolveBackend().observeSettings())
    }

    override suspend fun getSettings(): UserSettings? {
        val backend = resolveBackend()
        return cloudAccountStatus.cloudRead({ null }) { backend.getSettings() }
    }

    override suspend fun updateSettings(settings: UserSettings) {
        val backend = resolveBackend()
        cloudAccountStatus.cloudWriteBestEffort { backend.updateSettings(settings) }
    }
}

@Singleton
class RoutingParentalControlsRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudAccountStatus: CloudAccountStatus,
    private val supabase: SupabaseParentalControlsRepository,
    private val local: LocalParentalControlsRepository,
) : ParentalControlsRepository {
    private suspend fun resolveBackend(): ParentalControlsRepository =
        if (authRepository.isConfigured() && authRepository.hasActiveSession()) supabase else local

    override fun observeControls(profileId: String): Flow<ParentalControls?> = flow {
        emitAll(
            resolveBackend().observeControls(profileId).map { controls ->
                when {
                    controls != null -> {
                        cloudAccountStatus.reportCloudReadSuccess()
                        controls
                    }
                    authRepository.isConfigured() -> {
                        cloudAccountStatus.reportCloudReadFailure(
                            IllegalStateException("Parental controls unavailable for profile $profileId"),
                        )
                        ParentalControlsDefaults.restrictiveFallback(profileId)
                    }
                    else -> controls
                }
            },
        )
    }

    override suspend fun getControls(profileId: String): ParentalControls? {
        val backend = resolveBackend()
        return cloudAccountStatus.cloudRead({ ParentalControlsDefaults.restrictiveFallback(profileId) }) {
            backend.getControls(profileId)
        }
    }

    override suspend fun updateControls(controls: ParentalControls) {
        val backend = resolveBackend()
        // Rethrows so the screen can roll back and say the save failed; a PIN that only looked
        // saved would be gone on the next load.
        cloudAccountStatus.cloudWrite { backend.updateControls(controls) }
    }

    override suspend fun ensureControls(profileId: String): ParentalControls {
        val backend = resolveBackend()
        return try {
            cloudAccountStatus.cloudWrite { backend.ensureControls(profileId) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ParentalControlsDefaults.restrictiveFallback(profileId)
        }
    }
}

/** A cloud read: success and failure are reported; cancellation is neither and propagates. */
private suspend inline fun <T> CloudAccountStatus.cloudRead(fallback: (Exception) -> T, block: () -> T): T =
    try {
        block().also { reportCloudReadSuccess() }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        reportCloudReadFailure(error)
        fallback(error)
    }

/** A cloud write whose failure the caller must handle: it is reported, then rethrown. */
private suspend inline fun <T> CloudAccountStatus.cloudWrite(block: () -> T): T =
    try {
        block().also { reportCloudWriteSuccess() }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        reportCloudWriteFailure(error)
        throw error
    }

/** A cloud write nobody waits on (progress, settings): a failure is reported and dropped. */
private suspend inline fun CloudAccountStatus.cloudWriteBestEffort(block: () -> Unit) {
    try {
        block()
        reportCloudWriteSuccess()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        reportCloudWriteFailure(error)
    }
}
