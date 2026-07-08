package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeCardAction
import com.iptvcinema.tv.core.model.home.HomeContentCard

enum class ExpandedPosterCardVariant {
    Portrait,
    Landscape,
    /** Landscape poster artwork with title row below (Recommended Series, New Releases). */
    LandscapePoster,
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExpandedPosterCard(
    data: HomeContentCard,
    onWatchNow: () -> Unit,
    onAddToList: () -> Unit,
    onFavorite: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ExpandedPosterCardVariant = ExpandedPosterCardVariant.Portrait,
    onFocusChanged: (Boolean) -> Unit = {},
    onCardLongClick: (() -> Unit)? = null,
) {
    val cardWidth = CinemaSpacing.ExpandedPosterCardWidth
    val cardHeight = when (variant) {
        ExpandedPosterCardVariant.Landscape -> CinemaSpacing.ExpandedLandscapeCardHeight
        ExpandedPosterCardVariant.LandscapePoster -> CinemaSpacing.ExpandedLandscapePosterTotalHeight
        ExpandedPosterCardVariant.Portrait -> CinemaSpacing.ExpandedPosterCardHeight
    }
    val posterHeight = when (variant) {
        ExpandedPosterCardVariant.Landscape -> CinemaSpacing.CompactLandscapePosterHeight
        ExpandedPosterCardVariant.LandscapePoster -> CinemaSpacing.ExpandedLandscapePosterOnlyHeight
        ExpandedPosterCardVariant.Portrait -> CinemaSpacing.CompactPosterCardHeight
    }
    val panelHeight = (cardHeight - posterHeight).coerceAtLeast(CinemaSpacing.ExpandedPosterPanelMinHeight)
    val reserveSubtitleSlot = variant == ExpandedPosterCardVariant.Landscape ||
        data.subtitle?.isNotBlank() == true
    val cardLongClick = onCardLongClick ?: onFavorite

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight),
    ) {
        when (variant) {
            ExpandedPosterCardVariant.Landscape -> LandscapeHomeCard(
                data = data,
                onCardClick = onCardClick,
                onWatchNow = onWatchNow,
                onCardLongClick = cardLongClick,
                onFocusChanged = onFocusChanged,
            )
            ExpandedPosterCardVariant.LandscapePoster -> LandscapePosterCard(
                data = data,
                onCardClick = onCardClick,
                onCardLongClick = cardLongClick,
                onFocusChanged = onFocusChanged,
            )
            ExpandedPosterCardVariant.Portrait ->
                VerticalHomeCard(
                    data = data,
                    posterHeight = posterHeight,
                    panelHeight = panelHeight,
                    showTop10Badge = data.showTop10Badge,
                    titleMaxLines = 2,
                    reserveSubtitleSlot = reserveSubtitleSlot,
                    onCardClick = onCardClick,
                    onCardLongClick = cardLongClick,
                    onFocusChanged = onFocusChanged,
                )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LandscapePosterCard(
    data: HomeContentCard,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FocusableCinemaCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(CinemaSpacing.ExpandedLandscapePosterOnlyHeight)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            onClick = onCardClick,
            onLongClick = onCardLongClick,
            shape = CinemaShapes.Card,
            defaultBorderWidth = 0.dp,
            contentDescription = data.title,
            focusScale = 1.02f,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CinemaShapes.Card)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = data.backdropUrl ?: data.imageUrl,
                    contentDescription = data.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = data.title,
                )
                if (data.showTop10Badge) {
                    Top10Badge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }
            }
        }
        Text(
            text = data.title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CinemaSpacing.CardTitleTopGap),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LandscapeHomeCard(
    data: HomeContentCard,
    onCardClick: () -> Unit,
    onWatchNow: () -> Unit,
    onCardLongClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FocusableCinemaCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            onClick = onCardClick,
            onLongClick = onCardLongClick,
            shape = CinemaShapes.Card,
            defaultBorderWidth = 0.dp,
            contentDescription = data.title,
            focusScale = 1.02f,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CinemaShapes.Card)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = data.backdropUrl ?: data.imageUrl,
                    contentDescription = data.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = data.title,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    CinemaColors.Background.copy(alpha = 0.62f),
                                ),
                            ),
                        ),
                )
                if (data.showTop10Badge) {
                    Top10Badge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }
                if (data.progress != null && data.progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
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
                data.remainingTimeLabel?.takeIf { it.isNotBlank() }?.let { remainingLabel ->
                    Text(
                        text = remainingLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = CinemaColors.White,
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 10.dp)
                            .clip(CinemaShapes.Card)
                            .background(CinemaColors.Background.copy(alpha = 0.72f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CinemaSpacing.CardTitleTopGap),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.CardTitleSubtitleGap),
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CinemaColors.White,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = data.subtitle ?: data.runtimeOrEpisodes.orEmpty(),
                style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VerticalHomeCard(
    data: HomeContentCard,
    posterHeight: Dp,
    panelHeight: Dp,
    showTop10Badge: Boolean,
    titleMaxLines: Int,
    reserveSubtitleSlot: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FocusableCinemaCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(posterHeight)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            onClick = onCardClick,
            onLongClick = onCardLongClick,
            shape = CinemaShapes.Card,
            defaultBorderWidth = 0.dp,
            contentDescription = data.title,
        ) {
            PosterImageBox(
                data = data,
                modifier = Modifier.fillMaxSize(),
                showTop10Badge = showTop10Badge,
                showFavoriteBadge = data.isFavorite,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .padding(horizontal = 10.dp)
                .padding(top = 10.dp, bottom = 8.dp),
        ) {
            HomeCardInfo(
                data = data,
                titleMaxLines = titleMaxLines,
                reserveSubtitleSlot = reserveSubtitleSlot,
            )
        }
    }
}

@Composable
private fun PosterImageBox(
    data: HomeContentCard,
    modifier: Modifier,
    showTop10Badge: Boolean = false,
    showFavoriteBadge: Boolean = false,
) {
    Box(
        modifier = modifier
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            CinemaColors.Background.copy(alpha = 0.68f),
                        ),
                    ),
                ),
        )
        if (showTop10Badge) {
            Top10Badge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
        }
        if (showFavoriteBadge) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = stringResource(R.string.content_desc_favorite),
                tint = CinemaColors.Accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp),
            )
        }
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
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeCardInfo(
    data: HomeContentCard,
    titleMaxLines: Int,
    reserveSubtitleSlot: Boolean,
) {
    val metadata = buildList {
        data.year?.let { add(it) }
        data.genres.firstOrNull()?.let { add(it) }
        data.runtimeOrEpisodes?.let { add(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.CardTitleSubtitleGap),
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CinemaColors.White,
                ),
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (reserveSubtitleSlot) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    data.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (data.primaryAction == HomeCardAction.ContinueWatching && data.progress != null) {
                val percent = (data.progress * 100).toInt().coerceIn(0, 100)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CinemaColors.Success,
                        modifier = Modifier.height(12.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_continue_progress, percent),
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                data.runtimeOrEpisodes?.let { status ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CinemaColors.Success,
                            modifier = Modifier.height(12.dp),
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (metadata.isNotEmpty()) {
                MetadataRow(items = metadata)
            }
            data.highlightText
                ?.takeIf { it.isNotBlank() && it != data.subtitle }
                ?.let { highlight ->
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.AmberWarm),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
    }
}

@Composable
private fun PrimaryActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
        Row(
            modifier = modifier
                .defaultMinSize(minHeight = 36.dp)
            .clip(CinemaShapes.Pill)
            .background(CinemaColors.Accent, CinemaShapes.Pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = CinemaColors.Background,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.Background,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Top10Badge(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(CinemaShapes.Small)
            .background(CinemaColors.LiveRed)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.badge_top_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
            ),
        )
        Text(
            text = stringResource(R.string.badge_top_number),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.White,
            ),
        )
    }
}

fun ExpandedPosterCardVariant.railHeight(): Dp = when (this) {
    ExpandedPosterCardVariant.Portrait -> CinemaSpacing.ExpandedPosterCardHeight
    ExpandedPosterCardVariant.Landscape -> CinemaSpacing.ExpandedLandscapeCardHeight
    ExpandedPosterCardVariant.LandscapePoster -> CinemaSpacing.ExpandedLandscapePosterTotalHeight
}
