package com.iptvcinema.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.launcher.home.HomeDimens.cardHeight
import com.iptvcinema.launcher.home.HomeDimens.cardWidth
import com.iptvcinema.launcher.home.HomeDimens.hasTitleBelow
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.components.ImageFallback
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.util.isolateDirection

/**
 * One card of any row. Focus only scales it and draws a border inside [FocusableCinemaCard]'s
 * graphics layer, so a focus move never changes layout.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeCardView(
    card: HomeCard,
    style: RowStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.width(style.cardWidth())) {
        FocusableCinemaCard(
            modifier = Modifier.size(width = style.cardWidth(), height = style.cardHeight()),
            onClick = onClick,
            onLongClick = onLongClick,
            shape = CinemaShapes.Card,
            contentDescription = card.title,
        ) { focused ->
            when {
                card.isPlaceholder -> PlaceholderTile(card.title)
                style == RowStyle.Channel -> ChannelTile(card, focused)
                style == RowStyle.Input -> InputTile(card, focused)
                else -> Artwork(card)
            }
            card.progress?.let { ProgressLine(it, Modifier.align(Alignment.BottomCenter)) }
        }
        if (style.hasTitleBelow()) {
            Text(
                text = if (card.isPlaceholder) "" else card.title.isolateDirection(),
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeDimens.CardTitleHeight)
                    .padding(top = 4.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

@Composable
private fun Artwork(card: HomeCard) {
    CinemaAsyncImage(
        imageUrl = card.image,
        contentDescription = card.title,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        fallbackLabel = card.title,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaceholderTile(label: String) {
    ImageFallback(label = label, modifier = Modifier.fillMaxSize())
}

/** Logo (or the name's first letter), name and what is on now. Focused: white tile, dark text. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelTile(card: HomeCard, focused: Boolean) {
    val textColor = if (focused) CinemaColors.Background else CinemaColors.TextPrimary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (focused) CinemaColors.White else CinemaColors.Surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.SurfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                if (card.image.isNullOrBlank()) {
                    Text(
                        text = card.title.take(1).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = CinemaColors.AccentSoft),
                    )
                } else {
                    CinemaAsyncImage(
                        imageUrl = card.image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        fallbackLabel = card.title.take(1),
                        showLoadingSkeleton = false,
                    )
                }
            }
            Text(
                text = card.title.isolateDirection(),
                style = MaterialTheme.typography.labelMedium.copy(color = textColor, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = card.subtitle.orEmpty().isolateDirection(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (focused) textColor.copy(alpha = 0.7f) else CinemaColors.TextSecondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Type mark ("HDMI"), the input's name, and a dot for its connection state. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InputTile(card: HomeCard, focused: Boolean) {
    val textColor = if (focused) CinemaColors.Background else CinemaColors.TextPrimary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (focused) CinemaColors.White else CinemaColors.Surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = card.mark.orEmpty(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = textColor),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            card.badge?.let { badge ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (badge == CardBadge.Connected) CinemaColors.Success else CinemaColors.TextMuted,
                            CircleShape,
                        ),
                )
            }
            Text(
                text = card.title.isolateDirection(),
                style = MaterialTheme.typography.labelSmall.copy(color = if (focused) textColor else CinemaColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, modifier: Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(3.dp)
            .clip(CinemaShapes.Pill)
            .background(CinemaColors.White.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(CinemaColors.Accent, CinemaShapes.Pill),
        )
    }
}

@Suppress("unused")
private val textAlignCenter = TextAlign.Center
