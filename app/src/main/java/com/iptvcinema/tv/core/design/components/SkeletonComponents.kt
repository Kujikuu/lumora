package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    val brush = Brush.verticalGradient(
        listOf(
            CinemaColors.SurfaceSoft.copy(alpha = alpha),
            CinemaColors.GoldDeep.copy(alpha = alpha * 0.25f),
        ),
    )
    Box(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .height(height)
            .clip(CinemaShapes.Medium)
            .background(brush),
    )
}

@Composable
fun SkeletonPosterRail(count: Int = 6, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        repeat(count) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(width = 160.dp, height = 240.dp)
                SkeletonBox(width = 120.dp, height = 14.dp)
            }
        }
    }
}

@Composable
fun SkeletonPosterGrid(columns: Int = 5, rows: Int = 2, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap)) {
                repeat(columns) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBox(width = 160.dp, height = 240.dp)
                        SkeletonBox(width = 100.dp, height = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonHeroBanner(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
    ) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 280.dp)
        SkeletonBox(width = 320.dp, height = 28.dp)
        SkeletonBox(width = 480.dp, height = 16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            SkeletonBox(width = 140.dp, height = 44.dp)
            SkeletonBox(width = 120.dp, height = 44.dp)
        }
    }
}

@Composable
fun SkeletonChannelRow(count: Int = 6, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        repeat(count) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(width = 140.dp, height = 100.dp)
                SkeletonBox(width = 90.dp, height = 12.dp)
            }
        }
    }
}

@Composable
fun SkeletonEpgGrid(rows: Int = 6, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 32.dp)
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBox(width = 120.dp, height = 56.dp)
                SkeletonBox(modifier = Modifier.weight(1f), height = 56.dp)
                SkeletonBox(modifier = Modifier.weight(1.5f), height = 56.dp)
                SkeletonBox(modifier = Modifier.weight(1f), height = 56.dp)
            }
        }
    }
}

@Composable
fun SkeletonHomeContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
    ) {
        SkeletonHeroBanner()
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                SkeletonBox(width = 180.dp, height = 20.dp)
                SkeletonPosterRail(count = 5)
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
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
    ) {
        repeat(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), height = 88.dp)
        }
    }
}

@Composable
fun SkeletonDetailHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
    ) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 320.dp)
        SkeletonBox(width = 360.dp, height = 32.dp)
        SkeletonBox(width = 480.dp, height = 16.dp)
        SkeletonBox(width = 520.dp, height = 16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            SkeletonBox(width = 140.dp, height = 44.dp)
            SkeletonBox(width = 120.dp, height = 44.dp)
        }
    }
}

@Composable
fun SkeletonEpisodeList(count: Int = 4, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
    ) {
        repeat(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 72.dp)
        }
    }
}
