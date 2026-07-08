package com.iptvcinema.tv.core.data.local

import com.iptvcinema.tv.core.database.dao.UserDataCacheDao
import com.iptvcinema.tv.core.database.entity.CachedFavoriteEntity
import com.iptvcinema.tv.core.database.entity.CachedParentalControlsEntity
import com.iptvcinema.tv.core.database.entity.CachedPlaylistSourceEntity
import com.iptvcinema.tv.core.database.entity.CachedUserSettingsEntity
import com.iptvcinema.tv.core.database.entity.CachedWatchHistoryEntity
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.model.WatchHistoryItem
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class CloudUserDataCache @Inject constructor(
    private val userDataCacheDao: UserDataCacheDao,
    private val json: Json,
) {
    suspend fun getFavorites(profileId: String): List<FavoriteItem>? =
        userDataCacheDao.getFavorites(profileId)
            .takeIf { it.isNotEmpty() }
            ?.map { it.toDomain() }

    suspend fun saveFavorites(profileId: String, favorites: List<FavoriteItem>) {
        userDataCacheDao.replaceFavorites(
            profileId = profileId,
            favorites = favorites.map { it.toEntity() },
        )
    }

    suspend fun getWatchHistory(profileId: String, limit: Int): List<WatchHistoryItem>? =
        userDataCacheDao.getWatchHistory(profileId, limit)
            .takeIf { it.isNotEmpty() }
            ?.map { it.toDomain() }

    suspend fun saveWatchHistory(profileId: String, items: List<WatchHistoryItem>) {
        userDataCacheDao.replaceWatchHistory(
            profileId = profileId,
            items = items.map { it.toEntity() },
        )
    }

    suspend fun getUserSettings(userId: String): UserSettings? =
        userDataCacheDao.getUserSettings(userId)?.toDomain()

    suspend fun saveUserSettings(settings: UserSettings) {
        userDataCacheDao.upsertUserSettings(settings.toEntity())
    }

    suspend fun getParentalControls(profileId: String): ParentalControls? =
        userDataCacheDao.getParentalControls(profileId)?.toDomain()

    suspend fun saveParentalControls(controls: ParentalControls) {
        userDataCacheDao.upsertParentalControls(controls.toEntity(json))
    }

    suspend fun getPlaylistSources(userId: String): List<PlaylistSourceRecord>? =
        userDataCacheDao.getPlaylistSources(userId)
            .takeIf { it.isNotEmpty() }
            ?.map { it.toDomain() }

    suspend fun savePlaylistSources(userId: String, sources: List<PlaylistSourceRecord>) {
        val now = System.currentTimeMillis()
        userDataCacheDao.replacePlaylistSources(
            userId = userId,
            sources = sources.map { it.toEntity(now) },
        )
    }

    private fun FavoriteItem.toEntity() = CachedFavoriteEntity(
        id = id,
        profileId = profileId,
        sourceId = sourceId,
        contentId = contentId,
        contentType = contentType.name,
        title = title,
        posterUrl = posterUrl,
        createdAtEpochMs = System.currentTimeMillis(),
    )

    private fun CachedFavoriteEntity.toDomain() = FavoriteItem(
        id = id,
        profileId = profileId,
        sourceId = sourceId,
        contentId = contentId,
        contentType = com.iptvcinema.tv.core.model.FavoriteContentType.valueOf(contentType),
        title = title,
        posterUrl = posterUrl,
    )

    private fun WatchHistoryItem.toEntity() = CachedWatchHistoryEntity(
        id = id,
        profileId = profileId,
        sourceId = sourceId,
        contentId = contentId,
        contentType = contentType.name,
        seriesId = seriesId,
        title = title,
        posterUrl = posterUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        lastWatchedAtEpochMs = lastWatchedAt.toEpochMilli(),
    )

    private fun CachedWatchHistoryEntity.toDomain() = WatchHistoryItem(
        id = id,
        profileId = profileId,
        sourceId = sourceId,
        contentId = contentId,
        contentType = com.iptvcinema.tv.core.model.WatchHistoryContentType.valueOf(contentType),
        seriesId = seriesId,
        title = title,
        posterUrl = posterUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        lastWatchedAt = Instant.ofEpochMilli(lastWatchedAtEpochMs),
    )

    private fun UserSettings.toEntity() = CachedUserSettingsEntity(
        userId = userId,
        defaultAudioLanguage = defaultAudioLanguage,
        defaultSubtitleLanguage = defaultSubtitleLanguage,
        subtitlesEnabled = subtitlesEnabled,
        autoplayNextEpisode = autoplayNextEpisode,
        continueWatchingEnabled = continueWatchingEnabled,
        skipIntroEnabled = skipIntroEnabled,
        streamingQuality = streamingQuality,
        theme = theme,
    )

    private fun CachedUserSettingsEntity.toDomain() = UserSettings(
        id = userId,
        userId = userId,
        defaultAudioLanguage = defaultAudioLanguage,
        defaultSubtitleLanguage = defaultSubtitleLanguage,
        subtitlesEnabled = subtitlesEnabled,
        autoplayNextEpisode = autoplayNextEpisode,
        continueWatchingEnabled = continueWatchingEnabled,
        skipIntroEnabled = skipIntroEnabled,
        streamingQuality = streamingQuality,
        theme = theme,
    )

    private fun ParentalControls.toEntity(json: Json) = CachedParentalControlsEntity(
        id = id,
        userId = userId,
        profileId = profileId,
        pinHash = pinHash,
        hideAdultCategories = hideAdultCategories,
        lockPlaylistSettings = lockPlaylistSettings,
        lockLiveCategories = lockLiveCategories,
        maxRating = maxRating,
        blockedCategoriesJson = json.encodeToString(blockedCategories),
    )

    private fun CachedParentalControlsEntity.toDomain() = ParentalControls(
        id = id,
        userId = userId,
        profileId = profileId,
        pinHash = pinHash,
        hideAdultCategories = hideAdultCategories,
        lockPlaylistSettings = lockPlaylistSettings,
        lockLiveCategories = lockLiveCategories,
        maxRating = maxRating,
        blockedCategories = runCatching {
            json.decodeFromString<List<String>>(blockedCategoriesJson)
        }.getOrDefault(emptyList()),
    )

    private fun PlaylistSourceRecord.toEntity(cachedAtEpochMs: Long) = CachedPlaylistSourceEntity(
        id = id,
        userId = userId,
        name = name,
        type = type.name,
        serverUrl = serverUrl,
        playlistUrl = playlistUrl,
        epgUrl = epgUrl,
        isActive = isActive,
        status = status.name,
        lastSyncedAtEpochMs = lastSyncedAt?.toEpochMilli(),
        cachedAtEpochMs = cachedAtEpochMs,
    )

    private fun CachedPlaylistSourceEntity.toDomain() = PlaylistSourceRecord(
        id = id,
        userId = userId,
        name = name,
        type = SourceType.valueOf(type),
        serverUrl = serverUrl,
        playlistUrl = playlistUrl,
        epgUrl = epgUrl,
        isActive = isActive,
        status = SourceStatus.valueOf(status),
        lastSyncedAt = lastSyncedAtEpochMs?.let(Instant::ofEpochMilli),
    )
}
