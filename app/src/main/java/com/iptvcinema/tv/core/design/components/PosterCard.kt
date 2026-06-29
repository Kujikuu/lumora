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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    FocusableCinemaCard(
        modifier = cardModifier,
        onClick = onClick,
        shape = CinemaShapes.Medium,
        contentDescription = data.title,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspectRatio)
                    .clip(CinemaShapes.Medium)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = data.imageUrl,
                    contentDescription = data.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = data.title,
                )

                if (data.progress != null && data.progress > 0f) {
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
                                .fillMaxWidth(data.progress.coerceIn(0f, 1f))
                                .background(CinemaColors.Accent),
                        )
                    }
                }
            }
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (focused) CinemaColors.White else CinemaColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = CinemaSpacing.CardGap, end = CinemaSpacing.CardGap, top = 4.dp),
            )
        }
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BadgeChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color = CinemaColors.Accent,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
            ),
        )
    }
}
