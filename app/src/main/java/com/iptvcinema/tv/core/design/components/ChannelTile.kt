package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

private val tileWidth = 160.dp
private val tileImageSize = 160.dp
private val captionMinHeight = 44.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelTile(
    data: ChannelTileData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val innerWidth = tileWidth - 12.dp

    FocusableCinemaCard(
        modifier = modifier
            .width(tileWidth)
            .then(
                if (data.isNowPlaying) {
                    Modifier.border(2.dp, CinemaColors.Gold, CinemaShapes.Medium)
                } else {
                    Modifier
                },
            ),
        onClick = onClick,
        shape = CinemaShapes.Medium,
    ) { _ ->
        Column(
            modifier = Modifier
                .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                .border(1.dp, CinemaColors.Border, CinemaShapes.Medium)
                .padding(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(innerWidth)
                    .height(tileImageSize)
                    .clip(CinemaShapes.Medium)
                    .background(
                        Brush.verticalGradient(
                            listOf(CinemaColors.GoldDeep.copy(alpha = 0.28f), CinemaColors.Surface),
                        ),
                    ),
            ) {
                CinemaAsyncImage(
                    imageUrl = data.logoUrl,
                    contentDescription = data.channelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = data.channelName,
                )

                if (data.isNowPlaying) {
                    BadgeChip(
                        text = stringResource(R.string.badge_now),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        backgroundColor = CinemaColors.GoldDeep,
                    )
                } else if (data.isLive) {
                    BadgeChip(
                        text = stringResource(R.string.badge_live),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    )
                }

                if (data.qualityBadge != null) {
                    BadgeChip(
                        text = data.qualityBadge,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        backgroundColor = CinemaColors.GoldDeep,
                    )
                }

                if (data.programProgress != null && data.programProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(CinemaColors.Surface.copy(alpha = 0.7f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(data.programProgress.coerceIn(0f, 1f))
                                .background(CinemaColors.Gold),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = captionMinHeight)
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = data.channelName,
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.GoldSoft),
                    maxLines = 2,
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
