package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FocusAwareChannelRail(
    title: String,
    items: List<ChannelTileData>,
    modifier: Modifier = Modifier,
    countLabel: String? = null,
    firstItemFocusRequester: FocusRequester? = null,
    onChannelClick: (ChannelTileData) -> Unit,
) {
    val scrollState = rememberScrollState()
    val sectionBringIntoViewRequester = remember(title) { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var hadFocusInRail by remember(title) { mutableStateOf(false) }
    val tileStridePx = remember(density) {
        with(density) { (140.dp + CinemaSpacing.RailGap).roundToPx() }
    }

    Column(
        modifier = modifier
            .bringIntoViewRequester(sectionBringIntoViewRequester)
            .padding(bottom = CinemaSpacing.CardGap),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = CinemaSpacing.NavRailWidth + 16.dp,
                    end = CinemaSpacing.ScreenPadding,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CinemaColors.TextPrimary,
                ),
            )
            if (!countLabel.isNullOrBlank()) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .onFocusChanged { if (!it.hasFocus) hadFocusInRail = false }
                .padding(
                    start = CinemaSpacing.NavRailWidth + 16.dp,
                    end = CinemaSpacing.ScreenPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
        ) {
            items.forEachIndexed { index, channel ->
                ChannelTile(
                    data = channel,
                    onClick = { onChannelClick(channel) },
                    modifier = Modifier
                        .then(
                            if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    val enteringRail = !hadFocusInRail
                                    hadFocusInRail = true
                                    if (enteringRail) {
                                        sectionBringIntoViewRequester.bringIntoView()
                                    }
                                    scrollState.scrollTo((index * tileStridePx).coerceAtLeast(0))
                                }
                            }
                        },
                )
            }
        }
    }
}
