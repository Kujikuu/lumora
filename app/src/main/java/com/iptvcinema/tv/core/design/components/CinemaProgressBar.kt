package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.design.theme.CinemaColors

@Composable
fun CinemaProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220),
        label = "syncProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(CinemaColors.SurfaceSoft),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction)
                .background(CinemaColors.Accent),
        )
    }
}
