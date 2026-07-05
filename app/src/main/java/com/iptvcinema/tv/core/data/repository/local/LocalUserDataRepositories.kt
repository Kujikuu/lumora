package com.iptvcinema.tv.core.data.repository.local

import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.player.ContinueWatchingResolver
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LocalProfilesRepository @Inject constructor() : ProfilesRepository {
    private val mutex = Mutex()
    private val profiles = mutableListOf<UserProfile>()

    override suspend fun getProfiles(): List<UserProfile> = mutex.withLock {
        profiles.toList()
    }

    override suspend fun ensureDefaultProfile(): UserProfile? = mutex.withLock {
        if (profiles.isEmpty()) {
            profiles += defaultProfile()
        }
        profiles.firstOrNull()
    }

    override suspend fun createProfile(name: String, type: String): UserProfile = mutex.withLock {
        val profile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            type = ProfileType.valueOf(type),
            avatarInitial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        )
        profiles += profile
        profile
    }

    override suspend fun updateProfile(profileId: String, name: String): UserProfile = mutex.withLock {
        val index = profiles.indexOfFirst { it.id == profileId }
        require(index >= 0) { "Profile not found" }
        val updated = profiles[index].copy(
            name = name,
            avatarInitial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        )
        profiles[index] = updated
        updated
    }

    override suspend fun deleteProfile(profileId: String) {
        mutex.withLock {
            profiles.removeAll { it.id == profileId }
        }
    }

    private fun defaultProfile(): UserProfile = UserProfile(
        id = "local-main-profile",
        name = "Main",
        type = ProfileType.MAIN,
        avatarInitial = "M",
    )
}

@Singleton
class LocalFavoritesRepository @Inject constructor() : FavoritesRepository {
    private val mutex = Mutex()
    private val favoritesByProfile = mutableMapOf<String, MutableList<FavoriteItem>>()
    private val refreshTrigger = MutableStateFlow(0)

    override fun observeFavorites(profileId: String): Flow<List<FavoriteItem>> =
        refreshTrigger.mapLatest {
            mutex.withLock {
                favoritesByProfile[profileId]?.toList().orEmpty()
            }
        }

    override suspend fun isFavorite(
        profileId: String,
        contentId: String,
        contentType: FavoriteContentType,
    ): Boolean = mutex.withLock {
        favoritesByProfile[profileId].orEmpty().any {
            it.contentId == contentId && it.contentType == contentType
        }
    }

    override suspend fun toggleFavorite(
        profileId: String,
        contentId: String,
        contentType: FavoriteContentType,
        title: String,
        posterUrl: String?,
        sourceId: String?,
    ): Boolean = mutex.withLock {
        val favorites = favoritesByProfile.getOrPut(profileId) { mutableListOf() }
        val existingIndex = favorites.indexOfFirst {
            it.contentId == contentId && it.contentType == contentType
        }
        if (existingIndex >= 0) {
            favorites.removeAt(existingIndex)
            refreshTrigger.value += 1
            return@withLock false
        }
        favorites += FavoriteItem(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            sourceId = sourceId,
            contentId = contentId,
            contentType = contentType,
            title = title,
            posterUrl = posterUrl,
        )
        refreshTrigger.value += 1
        true
    }

    override suspend fun removeFavorite(profileId: String, favorite: FavoriteItem) = mutex.withLock {
        val favorites = favoritesByProfile[profileId] ?: return@withLock
        val removed = favorites.removeAll {
            it.contentId == favorite.contentId && it.contentType == favorite.contentType
        }
        if (removed) {
            refreshTrigger.value += 1
        }
    }
}

@Singleton
class LocalWatchHistoryRepository @Inject constructor(
    private val continueWatchingResolver: ContinueWatchingResolver,
) : WatchHistoryRepository {
    private val mutex = Mutex()
    private val historyByProfile = mutableMapOf<String, MutableList<WatchHistoryItem>>()
    private val refreshTrigger = MutableStateFlow(0)

    override fun observeHistory(profileId: String, limit: Int): Flow<List<WatchHistoryItem>> =
        refreshTrigger.mapLatest {
            mutex.withLock {
                historyByProfile[profileId].orEmpty()
                    .sortedByDescending { it.lastWatchedAt }
                    .take(limit)
            }
        }

    override fun observeContinueWatching(profileId: String, limit: Int): Flow<List<WatchHistoryItem>> =
        refreshTrigger.mapLatest {
            mutex.withLock {
                val items = historyByProfile[profileId].orEmpty()
                continueWatchingResolver.resolve(
                    history = items,
                    sourceId = items.firstOrNull()?.sourceId,
                    limit = limit,
                )
            }
        }

    override suspend fun getProgress(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
    ): WatchHistoryItem? = mutex.withLock {
        historyByProfile[profileId].orEmpty().firstOrNull {
            it.contentId == contentId && it.contentType == contentType
        }
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
        mutex.withLock {
            val history = historyByProfile.getOrPut(profileId) { mutableListOf() }
            val existingIndex = history.indexOfFirst {
                it.contentId == contentId && it.contentType == contentType
            }
            val item = WatchHistoryItem(
                id = history.getOrNull(existingIndex)?.id ?: UUID.randomUUID().toString(),
                profileId = profileId,
                sourceId = sourceId,
                contentId = contentId,
                contentType = contentType,
                seriesId = seriesId,
                title = title,
                posterUrl = posterUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                lastWatchedAt = Instant.now(),
            )
            if (existingIndex >= 0) {
                history[existingIndex] = item
            } else {
                history += item
            }
        }
        refreshTrigger.value += 1
    }

    override suspend fun getDistinctSeriesIds(profileId: String): List<String> = mutex.withLock {
        historyByProfile[profileId].orEmpty()
            .mapNotNull { it.seriesId }
            .distinct()
    }

    override suspend fun remove(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
    ) {
        mutex.withLock {
            historyByProfile[profileId]?.removeAll {
                it.contentId == contentId && it.contentType == contentType
            }
        }
        refreshTrigger.value += 1
    }

    override fun invalidate() {
        refreshTrigger.value += 1
    }
}

@Singleton
class LocalUserSettingsRepository @Inject constructor() : UserSettingsRepository {
    private val settingsFlow = MutableStateFlow<UserSettings?>(defaultSettings())

    override fun observeSettings(): Flow<UserSettings?> = settingsFlow

    override suspend fun getSettings(): UserSettings? = settingsFlow.value

    override suspend fun updateSettings(settings: UserSettings) {
        settingsFlow.value = settings
    }

    companion object {
        private fun defaultSettings(): UserSettings = UserSettings(
            id = "local-settings",
            userId = "local-dev-user",
            defaultAudioLanguage = "en",
            defaultSubtitleLanguage = null,
            subtitlesEnabled = false,
            autoplayNextEpisode = true,
            continueWatchingEnabled = true,
            skipIntroEnabled = false,
            streamingQuality = "auto",
            theme = "dark",
        )
    }
}

@Singleton
class LocalParentalControlsRepository @Inject constructor() : ParentalControlsRepository {
    private val mutex = Mutex()
    private val controlsByProfile = mutableMapOf<String, ParentalControls>()
    private val refreshTrigger = MutableStateFlow(0)

    override fun observeControls(profileId: String): Flow<ParentalControls?> =
        refreshTrigger.mapLatest {
            mutex.withLock { controlsByProfile[profileId] ?: defaultControls(profileId) }
        }

    override suspend fun getControls(profileId: String): ParentalControls? = mutex.withLock {
        controlsByProfile[profileId] ?: defaultControls(profileId)
    }

    override suspend fun updateControls(controls: ParentalControls) {
        mutex.withLock {
            controlsByProfile[controls.profileId] = controls
        }
        refreshTrigger.value += 1
    }

    override suspend fun ensureControls(profileId: String): ParentalControls = mutex.withLock {
        controlsByProfile.getOrPut(profileId) { defaultControls(profileId) }
    }

    private fun defaultControls(profileId: String): ParentalControls = ParentalControls(
        id = "local-parental-$profileId",
        userId = "local-dev-user",
        profileId = profileId,
        pinHash = null,
        hideAdultCategories = false,
        lockPlaylistSettings = false,
        lockLiveCategories = false,
        maxRating = null,
        blockedCategories = emptyList(),
    )
}
