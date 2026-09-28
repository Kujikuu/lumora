package com.iptvcinema.tv.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.model.home.HomeContentCard

enum class HomeCardStyle {
    /** 2:3 poster for catalog rails; the title lives in the spotlight panel. */
    Poster,
    /** 16:9 frame for Continue Watching and Next episode, with a progress bar. */
    Landscape,
    /** 16:9 tile with the channel logo fitted on a dark surface. */
    Channel,
}

internal fun HomeCardStyle.width() = when (this) {
    HomeCardStyle.Poster -> HomeDimens.PosterWidth
    HomeCardStyle.Landscape, HomeCardStyle.Channel -> HomeDimens.LandscapeWidth
}

internal fun HomeCardStyle.height() = when (this) {
    HomeCardStyle.Poster -> HomeDimens.PosterHeight
    HomeCardStyle.Landscape, HomeCardStyle.Channel -> HomeDimens.LandscapeHeight
}

/**
 * One Home card. Focus only scales it and adds a border (both in [FocusableCinemaCard]'s
 * graphics layer), so a focus move never changes layout.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeCard(
    card: HomeContentCard,
    style: HomeCardStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    FocusableCinemaCard(
        modifier = modifier.size(width = style.width(), height = style.height()),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CinemaShapes.Medium,
        contentDescription = card.title,
    ) { _ ->
        when (style) {
            HomeCardStyle.Channel -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                CinemaAsyncImage(
                    imageUrl = card.imageUrl,
                    contentDescription = card.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 18.dp),
                    contentScale = ContentScale.Fit,
                    fallbackLabel = card.title,
                    showLoadingSkeleton = false,
                )
            }
            HomeCardStyle.Landscape -> CinemaAsyncImage(
                imageUrl = card.imageUrl ?: card.backdropUrl,
                contentDescription = card.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallbackLabel = card.title,
            )
            HomeCardStyle.Poster -> CinemaAsyncImage(
                imageUrl = card.imageUrl ?: card.backdropUrl,
                contentDescription = card.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallbackLabel = card.title,
            )
        }
        card.progress?.let { progress ->
            CardProgress(progress = progress, modifier = Modifier.align(Alignment.BottomCenter))
        }
        card.rank?.let { rank ->
            RankNumber(rank = rank, modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun CardProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .drawBehind {
                drawRect(CinemaColors.Background.copy(alpha = 0.6f))
                val fillWidth = size.width * progress.coerceIn(0f, 1f)
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
                drawRect(CinemaColors.Accent, topLeft = Offset(left, 0f), size = Size(fillWidth, size.height))
            },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RankNumber(rank: Int, modifier: Modifier = Modifier) {
    Text(
        text = rank.toString(),
        modifier = modifier.padding(start = 6.dp),
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 52.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Black,
            color = CinemaColors.White,
            shadow = Shadow(color = CinemaColors.Background, offset = Offset(0f, 2f), blurRadius = 10f),
        ),
    )
}
