package com.iptvcinema.tv.features.player

import androidx.compose.ui.input.key.Key
import com.iptvcinema.tv.core.player.PlayerCommand

sealed interface PlayerKeyAction {
    data object PlayPause : PlayerKeyAction
    data object Play : PlayerKeyAction
    data object Pause : PlayerKeyAction
    data class SeekRelative(val deltaMs: Long) : PlayerKeyAction
    data object EpisodePrevious : PlayerKeyAction
    data object EpisodeNext : PlayerKeyAction
    data object ChannelPrevious : PlayerKeyAction
    data object ChannelNext : PlayerKeyAction
    data object RevealOverlay : PlayerKeyAction
}

object PlayerKeyHandler {
    private const val SEEK_STEP_MS = 10_000L

    fun resolve(
        key: Key,
        isLive: Boolean,
        isEpisode: Boolean,
        controlsVisible: Boolean,
        pickerOpen: Boolean,
    ): PlayerKeyAction? {
        if (pickerOpen) return null

        when (key) {
            Key.MediaPlayPause, Key.Spacebar -> return PlayerKeyAction.PlayPause
            Key.MediaPlay -> return PlayerKeyAction.Play
            Key.MediaPause -> return PlayerKeyAction.Pause
            Key.MediaRewind -> {
                if (!isLive) return PlayerKeyAction.SeekRelative(-SEEK_STEP_MS)
                return null
            }
            Key.MediaFastForward -> {
                if (!isLive) return PlayerKeyAction.SeekRelative(SEEK_STEP_MS)
                return null
            }
            Key.DirectionLeft -> {
                if (!controlsVisible && !isLive) return PlayerKeyAction.SeekRelative(-SEEK_STEP_MS)
                return null
            }
            Key.DirectionRight -> {
                if (!controlsVisible && !isLive) return PlayerKeyAction.SeekRelative(SEEK_STEP_MS)
                return null
            }
            Key.MediaPrevious -> {
                return when {
                    isLive -> PlayerKeyAction.ChannelPrevious
                    isEpisode -> PlayerKeyAction.EpisodePrevious
                    else -> null
                }
            }
            Key.MediaNext -> {
                return when {
                    isLive -> PlayerKeyAction.ChannelNext
                    isEpisode -> PlayerKeyAction.EpisodeNext
                    else -> null
                }
            }
            Key.ChannelUp, Key.PageUp -> {
                if (isLive) return PlayerKeyAction.ChannelPrevious
                return null
            }
            Key.ChannelDown, Key.PageDown -> {
                if (isLive) return PlayerKeyAction.ChannelNext
                return null
            }
            else -> Unit
        }

        if (!controlsVisible) {
            return PlayerKeyAction.RevealOverlay
        }
        return null
    }

    fun toCommand(action: PlayerKeyAction): PlayerCommand? = when (action) {
        PlayerKeyAction.PlayPause -> PlayerCommand.PlayPause
        PlayerKeyAction.Play -> PlayerCommand.Play
        PlayerKeyAction.Pause -> PlayerCommand.Pause
        is PlayerKeyAction.SeekRelative -> PlayerCommand.SeekRelative(action.deltaMs)
        PlayerKeyAction.ChannelPrevious -> PlayerCommand.ChannelPrevious
        PlayerKeyAction.ChannelNext -> PlayerCommand.ChannelNext
        else -> null
    }
}
