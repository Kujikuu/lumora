package com.iptvcinema.tv.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_favorites",
    primaryKeys = ["id"],
    indices = [Index("profileId")],
)
data class CachedFavoriteEntity(
    val id: String,
    val profileId: String,
    val sourceId: String?,
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "cached_watch_history",
    primaryKeys = ["id"],
    indices = [Index("profileId"), Index("profileId", "lastWatchedAtEpochMs")],
)
data class CachedWatchHistoryEntity(
    val id: String,
    val profileId: String,
    val sourceId: String?,
    val contentId: String,
    val contentType: String,
    val seriesId: String?,
    val title: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long?,
    val lastWatchedAtEpochMs: Long,
)

@Entity(
    tableName = "cached_user_settings",
    primaryKeys = ["userId"],
)
data class CachedUserSettingsEntity(
    val userId: String,
    val defaultAudioLanguage: String,
    val defaultSubtitleLanguage: String?,
    val subtitlesEnabled: Boolean,
    val autoplayNextEpisode: Boolean,
    val continueWatchingEnabled: Boolean,
    val skipIntroEnabled: Boolean,
    val streamingQuality: String,
    val theme: String,
)

@Entity(
    tableName = "cached_parental_controls",
    primaryKeys = ["profileId"],
)
data class CachedParentalControlsEntity(
    val id: String,
    val userId: String,
    val profileId: String,
    val pinHash: String?,
    val hideAdultCategories: Boolean,
    val lockPlaylistSettings: Boolean,
    val lockLiveCategories: Boolean,
    val maxRating: String?,
    val blockedCategoriesJson: String,
)

@Entity(
    tableName = "cached_playlist_sources",
    primaryKeys = ["id"],
    indices = [Index("userId")],
)
data class CachedPlaylistSourceEntity(
    val id: String,
    val userId: String,
    val name: String,
    val type: String,
    val serverUrl: String?,
    val playlistUrl: String?,
    val epgUrl: String?,
    val isActive: Boolean,
    val status: String,
    val lastSyncedAtEpochMs: Long?,
    val cachedAtEpochMs: Long,
)
