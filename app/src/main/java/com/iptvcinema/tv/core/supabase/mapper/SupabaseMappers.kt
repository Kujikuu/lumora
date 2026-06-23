package com.iptvcinema.tv.core.supabase.mapper

import com.iptvcinema.tv.core.model.ActivationSessionStatus
import com.iptvcinema.tv.core.model.DeviceActivationSession
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.supabase.dto.DeviceActivationSessionDto
import com.iptvcinema.tv.core.supabase.dto.FavoriteDto
import com.iptvcinema.tv.core.supabase.dto.ParentalControlsDto
import com.iptvcinema.tv.core.supabase.dto.PlaylistSourceDto
import com.iptvcinema.tv.core.supabase.dto.ProfileDto
import com.iptvcinema.tv.core.supabase.dto.UserSettingsDto
import com.iptvcinema.tv.core.supabase.dto.WatchHistoryDto

fun ProfileDto.toUserProfile(): UserProfile = UserProfile(
    id = id,
    name = name,
    type = runCatching { ProfileType.valueOf(type) }.getOrDefault(ProfileType.MAIN),
    avatarInitial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
)

fun DeviceActivationSessionDto.toDomain(): DeviceActivationSession = DeviceActivationSession(
    id = id,
    code = code,
    qrToken = qrToken,
    status = runCatching { ActivationSessionStatus.valueOf(status) }.getOrDefault(ActivationSessionStatus.PENDING),
    userId = userId,
    deviceName = deviceName,
    expiresAt = parseSupabaseInstant(expiresAt),
)

fun PlaylistSourceDto.toDomain(): PlaylistSourceRecord = PlaylistSourceRecord(
    id = id,
    userId = userId,
    name = name,
    type = runCatching { SourceType.valueOf(type) }.getOrDefault(SourceType.XTREAM_CODES),
    serverUrl = serverUrl,
    playlistUrl = playlistUrl,
    epgUrl = epgUrl,
    isActive = isActive,
    status = runCatching { SourceStatus.valueOf(status) }.getOrDefault(SourceStatus.ACTIVE),
    lastSyncedAt = lastSyncedAt?.let(::parseSupabaseInstant),
)

fun FavoriteDto.toDomain(): FavoriteItem = FavoriteItem(
    id = id,
    profileId = profileId,
    sourceId = sourceId,
    contentId = contentId,
    contentType = runCatching { FavoriteContentType.valueOf(contentType) }.getOrDefault(FavoriteContentType.MOVIE),
    title = title,
    posterUrl = posterUrl,
)

fun WatchHistoryDto.toDomain(): WatchHistoryItem = WatchHistoryItem(
    id = id,
    profileId = profileId,
    sourceId = sourceId,
    contentId = contentId,
    contentType = runCatching { WatchHistoryContentType.valueOf(contentType) }.getOrDefault(WatchHistoryContentType.MOVIE),
    seriesId = seriesId,
    title = title,
    posterUrl = posterUrl,
    positionMs = positionMs,
    durationMs = durationMs,
    lastWatchedAt = parseSupabaseInstant(lastWatchedAt),
)

fun UserSettingsDto.toDomain(): UserSettings = UserSettings(
    id = id,
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

fun ParentalControlsDto.toDomain(): ParentalControls = ParentalControls(
    id = id,
    userId = userId,
    profileId = profileId,
    pinHash = pinHash,
    hideAdultCategories = hideAdultCategories,
    lockPlaylistSettings = lockPlaylistSettings,
    lockLiveCategories = lockLiveCategories,
    maxRating = maxRating,
    blockedCategories = blockedCategories,
)

fun UserSettings.toDto(userId: String): UserSettingsDto = UserSettingsDto(
    id = id,
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

fun ParentalControls.toDto(): ParentalControlsDto = ParentalControlsDto(
    id = id,
    userId = userId,
    profileId = profileId,
    pinHash = pinHash,
    hideAdultCategories = hideAdultCategories,
    lockPlaylistSettings = lockPlaylistSettings,
    lockLiveCategories = lockLiveCategories,
    maxRating = maxRating,
    blockedCategories = blockedCategories,
)
