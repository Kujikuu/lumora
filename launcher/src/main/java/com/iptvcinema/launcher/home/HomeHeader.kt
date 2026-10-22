package com.iptvcinema.launcher.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.launcher.LauncherGraph
import com.iptvcinema.launcher.R
import com.iptvcinema.launcher.system.ClockFormatter
import com.iptvcinema.launcher.system.NetworkState
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

/** Clock, network state and the shortcut pills. Down from any pill goes to the first row. */
@Composable
fun HomeHeader(
    entryRequester: FocusRequester,
    onHeaderFocused: () -> Unit,
    onDown: () -> Unit,
    shortcuts: List<HeaderShortcut>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeDimens.HeaderHeight)
            .padding(horizontal = HomeDimens.ScreenPadding)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> { onDown(); true }
                    Key.DirectionUp -> true
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Clock()
        Spacer(Modifier.weight(1f))
        NetworkBadge()
        shortcuts.forEachIndexed { index, shortcut ->
            ShortcutPill(
                label = shortcut.label,
                onClick = shortcut.onClick,
                modifier = Modifier
                    .then(if (index == 0) Modifier.focusRequester(entryRequester) else Modifier)
                    .onFocusChanged { if (it.isFocused) onHeaderFocused() },
            )
        }
    }
}

data class HeaderShortcut(val label: String, val onClick: () -> Unit)

/** Reads the minute state on its own, so a tick redraws these two lines and nothing else. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Clock() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { configuration.locales[0] }
    val is24Hour = remember(configuration) { DateFormat.is24HourFormat(context) }
    val nowMs = LauncherGraph.clock.nowMs
    Column {
        Text(
            text = ClockFormatter.format(nowMs, locale, is24Hour),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CinemaColors.TextPrimary),
        )
        Text(
            text = ClockFormatter.formatDate(nowMs, locale),
            style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetworkBadge() {
    val state by LauncherGraph.network.state.collectAsState()
    val label = when (state) {
        NetworkState.Wifi -> stringResource(R.string.network_wifi)
        NetworkState.Ethernet -> stringResource(R.string.network_ethernet)
        NetworkState.Other -> stringResource(R.string.network_wifi)
        NetworkState.Offline -> stringResource(R.string.network_offline)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (state == NetworkState.Offline) CinemaColors.Danger else CinemaColors.Success, CircleShape),
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextSecondary))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ShortcutPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusableCinemaCard(
        modifier = modifier,
        onClick = onClick,
        shape = CinemaShapes.Pill,
        focusedBorderWidth = 0.dp,
        focusScale = 1.04f,
        contentDescription = label,
    ) { focused ->
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (focused) CinemaColors.Background else CinemaColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier
                .background(if (focused) CinemaColors.White else CinemaColors.OverlayFill, CinemaShapes.Pill)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            maxLines = 1,
        )
    }
}
