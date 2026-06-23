package com.iptvcinema.tv.core.model

import java.time.Instant

enum class FavoriteContentType {
    CHANNEL,
    MOVIE,
    SERIES,
    EPISODE,
}

enum class WatchHistoryContentType {
    CHANNEL,
    MOVIE,
    EPISODE,
}

enum class ActivationSessionStatus {
    PENDING,
    APPROVED,
    EXPIRED,
}

data class DeviceActivationSession(
    val id: String,
    val code: String,
    val qrToken: String,
    val status: ActivationSessionStatus,
    val userId: String?,
    val deviceName: String?,
    val expiresAt: Instant,
)

data class PlaylistSourceRecord(
    val id: String,
    val userId: String,
    val name: String,
    val type: SourceType,
    val serverUrl: String?,
    val playlistUrl: String?,
    val epgUrl: String?,
    val isActive: Boolean,
    val status: SourceStatus,
    val lastSyncedAt: Instant?,
)

data class FavoriteItem(
    val id: String,
    val profileId: String,
    val sourceId: String?,
    val contentId: String,
    val contentType: FavoriteContentType,
    val title: String,
    val posterUrl: String?,
)

data class WatchHistoryItem(
    val id: String,
    val profileId: String,
    val sourceId: String?,
    val contentId: String,
    val contentType: WatchHistoryContentType,
    val seriesId: String? = null,
    val title: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long?,
    val lastWatchedAt: Instant,
)

data class UserSettings(
    val id: String,
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

data class ParentalControls(
    val id: String,
    val userId: String,
    val profileId: String,
    val pinHash: String? = null,
    val hideAdultCategories: Boolean,
    val lockPlaylistSettings: Boolean,
    val lockLiveCategories: Boolean,
    val maxRating: String?,
    val blockedCategories: List<String>,
) {
    val pinEnabled: Boolean get() = !pinHash.isNullOrBlank()
}

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val accountName: String,
)

data class M3uCredentials(
    val playlistUrl: String,
    val epgUrl: String?,
    val playlistName: String,
    val userAgent: String?,
    val referer: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
)
