package com.iptvcinema.tv.core.player

sealed interface PlayerCommand {
    data object PlayPause : PlayerCommand
    data object Play : PlayerCommand
    data object Pause : PlayerCommand
    data class SeekTo(val positionMs: Long) : PlayerCommand
    data class SeekRelative(val deltaMs: Long) : PlayerCommand
    data class SelectAudioTrack(val index: Int) : PlayerCommand
    data class SelectSubtitleTrack(val index: Int) : PlayerCommand
    data object DisableSubtitles : PlayerCommand
    data object Retry : PlayerCommand
    data object ChannelPrevious : PlayerCommand
    data object ChannelNext : PlayerCommand
}

enum class ChannelDirection {
    PREVIOUS,
    NEXT,
}
