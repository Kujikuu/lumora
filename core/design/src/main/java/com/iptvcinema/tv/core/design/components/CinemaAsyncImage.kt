package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.util.isolateDirection

/**
 * Static loading fill drawn by Coil itself while the image loads. A painter instead of a
 * modifier toggled from load callbacks, so a finished load never recomposes the card.
 */
private val LoadingPlaceholder = ColorPainter(SkeletonFill)

@Composable
fun CinemaAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackLabel: String = contentDescription.orEmpty(),
    showLoadingSkeleton: Boolean = true,
    /** Where a cropped image anchors; backdrops use top center so faces and titles stay in view. */
    alignment: Alignment = Alignment.Center,
) {
    if (imageUrl.isNullOrBlank()) {
        ImageFallback(
            label = fallbackLabel,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    var loadFailed by remember(imageUrl) { mutableStateOf(false) }
    if (loadFailed) {
        ImageFallback(
            label = fallbackLabel,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    // The composable is sized by its caller, so Coil decodes at the card's size, not the source's.
    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier.fillMaxSize(),
        placeholder = if (showLoadingSkeleton) LoadingPlaceholder else null,
        contentScale = contentScale,
        alignment = alignment,
        onError = { loadFailed = true },
    )
}

/**
 * Shown for a missing or dead image: a surface tile with the title. Dead links are common in
 * IPTV, so this must stay cheap; it allocates no bitmap, unlike the full-size banner it replaces.
 * [contentScale] is kept so callers stay unchanged; a text tile has nothing to scale.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImageFallback(
    label: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label.isolateDirection(),
                modifier = Modifier.padding(horizontal = 10.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CinemaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelImageFallback(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChannelFallbackBrush, CinemaShapes.Medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(2).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.GoldSoft,
            ),
        )
    }
}

/** Shared so each channel tile does not build its own gradient on every composition. */
private val ChannelFallbackBrush = Brush.verticalGradient(
    listOf(CinemaColors.GoldDeep.copy(alpha = 0.22f), CinemaColors.SurfaceSoft),
)
