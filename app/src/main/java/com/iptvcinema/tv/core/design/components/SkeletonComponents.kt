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
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .height(height)
            .clip(CinemaShapes.Small)
            .background(CinemaColors.TextPrimary.copy(alpha = alpha)),
    )
}

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
fun SkeletonPosterGrid(columns: Int = 6, rows: Int = 2, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap)) {
                repeat(columns) {
                    SkeletonBox(width = 148.dp, height = 222.dp)
                }
            }
        }
    }
}

@Composable
fun SkeletonHeroBanner(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 400.dp)
        SkeletonBox(width = 280.dp, height = 32.dp)
        SkeletonBox(width = 400.dp, height = 16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            SkeletonBox(width = 120.dp, height = 40.dp)
            SkeletonBox(width = 100.dp, height = 40.dp)
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

@Composable
fun SkeletonHomeContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
    ) {
        SkeletonHeroBanner()
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBox(width = 160.dp, height = 18.dp)
                SkeletonPosterRail(count = 7)
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
