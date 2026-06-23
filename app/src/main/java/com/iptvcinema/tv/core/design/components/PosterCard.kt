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
import androidx.compose.foundation.shape.RoundedCornerShape
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
) {
    val (width, imageHeight, titleMaxLines, captionMinHeight) = when (variant) {
        PosterCardVariant.PortraitPoster -> PosterDimensions(160.dp, 240.dp, 2, 44.dp)
        PosterCardVariant.LandscapePoster -> PosterDimensions(240.dp, 135.dp, 1, 40.dp)
        PosterCardVariant.CompactPoster -> PosterDimensions(120.dp, 180.dp, 2, 40.dp)
    }
    val innerWidth = width - 12.dp

    FocusableCinemaCard(
        modifier = modifier.width(width),
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
                    .height(imageHeight)
                    .clip(CinemaShapes.Medium)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(CinemaColors.GoldDeep.copy(alpha = 0.28f), CinemaColors.Surface),
                        ),
                    ),
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
                            .padding(8.dp),
                        backgroundColor = CinemaColors.GoldDeep,
                    )
                }

                if (data.progress != null && data.progress > 0f) {
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
                                .fillMaxWidth(data.progress.coerceIn(0f, 1f))
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
                    text = data.title,
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.GoldSoft),
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )

                when {
                    !data.subtitle.isNullOrBlank() -> {
                        Text(
                            text = data.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    else -> {
                        val metadata = listOfNotNull(data.year, data.runtime).joinToString(" · ")
                        if (metadata.isNotEmpty()) {
                            Text(
                                text = metadata,
                                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PosterDimensions(
    val width: Dp,
    val imageHeight: Dp,
    val titleMaxLines: Int,
    val captionMinHeight: Dp,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BadgeChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color = CinemaColors.LiveRed,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.TextPrimary,
            ),
        )
    }
}
