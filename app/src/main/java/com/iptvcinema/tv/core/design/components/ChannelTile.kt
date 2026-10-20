package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

data class ChannelTileData(
    val id: String? = null,
    val channelName: String,
    val logoUrl: String? = null,
    val currentProgram: String? = null,
    val isLive: Boolean = true,
    val isNowPlaying: Boolean = false,
    val qualityBadge: String? = null,
    val programProgress: Float? = null,
)

private val TileWidth = 172.dp
private val FocusedTrack = Color(0xFFD6D8DC)

/**
 * Compact channel tile: rounded Surface card with the logo tile, name and current
 * programme. Focused it turns white with dark text, like the channel rows.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelTile(
    data: ChannelTileData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    FocusableCinemaCard(
        modifier = modifier.width(TileWidth),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CinemaShapes.Card,
        contentDescription = data.channelName,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
    ) { focused ->
        val contentColor = if (focused) CinemaColors.Background else CinemaColors.White
        Column(
            modifier = Modifier
                .width(TileWidth)
                .background(if (focused) CinemaColors.White else CinemaColors.Surface, CinemaShapes.Card)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelLogoTile(
                    logoUrl = data.logoUrl,
                    name = data.channelName,
                    size = 32.dp,
                    shape = CinemaShapes.Large,
                )
                Text(
                    text = data.channelName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = contentColor,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Always laid out, so tiles in a rail keep one height.
            Text(
                text = data.currentProgram.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (focused) contentColor.copy(alpha = 0.75f) else CinemaColors.TextSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (data.isLive) {
                    LivePill(
                        text = if (data.isNowPlaying) {
                            stringResource(R.string.badge_now)
                        } else {
                            stringResource(R.string.badge_live)
                        },
                    )
                }
                if (data.programProgress != null && data.programProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .roundedProgressTrack(
                                fraction = data.programProgress,
                                trackColor = if (focused) FocusedTrack else CinemaColors.Border,
                            ),
                    )
                }
            }
        }
    }
}
