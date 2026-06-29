package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.WatchHistoryContentType

data class PlaybackHeaders(
    val userAgent: String? = null,
    val referer: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
)

data class PlaybackRequest(
    val contentId: String,
    val contentType: WatchHistoryContentType,
    val sourceId: String?,
    val title: String,
    val posterUrl: String?,
    val streamUrl: String,
    val durationMs: Long?,
    val isLive: Boolean,
    val headers: PlaybackHeaders = PlaybackHeaders(),
    val metadata: List<String> = emptyList(),
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

sealed class PlaybackResolveResult {
    data class Success(val request: PlaybackRequest) : PlaybackResolveResult()
    data class Error(val message: String, val errorCode: String? = null) : PlaybackResolveResult()
}

data class TrackOption(
    val index: Int,
    val label: String,
    val groupIndex: Int,
    val trackIndex: Int,
)

data class PlayerUiState(
    val title: String = "",
    val metadata: List<String> = emptyList(),
    val isLive: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val hasFirstFrame: Boolean = false,
    val isReconnecting: Boolean = false,
    val playbackEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val selectedAudioIndex: Int = 0,
    val selectedSubtitleIndex: Int = -1,
    val qualityLabel: String? = null,
)
