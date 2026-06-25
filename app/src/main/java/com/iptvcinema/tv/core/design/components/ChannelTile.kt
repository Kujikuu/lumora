package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelTile(
    data: ChannelTileData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.width(148.dp),
        onClick = onClick,
        shape = CinemaShapes.Medium,
        contentDescription = data.channelName,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Column {
            Box(
                modifier = Modifier
                    .width(148.dp)
                    .height(100.dp)
                    .clip(CinemaShapes.Medium)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = data.logoUrl,
                    contentDescription = data.channelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = data.channelName,
                )

                if (data.isLive) {
                    BadgeChip(
                        text = if (data.isNowPlaying) stringResource(R.string.badge_now) else stringResource(R.string.badge_live),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }

                if (data.programProgress != null && data.programProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(CinemaColors.SurfaceSoft),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(data.programProgress.coerceIn(0f, 1f))
                                .background(CinemaColors.Accent),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = data.channelName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (focused) CinemaColors.White else CinemaColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (data.currentProgram != null) {
                    Text(
                        text = data.currentProgram,
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
