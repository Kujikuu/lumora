package com.iptvcinema.tv.features.player

import androidx.compose.ui.input.key.Key
import com.iptvcinema.tv.core.player.PlayerCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerKeyHandlerTest {
    @Test
    fun resolve_playPause_whenOverlayHidden() {
        val action = PlayerKeyHandler.resolve(
            key = Key.MediaPlayPause,
            isLive = false,
            isEpisode = false,
            controlsVisible = false,
            pickerOpen = false,
        )
        assertEquals(PlayerKeyAction.PlayPause, action)
    }

    @Test
    fun resolve_seekLeft_forVodWhenOverlayHidden() {
        val action = PlayerKeyHandler.resolve(
            key = Key.DirectionLeft,
            isLive = false,
            isEpisode = false,
            controlsVisible = false,
            pickerOpen = false,
        )
        assertTrue(action is PlayerKeyAction.SeekRelative)
        assertEquals(-10_000L, (action as PlayerKeyAction.SeekRelative).deltaMs)
    }

    @Test
    fun resolve_noSeekLeft_forLive() {
        assertNull(
            PlayerKeyHandler.resolve(
                key = Key.DirectionLeft,
                isLive = true,
                isEpisode = false,
                controlsVisible = false,
                pickerOpen = false,
            ),
        )
    }

    @Test
    fun resolve_episodeNext_forEpisode() {
        assertEquals(
            PlayerKeyAction.EpisodeNext,
            PlayerKeyHandler.resolve(
                key = Key.MediaNext,
                isLive = false,
                isEpisode = true,
                controlsVisible = false,
                pickerOpen = false,
            ),
        )
    }

    @Test
    fun resolve_channelNext_forLive() {
        assertEquals(
            PlayerKeyAction.ChannelNext,
            PlayerKeyHandler.resolve(
                key = Key.MediaNext,
                isLive = true,
                isEpisode = false,
                controlsVisible = false,
                pickerOpen = false,
            ),
        )
    }

    @Test
    fun resolve_returnsNull_whenPickerOpen() {
        assertNull(
            PlayerKeyHandler.resolve(
                key = Key.MediaPlayPause,
                isLive = false,
                isEpisode = false,
                controlsVisible = false,
                pickerOpen = true,
            ),
        )
    }

    @Test
    fun toCommand_mapsPlayPause() {
        assertEquals(PlayerCommand.PlayPause, PlayerKeyHandler.toCommand(PlayerKeyAction.PlayPause))
    }

    @Test
    fun toCommand_mapsEpisodeControls() {
        assertEquals(PlayerCommand.EpisodePrevious, PlayerKeyHandler.toCommand(PlayerKeyAction.EpisodePrevious))
        assertEquals(PlayerCommand.EpisodeNext, PlayerKeyHandler.toCommand(PlayerKeyAction.EpisodeNext))
    }
}
