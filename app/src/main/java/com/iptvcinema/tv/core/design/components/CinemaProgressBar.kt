package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
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
            .roundedProgressTrack(
                fraction = { animatedFraction },
                trackColor = CinemaColors.Border,
            ),
    )
}

/**
 * Rounded track with an accent fill, drawn in one pass so progress ticks only redraw it.
 * The fill grows from the start edge, so it follows RTL.
 */
internal fun Modifier.roundedProgressTrack(
    fraction: () -> Float,
    trackColor: Color,
    fillColor: Color = CinemaColors.Accent,
): Modifier = drawBehind {
    val radius = CornerRadius(size.height / 2f)
    drawRoundRect(color = trackColor, cornerRadius = radius)
    val fillWidth = size.width * fraction().coerceIn(0f, 1f)
    if (fillWidth > 0f) {
        val rtl = layoutDirection == LayoutDirection.Rtl
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(if (rtl) size.width - fillWidth else 0f, 0f),
            size = Size(fillWidth, size.height),
            cornerRadius = radius,
        )
    }
}

internal fun Modifier.roundedProgressTrack(
    fraction: Float,
    trackColor: Color,
    fillColor: Color = CinemaColors.Accent,
): Modifier = roundedProgressTrack(fraction = { fraction }, trackColor = trackColor, fillColor = fillColor)
