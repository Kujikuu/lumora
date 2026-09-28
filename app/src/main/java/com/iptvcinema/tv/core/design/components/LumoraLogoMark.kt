package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.GraphikArabicFontFamily

/** The four color bands behind the card, top to bottom. `step` is the offset in band units. */
enum class LumoraBand(val color: Color, val step: Int) {
    Yellow(CinemaColors.LumoraYellow, -2),
    Red(CinemaColors.LumoraRed, -1),
    Blue(CinemaColors.LumoraBlue, 1),
    Cyan(CinemaColors.LumoraCyan, 2),
}

// Geometry in source-PNG pixels: a 132 x 117 card, bands peeking out 16 px per step.
private const val CARD_WIDTH_UNITS = 132f
private const val CARD_HEIGHT_UNITS = 117f
private const val BAND_STEP_UNITS = 16f
private const val CORNER_UNITS = 40f
private const val MARK_HEIGHT_UNITS = CARD_HEIGHT_UNITS + BAND_STEP_UNITS * 4
private const val BAND_SLIDE_STEPS = 2.5f
private const val CARD_START_SCALE = 0.85f
private const val GLYPH_SIZE_RATIO = 0.5f

// Outer bands first so the inner ones sit on top, matching the static logo.
private val BandDrawOrder = listOf(LumoraBand.Yellow, LumoraBand.Cyan, LumoraBand.Red, LumoraBand.Blue)

/**
 * Lumora "tv" mark drawn in code so each layer can animate on its own.
 * Progress lambdas are read in the draw/layer phase only, so animating them never recomposes.
 * 0 = hidden, 1 = resting position; springs may overshoot past 1.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LumoraLogoMark(
    modifier: Modifier = Modifier,
    width: Dp = 88.dp,
    bandProgress: (LumoraBand) -> Float = { 1f },
    cardProgress: () -> Float = { 1f },
) {
    val height = width * (MARK_HEIGHT_UNITS / CARD_WIDTH_UNITS)
    val glyphSize = with(LocalDensity.current) {
        (width * (CARD_HEIGHT_UNITS / CARD_WIDTH_UNITS) * GLYPH_SIZE_RATIO).toSp()
    }

    Box(modifier = modifier.size(width, height), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val unit = size.width / CARD_WIDTH_UNITS
            BandDrawOrder.forEach { band -> drawBand(band, bandProgress(band), unit) }
            drawCard(cardProgress(), unit)
        }
        Text(
            text = "tv",
            modifier = Modifier.graphicsLayer {
                val progress = cardProgress()
                val scale = cardScale(progress)
                scaleX = scale
                scaleY = scale
                alpha = progress.coerceIn(0f, 1f)
            },
            style = TextStyle(
                color = Color.Black,
                fontFamily = GraphikArabicFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = glyphSize,
            ),
        )
    }
}

private fun cardScale(progress: Float): Float = CARD_START_SCALE + (1f - CARD_START_SCALE) * progress

private fun DrawScope.cardTopLeft(unit: Float, offsetY: Float): Offset =
    Offset(0f, (size.height - CARD_HEIGHT_UNITS * unit) / 2f + offsetY)

private fun DrawScope.drawBand(band: LumoraBand, progress: Float, unit: Float) {
    val alpha = progress.coerceIn(0f, 1f)
    if (alpha == 0f) return
    val restingOffset = band.step * BAND_STEP_UNITS * unit
    val direction = if (band.step < 0) -1f else 1f
    val slide = direction * BAND_SLIDE_STEPS * BAND_STEP_UNITS * unit * (1f - progress)
    drawRoundRect(
        color = band.color,
        topLeft = cardTopLeft(unit, restingOffset + slide),
        size = Size(CARD_WIDTH_UNITS * unit, CARD_HEIGHT_UNITS * unit),
        cornerRadius = CornerRadius(CORNER_UNITS * unit),
        alpha = alpha,
    )
}

private fun DrawScope.drawCard(progress: Float, unit: Float) {
    val alpha = progress.coerceIn(0f, 1f)
    if (alpha == 0f) return
    scale(cardScale(progress)) {
        drawRoundRect(
            color = Color.White,
            topLeft = cardTopLeft(unit, 0f),
            size = Size(CARD_WIDTH_UNITS * unit, CARD_HEIGHT_UNITS * unit),
            cornerRadius = CornerRadius(CORNER_UNITS * unit),
            alpha = alpha,
        )
    }
}
