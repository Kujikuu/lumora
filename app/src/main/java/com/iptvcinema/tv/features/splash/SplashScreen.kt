package com.iptvcinema.tv.features.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.LumoraBand
import com.iptvcinema.tv.core.design.components.LumoraLogoMark
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.GraphikArabicFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object SplashTimeline {
    const val BAND_STAGGER_MS = 90L
    const val CARD_DELAY_MS = 420L
    const val CARD_MS = 340
    const val WORDMARK_DELAY_MS = 800L
    const val WORDMARK_MS = 520
    const val HOLD_MS = 380L
    const val PULSE_MS = 700
    const val PULSE_SCALE = 1.04f
    const val WORDMARK_SLIDE_DP = 24
}

private val OvershootEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val LogoGap = 28.dp

private class SplashAnimations(startValue: Float) {
    val bands = LumoraBand.entries.associateWith { Animatable(startValue) }
    val card = Animatable(startValue)
    val wordmark = Animatable(startValue)
    val pulse = Animatable(1f)
}

/**
 * Branded intro: the logo bands stack in, the "tv" card pops, then the wordmark reveals.
 * Calls [onFinished] once the intro has played and [isReady] is true.
 */
@Composable
fun SplashScreen(
    isReady: Boolean,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val skipIntro = remember {
        SplashExitPolicy.shouldSkipIntro(
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
        )
    }
    val animations = remember { SplashAnimations(startValue = if (skipIntro) 1f else 0f) }
    var introFinished by remember { mutableStateOf(skipIntro) }
    val phase = SplashExitPolicy.phase(introFinished, destinationReady = isReady)
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        if (!skipIntro) {
            animations.playIntro()
            delay(SplashTimeline.HOLD_MS)
        }
        introFinished = true
    }

    LaunchedEffect(phase) {
        when (phase) {
            SplashPhase.Intro -> Unit
            SplashPhase.Waiting -> animations.pulseForever(skipIntro)
            SplashPhase.Exit -> {
                animations.pulse.snapTo(1f)
                currentOnFinished()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        SplashLockup(animations)
    }
}

@Composable
private fun SplashLockup(animations: SplashAnimations) {
    var wordmarkWidthPx by remember { mutableIntStateOf(0) }
    val gapPx = with(LocalDensity.current) { LogoGap.toPx() }

    // The brand lockup reads left to right in every locale.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer {
                // Keep the mark centered until the wordmark starts sliding in beside it.
                val reveal = animations.wordmark.value.coerceIn(0f, 1f)
                translationX = (1f - reveal) * (wordmarkWidthPx + gapPx) / 2f
                scaleX = animations.pulse.value
                scaleY = animations.pulse.value
            },
        ) {
            LumoraLogoMark(
                bandProgress = { band -> animations.bands.getValue(band).value },
                cardProgress = { animations.card.value },
            )
            Spacer(modifier = Modifier.width(LogoGap))
            SplashWordmark(
                progress = { animations.wordmark.value },
                modifier = Modifier.onSizeChanged { wordmarkWidthPx = it.width },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SplashWordmark(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .graphicsLayer {
                val reveal = progress().coerceIn(0f, 1f)
                alpha = reveal
                translationX = -(1f - reveal) * SplashTimeline.WORDMARK_SLIDE_DP.dp.toPx()
            }
            .drawWithContent {
                clipRect(right = size.width * progress().coerceIn(0f, 1f)) {
                    this@drawWithContent.drawContent()
                }
            },
    ) {
        Text(
            text = stringResource(R.string.splash_brand_name),
            style = TextStyle(
                color = CinemaColors.TextPrimary,
                fontFamily = GraphikArabicFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
                letterSpacing = 3.sp,
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.splash_lockup_tagline),
            style = TextStyle(
                color = CinemaColors.TextSecondary,
                fontFamily = GraphikArabicFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
            ),
        )
    }
}

private suspend fun SplashAnimations.playIntro() = coroutineScope {
    LumoraBand.entries.forEachIndexed { index, band ->
        launch {
            delay(index * SplashTimeline.BAND_STAGGER_MS)
            bands.getValue(band).animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }
    launch {
        delay(SplashTimeline.CARD_DELAY_MS)
        card.animateTo(1f, tween(SplashTimeline.CARD_MS, easing = OvershootEasing))
    }
    launch {
        delay(SplashTimeline.WORDMARK_DELAY_MS)
        wordmark.animateTo(1f, tween(SplashTimeline.WORDMARK_MS, easing = FastOutSlowInEasing))
    }
}

// Gentle breathing while a slow bootstrap finishes, so the screen never looks frozen.
private suspend fun SplashAnimations.pulseForever(reduceMotion: Boolean) {
    if (reduceMotion) return
    while (true) {
        pulse.animateTo(SplashTimeline.PULSE_SCALE, tween(SplashTimeline.PULSE_MS, easing = FastOutSlowInEasing))
        pulse.animateTo(1f, tween(SplashTimeline.PULSE_MS, easing = FastOutSlowInEasing))
    }
}
