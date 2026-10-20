package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

enum class PosterCardVariant {
    PortraitPoster,
    LandscapePoster,
    CompactPoster,
}

data class PosterCardData(
    val title: String,
    val year: String? = null,
    val runtime: String? = null,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val is4K: Boolean = false,
    val seasonCount: Int? = null,
    val hasNewEpisode: Boolean = false,
    val progress: Float? = null,
    val isFavorite: Boolean = false,
    val contentId: String? = null,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(
    data: PosterCardData,
    variant: PosterCardVariant = PosterCardVariant.PortraitPoster,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fixedWidth: Dp? = CinemaSpacing.PosterCardWidth,
    onLongClick: (() -> Unit)? = null,
    focusScale: Float = 1.06f,
    focusedBorderWidth: Dp = 2.dp,
) {
    val imageAspectRatio = when (variant) {
        PosterCardVariant.PortraitPoster -> 2f / 3f
        PosterCardVariant.LandscapePoster -> 16f / 9f
        PosterCardVariant.CompactPoster -> 2f / 3f
    }
    val cardModifier = if (fixedWidth != null) {
        modifier.width(fixedWidth)
    } else {
        modifier
    }

    Column(modifier = cardModifier) {
        FocusableCinemaCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            onLongClick = onLongClick,
            shape = CinemaShapes.Card,
            contentDescription = data.title,
            defaultBorderWidth = 0.dp,
            focusedBorderWidth = focusedBorderWidth,
            focusScale = focusScale,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspectRatio)
                    .clip(CinemaShapes.Card)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = data.imageUrl,
                    contentDescription = data.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = data.title,
                )
                if (data.is4K) {
                    BadgeChip(
                        text = stringResource(R.string.badge_4k),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        backgroundColor = CinemaColors.SurfaceGlass,
                    )
                }
                if (data.hasNewEpisode) {
                    BadgeChip(
                        text = stringResource(R.string.badge_new_episode),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        backgroundColor = CinemaColors.Accent,
                    )
                } else if (data.seasonCount != null && data.seasonCount > 0) {
                    BadgeChip(
                        text = stringResource(R.string.badge_seasons, data.seasonCount),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        backgroundColor = CinemaColors.SurfaceGlass,
                    )
                }

                if (data.progress != null && data.progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 7.dp, end = 7.dp, bottom = 7.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CinemaShapes.Pill)
                            .background(CinemaColors.White.copy(alpha = 0.3f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(data.progress.coerceIn(0f, 1f))
                                .background(CinemaColors.Accent, CinemaShapes.Pill),
                        )
                    }
                }
            }
        }
        Text(
            text = data.title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = CinemaColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CinemaSpacing.CardGap)
                .padding(top = CinemaSpacing.CardTitleTopGap, bottom = 4.dp),
        )
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BadgeChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color = CinemaColors.LiveRed,
    textColor: androidx.compose.ui.graphics.Color = CinemaColors.White,
) {
    Box(
        modifier = modifier
            .clip(CinemaShapes.Pill)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
            ),
        )
    }
}
