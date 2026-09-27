package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.design.theme.CinemaColors
import kotlinx.coroutines.delay

/** Thin rotating arc, the quiet YouTube-style loading indicator. */
@Composable
fun CinemaSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = CinemaColors.White,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "spinnerRotation",
    )
    val sweep by transition.animateFloat(
        initialValue = 40f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "spinnerSweep",
    )
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
    ) {
        val stroke = strokeWidth.toPx()
        drawArc(
            color = color.copy(alpha = 0.18f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/**
 * True only after [visible] has stayed true for [delayMs]. Hides short buffering
 * blips so indicators do not flash on every small network hiccup.
 */
@Composable
fun rememberDelayedVisibility(visible: Boolean, delayMs: Long): Boolean {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs)
            shown = true
        } else {
            shown = false
        }
    }
    return shown && visible
}
