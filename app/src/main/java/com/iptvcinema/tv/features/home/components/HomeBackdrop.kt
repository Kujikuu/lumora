package com.iptvcinema.tv.features.home.components

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.model.MovieItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val BACKDROP_SETTLE_MS = 300L
// Short, so only a few frames draw two full-screen images at once.
private const val BACKDROP_FADE_MS = 250
// Half of 1080p: the backdrop sits under dark scrims, so the lost detail is invisible, and it
// halves decode time, bitmap memory and texture bandwidth on low-end TVs.
private const val BACKDROP_DECODE_WIDTH = 960
private const val BACKDROP_DECODE_HEIGHT = 540

/**
 * Full-screen backdrop behind the whole Home screen.
 *
 * While the viewer moves quickly through a rail the backdrop stays put. It changes only once
 * focus has rested for [BACKDROP_SETTLE_MS], so a burst of D-pad presses costs one image load,
 * not one per card.
 */
@Composable
fun HomeBackdrop(
    state: HomeSpotlightState,
    hero: List<MovieItem>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentHero by rememberUpdatedState(hero)
    var settledUrl by remember(state) {
        mutableStateOf(Snapshot.withoutReadObservation { state.current(hero)?.backdropUrl })
    }

    // Followed in an effect, not read during composition, so a focus move does not recompose
    // the backdrop; only a settled change does.
    LaunchedEffect(state) {
        snapshotFlow { state.current(currentHero)?.backdropUrl }
            .collectLatest { targetUrl ->
                if (settledUrl != null) delay(BACKDROP_SETTLE_MS)
                settledUrl = targetUrl
            }
    }

    // Warm the next hero slide so its crossfade starts from a loaded image.
    LaunchedEffect(state.heroIndex, hero) {
        if (hero.size < 2) return@LaunchedEffect
        val next = hero[(state.heroIndex + 1) % hero.size]
        val url = next.backdropUrl ?: next.imageUrl ?: return@LaunchedEffect
        context.imageLoader.enqueue(backdropRequest(context, url))
    }

    // No offscreen layer: it held a full-screen buffer in memory and re-rendered it on every
    // frame of a crossfade or image load. The image and the two scrims draw straight to screen.
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = settledUrl,
            animationSpec = tween(BACKDROP_FADE_MS),
            label = "homeBackdrop",
        ) { url ->
            // Plain AsyncImage: a dead backdrop URL leaves the dark background, not the
            // app-logo fallback, behind the spotlight text.
            val request = remember(url) { url?.let { backdropRequest(context, it) } }
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                // Top center keeps faces and title art in view on wide crops.
                alignment = Alignment.TopCenter,
            )
        }
        HomeScrims()
    }
}

private fun backdropRequest(context: Context, url: String): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(BACKDROP_DECODE_WIDTH, BACKDROP_DECODE_HEIGHT)
        .build()

/** Side scrim for the spotlight text and a strong bottom scrim for the rails, drawn in one pass. */
@Composable
private fun HomeScrims() {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val bottomBrush = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.35f to Color.Transparent,
            0.58f to CinemaColors.Background.copy(alpha = 0.82f),
            0.72f to CinemaColors.Background.copy(alpha = 0.96f),
            1f to CinemaColors.Background,
        )
    }
    val sideBrush = remember(isRtl) {
        val stops = arrayOf(
            0f to CinemaColors.Background.copy(alpha = 0.95f),
            0.4f to CinemaColors.Background.copy(alpha = 0.55f),
            0.7f to Color.Transparent,
            1f to Color.Transparent,
        )
        if (isRtl) {
            Brush.horizontalGradient(*stops.map { (stop, color) -> (1f - stop) to color }.reversed().toTypedArray())
        } else {
            Brush.horizontalGradient(*stops)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(sideBrush)
                drawRect(bottomBrush)
            },
    )
}
