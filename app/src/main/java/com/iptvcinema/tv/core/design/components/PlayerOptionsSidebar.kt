package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.player.PlaybackSpeedOption
import com.iptvcinema.tv.core.player.SleepTimerOption
import com.iptvcinema.tv.core.player.VideoAspectMode

/**
 * Picture, speed and sleep-timer choices in the same side panel style as the track picker.
 * [speed] is null on live streams, which cannot play faster.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOptionsSidebar(
    aspectMode: VideoAspectMode,
    speed: PlaybackSpeedOption?,
    sleepTimer: SleepTimerOption,
    onSelectAspectMode: (VideoAspectMode) -> Unit,
    onSelectSpeed: (PlaybackSpeedOption) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerSidePanel(
        title = stringResource(R.string.player_options),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 520.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "aspect-title") { OptionGroupTitle(stringResource(R.string.player_picture)) }
            items(VideoAspectMode.entries.size, key = { "aspect-$it" }) { index ->
                val mode = VideoAspectMode.entries[index]
                PlayerOptionRow(
                    label = stringResource(mode.labelRes()),
                    isSelected = mode == aspectMode,
                    onClick = { onSelectAspectMode(mode) },
                )
            }
            if (speed != null) {
                item(key = "speed-title") { OptionGroupTitle(stringResource(R.string.player_speed)) }
                items(PlaybackSpeedOption.entries.size, key = { "speed-$it" }) { index ->
                    val option = PlaybackSpeedOption.entries[index]
                    PlayerOptionRow(
                        label = option.label,
                        isSelected = option == speed,
                        onClick = { onSelectSpeed(option) },
                    )
                }
            }
            item(key = "sleep-title") { OptionGroupTitle(stringResource(R.string.player_sleep_timer)) }
            items(SleepTimerOption.entries.size, key = { "sleep-$it" }) { index ->
                val option = SleepTimerOption.entries[index]
                PlayerOptionRow(
                    label = if (option.isOff) {
                        stringResource(R.string.toggle_off)
                    } else {
                        stringResource(R.string.player_sleep_minutes, option.minutes)
                    },
                    isSelected = option == sleepTimer,
                    onClick = { onSelectSleepTimer(option) },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OptionGroupTitle(text: String) {
    Column(modifier = Modifier.heightIn(min = 36.dp), verticalArrangement = Arrangement.Bottom) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
        )
    }
}

fun VideoAspectMode.labelRes(): Int = when (this) {
    VideoAspectMode.FIT -> R.string.player_aspect_fit
    VideoAspectMode.FILL -> R.string.player_aspect_fill
    VideoAspectMode.STRETCH -> R.string.player_aspect_stretch
}
