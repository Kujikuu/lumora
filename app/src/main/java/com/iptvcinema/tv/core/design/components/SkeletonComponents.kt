package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
    width: Dp = Dp.Unspecified,
) {
    Box(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .then(if (height != Dp.Unspecified) Modifier.height(height) else Modifier)
            .shimmer(CinemaShapes.Card),
    )
}

/**
 * Draws a skeleton base with a soft highlight sweeping left to right. The sweep is
 * positioned from the shared frame clock and the element's place on screen, so every
 * skeleton on a page moves as one band instead of pulsing out of sync. Only the draw
 * phase reads the animation, so it never triggers recomposition.
 */
fun Modifier.shimmer(shape: Shape = CinemaShapes.Card): Modifier = composed {
    var frameTimeMs by remember { mutableLongStateOf(0L) }
    var xInWindow by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameTimeMs = it }
        }
    }
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val bandPx = with(density) { ShimmerBandWidth.toPx() }
    this
        .onGloballyPositioned { xInWindow = it.positionInWindow().x }
        .clip(shape)
        .drawBehind {
            drawRect(ShimmerBase)
            val progress = (frameTimeMs % ShimmerPeriodMs) / ShimmerPeriodMs.toFloat()
            val bandCenter = -bandPx + progress * (screenWidthPx + bandPx * 2) - xInWindow
            drawRect(
                brush = Brush.linearGradient(
                    colors = ShimmerHighlightColors,
                    start = Offset(bandCenter - bandPx, 0f),
                    end = Offset(bandCenter + bandPx, size.height),
                ),
            )
        }
}

private const val ShimmerPeriodMs = 1_400L
private val ShimmerBandWidth = 220.dp
private val ShimmerBase = CinemaColors.TextPrimary.copy(alpha = 0.07f)
private val ShimmerHighlightColors = listOf(
    Color.Transparent,
    CinemaColors.TextPrimary.copy(alpha = 0.09f),
    Color.Transparent,
)

@Composable
fun SkeletonPosterRail(count: Int = 7, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        repeat(count) {
            SkeletonBox(width = 148.dp, height = 222.dp)
        }
    }
}

@Composable
fun SkeletonPosterGrid(columns: Int = 5, rows: Int = 2, modifier: Modifier = Modifier) {
    // Same column count and poster ratio as the catalog grid, so content lands in place.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            ) {
                repeat(columns) {
                    SkeletonBox(
                        modifier = Modifier.weight(1f).aspectRatio(2f / 3f),
                        height = Dp.Unspecified,
                    )
                }
            }
        }
    }
}

@Composable
fun SkeletonChannelRow(count: Int = 7, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        repeat(count) {
            SkeletonBox(width = 148.dp, height = 100.dp)
        }
    }
}

@Composable
fun SkeletonEpgGrid(rows: Int = 6, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 28.dp)
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SkeletonBox(width = 120.dp, height = 48.dp)
                SkeletonBox(modifier = Modifier.weight(1f), height = 48.dp)
                SkeletonBox(modifier = Modifier.weight(1.5f), height = 48.dp)
                SkeletonBox(modifier = Modifier.weight(1f), height = 48.dp)
            }
        }
    }
}

/** Mirrors the Home layout: spotlight text and buttons on top, then two rails. */
@Composable
fun SkeletonHomeContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = CinemaSpacing.ContentStart, top = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBox(width = 80.dp, height = 14.dp)
        SkeletonBox(width = 360.dp, height = 36.dp)
        SkeletonBox(width = 240.dp, height = 14.dp)
        SkeletonBox(width = 420.dp, height = 14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            SkeletonBox(width = 132.dp, height = 40.dp)
            SkeletonBox(width = 110.dp, height = 40.dp)
            SkeletonBox(width = 110.dp, height = 40.dp)
        }
        repeat(2) {
            Column(
                modifier = Modifier.padding(top = CinemaSpacing.SectionGap),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBox(width = 160.dp, height = 18.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(8) { SkeletonBox(width = 104.dp, height = 156.dp) }
                }
            }
        }
    }
}

@Composable
fun SkeletonProfileRow(count: Int = 3, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
    ) {
        repeat(count) {
            SkeletonBox(width = 120.dp, height = 120.dp)
        }
    }
}

@Composable
fun SkeletonSourceCards(count: Int = 2, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), height = 80.dp)
        }
    }
}

@Composable
fun SkeletonDetailHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 400.dp)
        SkeletonBox(width = 300.dp, height = 32.dp)
        SkeletonBox(width = 400.dp, height = 14.dp)
        SkeletonBox(width = 500.dp, height = 14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            SkeletonBox(width = 120.dp, height = 40.dp)
            SkeletonBox(width = 100.dp, height = 40.dp)
        }
    }
}

@Composable
fun SkeletonEpisodeList(count: Int = 4, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 64.dp)
        }
    }
}
