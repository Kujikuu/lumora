package com.iptvcinema.tv.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.iptvcinema.tv.core.util.isolateDirection

enum class HomeCardStyle {
    /** 2:3 poster for catalog rails; the title lives in the spotlight panel. */
    Poster,
    /** 16:9 frame for Continue Watching and Next episode, with a progress bar. */
    Landscape,
    /** 16:9 channel tile: logo or initial, name, and what is on now. */
    Channel,
    /** 16:9 category tile: dimmed artwork from the category under its name. */
    Category,
}

internal fun HomeCardStyle.width() = when (this) {
    HomeCardStyle.Poster -> HomeDimens.PosterWidth
    HomeCardStyle.Landscape, HomeCardStyle.Channel, HomeCardStyle.Category -> HomeDimens.LandscapeWidth
}

internal fun HomeCardStyle.height() = when (this) {
    HomeCardStyle.Poster -> HomeDimens.PosterHeight
    HomeCardStyle.Landscape, HomeCardStyle.Channel, HomeCardStyle.Category -> HomeDimens.LandscapeHeight
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
        shape = CinemaShapes.Card,
        contentDescription = card.title,
    ) { _ ->
        when (style) {
            HomeCardStyle.Channel -> ChannelTile(card)
            HomeCardStyle.Category -> CategoryTile(card)
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

/** Logo (or the name's first letter when the provider has none), name, and a live line. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelTile(card: HomeContentCard) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CinemaAsyncImage(
                imageUrl = card.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(ChannelLogoSize)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.SurfaceRaised),
                contentScale = ContentScale.Fit,
                fallbackLabel = card.title.trim().take(1),
                showLoadingSkeleton = false,
            )
            Text(
                text = card.title.isolateDirection(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CinemaColors.White),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        card.subtitle?.takeIf { it.isNotBlank() }?.let { now ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).background(CinemaColors.LiveRed, CircleShape))
                Text(
                    text = now.isolateDirection(),
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val ChannelLogoSize = 40.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryTile(card: HomeContentCard) {
    Box(modifier = Modifier.fillMaxSize().background(CinemaColors.Surface)) {
        val artwork = card.backdropUrl ?: card.imageUrl
        if (artwork != null) {
            CinemaAsyncImage(
                imageUrl = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                showLoadingSkeleton = false,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background.copy(alpha = 0.62f)),
        )
        Text(
            text = card.title.isolateDirection(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 14.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
                textAlign = TextAlign.Center,
                shadow = Shadow(color = CinemaColors.Background, offset = Offset(0f, 1f), blurRadius = 6f),
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Rounded bar inset from the card edges, drawn in one pass. */
@Composable
private fun CardProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .height(4.dp)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(CinemaColors.White.copy(alpha = 0.3f), cornerRadius = radius)
                val fillWidth = size.width * progress.coerceIn(0f, 1f)
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
                drawRoundRect(
                    CinemaColors.Accent,
                    topLeft = Offset(left, 0f),
                    size = Size(fillWidth, size.height),
                    cornerRadius = radius,
                )
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
