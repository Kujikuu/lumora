package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.UserSettings

data class PlaybackPreferences(
    val defaultAudioLanguage: String,
    val defaultSubtitleLanguage: String?,
    val subtitlesEnabled: Boolean,
    val streamingQuality: String,
) {
    companion object {
        fun fromUserSettings(settings: UserSettings?): PlaybackPreferences = PlaybackPreferences(
            defaultAudioLanguage = settings?.defaultAudioLanguage?.ifBlank { "en" } ?: "en",
            defaultSubtitleLanguage = settings?.defaultSubtitleLanguage,
            subtitlesEnabled = settings?.subtitlesEnabled ?: false,
            streamingQuality = StreamingQualityOption.normalize(settings?.streamingQuality),
        )
    }
}

enum class StreamingQualityOption(val storageValue: String, val maxVideoHeight: Int?) {
    AUTO("AUTO", null),
    P1080("1080", 1080),
    P720("720", 720),
    P480("480", 480),
    ;

    companion object {
        fun normalize(raw: String?): String = fromStorage(raw).storageValue

        fun fromStorage(raw: String?): StreamingQualityOption = when (raw?.trim()?.uppercase()) {
            "1080", "1080P" -> P1080
            "720", "720P" -> P720
            "480", "480P" -> P480
            else -> AUTO
        }
    }
}

data class MediaTrackOption(
    val index: Int,
    val label: String,
    val language: String?,
)

object PlaybackPreferencesApplier {
    fun selectAudioTrackIndex(
        tracks: List<MediaTrackOption>,
        preferredLanguage: String,
    ): Int? {
        if (tracks.isEmpty()) return null
        val normalizedPreferred = preferredLanguage.trim().lowercase()
        if (normalizedPreferred.isNotBlank()) {
            tracks.firstOrNull { track ->
                track.language?.trim()?.lowercase() == normalizedPreferred
            }?.index?.let { return it }
            tracks.firstOrNull { track ->
                track.language?.trim()?.lowercase()?.startsWith(normalizedPreferred) == true
            }?.index?.let { return it }
        }
        return tracks.first().index
    }

    fun selectSubtitleTrackIndex(
        tracks: List<MediaTrackOption>,
        preferredLanguage: String?,
        subtitlesEnabled: Boolean,
    ): Int? {
        if (!subtitlesEnabled || tracks.isEmpty()) return null
        val normalizedPreferred = preferredLanguage?.trim()?.lowercase().orEmpty()
        if (normalizedPreferred.isNotBlank()) {
            tracks.firstOrNull { track ->
                track.language?.trim()?.lowercase() == normalizedPreferred
            }?.index?.let { return it }
            tracks.firstOrNull { track ->
                track.language?.trim()?.lowercase()?.startsWith(normalizedPreferred) == true
            }?.index?.let { return it }
        }
        return tracks.first().index
    }

    fun maxVideoHeight(quality: String): Int? =
        StreamingQualityOption.fromStorage(quality).maxVideoHeight
}
