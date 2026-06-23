package com.iptvcinema.tv.core.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedCredentialsDto(
    val v: Int = 1,
    val type: String,
    val iv: String,
    val data: String,
)

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val type: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("parental_pin_enabled") val parentalPinEnabled: Boolean = false,
    @SerialName("max_rating") val maxRating: String? = null,
)

@Serializable
data class DeviceActivationSessionDto(
    val id: String,
    val code: String,
    @SerialName("qr_token") val qrToken: String,
    val status: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class PlaylistSourceDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val type: String,
    @SerialName("server_url") val serverUrl: String? = null,
    @SerialName("playlist_url") val playlistUrl: String? = null,
    @SerialName("epg_url") val epgUrl: String? = null,
    @SerialName("encrypted_credentials") val encryptedCredentials: EncryptedCredentialsDto? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val status: String = "ACTIVE",
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
)

@Serializable
data class FavoriteDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("content_id") val contentId: String,
    @SerialName("content_type") val contentType: String,
    val title: String,
    @SerialName("poster_url") val posterUrl: String? = null,
)

@Serializable
data class WatchHistoryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("content_id") val contentId: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("series_id") val seriesId: String? = null,
    val title: String,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("position_ms") val positionMs: Long = 0,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("last_watched_at") val lastWatchedAt: String,
)

@Serializable
data class UserSettingsDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("default_audio_language") val defaultAudioLanguage: String = "en",
    @SerialName("default_subtitle_language") val defaultSubtitleLanguage: String? = null,
    @SerialName("subtitles_enabled") val subtitlesEnabled: Boolean = false,
    @SerialName("autoplay_next_episode") val autoplayNextEpisode: Boolean = true,
    @SerialName("continue_watching_enabled") val continueWatchingEnabled: Boolean = true,
    @SerialName("skip_intro_enabled") val skipIntroEnabled: Boolean = false,
    @SerialName("streaming_quality") val streamingQuality: String = "AUTO",
    val theme: String = "DARK_CINEMA",
)

@Serializable
data class ParentalControlsDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("pin_hash") val pinHash: String? = null,
    @SerialName("hide_adult_categories") val hideAdultCategories: Boolean = true,
    @SerialName("lock_playlist_settings") val lockPlaylistSettings: Boolean = false,
    @SerialName("lock_live_categories") val lockLiveCategories: Boolean = false,
    @SerialName("max_rating") val maxRating: String? = null,
    @SerialName("blocked_categories") val blockedCategories: List<String> = emptyList(),
)

@Serializable
data class ActivationSessionInsertDto(
    val code: String,
    @SerialName("qr_token") val qrToken: String,
    val status: String,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class SessionExchangeRequest(
    val code: String,
)

@Serializable
data class SessionExchangeResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("user_id") val userId: String,
)
