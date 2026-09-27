package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.imageLoader
import coil.request.ImageRequest
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.util.RatingFormatter
import kotlinx.coroutines.launch

private const val SlideDurationMs = 8_000
private const val BackdropFadeMs = 600
private const val TextFadeInMs = 300
private const val TextFadeOutMs = 150
private const val ButtonCount = 3
private val TextBlockHeight = 200.dp

/** Pure carousel rules, kept out of Compose so they can be unit tested. */
internal object HeroCarouselLogic {
    fun nextIndex(current: Int, count: Int): Int = if (count <= 0) 0 else (current + 1) % count

    /** Rating chip text, or null when the provider sent no rating or a zero placeholder. */
    fun ratingBadge(rating: String?): String? {
        val formatted = RatingFormatter.formatForDisplay(rating) ?: return null
        val numeric = formatted.toDoubleOrNull()
        if (numeric != null && numeric <= 0.0) return null
        return "★ $formatted"
    }

    fun metadata(movie: MovieItem): List<String> = listOfNotNull(
        movie.year.takeIf { it > 0 }?.toString(),
        movie.genres.take(2).joinToString(" · ").takeIf { it.isNotBlank() },
        movie.runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
    )
}

/**
 * Featured carousel for Home, modelled on YouTube for TV.
 *
 * One hero layer instead of a pager: the backdrop crossfades and the text block fades
 * between slides, while the buttons stay in place so focus is never lost. Pressing away
 * from the nav rail on the last button moves to the next slide; pressing toward it on the
 * first button goes to the rail. While focused, slides advance every 8 s with a progress
 * bar, and any key press restarts the timer.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    movies: List<MovieItem>,
    onWatchNow: (MovieItem) -> Unit,
    onDetails: (MovieItem) -> Unit,
    modifier: Modifier = Modifier,
    watchNowFocusRequester: FocusRequester? = null,
    onAddToList: ((MovieItem) -> Unit)? = null,
    onFavorite: ((MovieItem) -> Unit)? = null,
) {
    if (movies.isEmpty()) return

    var index by rememberSaveable { mutableIntStateOf(0) }
    val currentIndex = index.coerceIn(0, movies.lastIndex)
    val movie = movies[currentIndex]
    val canRotate = movies.size > 1
    val slideProgress = remember { Animatable(0f) }
    var heroFocused by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var focusedButton by remember { mutableIntStateOf(-1) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The nav rail is on the left in LTR and on the right in RTL.
    val awayFromRailKey = if (isRtl) Key.DirectionLeft else Key.DirectionRight
    val towardRailKey = if (isRtl) Key.DirectionRight else Key.DirectionLeft

    val ownPlayRequester = remember { FocusRequester() }
    val buttonRequesters = listOf(
        watchNowFocusRequester ?: ownPlayRequester,
        remember { FocusRequester() },
        remember { FocusRequester() },
    )

    fun advance() {
        index = HeroCarouselLogic.nextIndex(currentIndex, movies.size)
    }

    // Restarts on every slide change, focus change and key press, so the timer only runs
    // while the user is idle on the hero.
    LaunchedEffect(currentIndex, heroFocused, interactionTick, canRotate) {
        slideProgress.snapTo(0f)
        if (!canRotate || !heroFocused) return@LaunchedEffect
        slideProgress.animateTo(1f, tween(durationMillis = SlideDurationMs, easing = LinearEasing))
        advance()
    }

    // Warm the next backdrop so the crossfade shows a loaded image, not a shimmer.
    LaunchedEffect(currentIndex, movies) {
        if (!canRotate) return@LaunchedEffect
        val next = movies[HeroCarouselLogic.nextIndex(currentIndex, movies.size)]
        val url = next.backdropUrl ?: next.imageUrl ?: return@LaunchedEffect
        context.imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CinemaSpacing.HeroMinHeight, max = CinemaSpacing.HeroMaxHeight)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { state ->
                heroFocused = state.hasFocus
                if (state.hasFocus) scope.launch { bringIntoViewRequester.bringIntoView() }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                interactionTick++
                // Buttons move explicitly so focus never jumps diagonally into the rails below.
                when {
                    event.key == awayFromRailKey && focusedButton == ButtonCount - 1 -> {
                        if (canRotate) advance()
                        true
                    }
                    event.key == awayFromRailKey && focusedButton in 0 until ButtonCount - 1 -> {
                        runCatching { buttonRequesters[focusedButton + 1].requestFocus() }
                        true
                    }
                    event.key == towardRailKey && focusedButton > 0 -> {
                        runCatching { buttonRequesters[focusedButton - 1].requestFocus() }
                        true
                    }
                    // First button toward the rail, and Up/Down, use normal traversal.
                    else -> false
                }
            },
    ) {
        Crossfade(
            targetState = currentIndex,
            animationSpec = tween(BackdropFadeMs),
            label = "heroBackdrop",
        ) { slide ->
            val slideMovie = movies[slide]
            CinemaAsyncImage(
                imageUrl = slideMovie.backdropUrl ?: slideMovie.imageUrl,
                contentDescription = slideMovie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallbackLabel = slideMovie.title,
            )
        }

        HeroScrims(isRtl = isRtl)

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.55f)
                .padding(start = shellHeroContentStart(), bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Fixed height, bottom aligned: titles of any length grow upward and the
            // buttons below never shift between slides.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TextBlockHeight),
                contentAlignment = Alignment.BottomStart,
            ) {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        val slideFrom = if (isRtl) -1 else 1
                        (
                            fadeIn(tween(TextFadeInMs, delayMillis = TextFadeOutMs)) +
                                slideInHorizontally(tween(TextFadeInMs, delayMillis = TextFadeOutMs)) { it / 12 * slideFrom }
                            ) togetherWith fadeOut(tween(TextFadeOutMs))
                    },
                    contentAlignment = Alignment.BottomStart,
                    label = "heroText",
                ) { slide ->
                    HeroText(movie = movies[slide])
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                CinemaButton(
                    text = stringResource(R.string.btn_watch_now),
                    variant = CinemaButtonVariant.PrimaryAccent,
                    icon = Icons.Default.PlayArrow,
                    onClick = { onWatchNow(movie) },
                    modifier = Modifier
                        .focusRequester(buttonRequesters[0])
                        .onFocusChanged { if (it.isFocused) focusedButton = 0 },
                )
                CinemaButton(
                    text = stringResource(R.string.btn_details),
                    variant = CinemaButtonVariant.SecondaryDark,
                    icon = Icons.Default.Info,
                    onClick = { onDetails(movie) },
                    modifier = Modifier
                        .focusRequester(buttonRequesters[1])
                        .onFocusChanged { if (it.isFocused) focusedButton = 1 },
                )
                CinemaButton(
                    text = stringResource(R.string.btn_watch_later),
                    variant = CinemaButtonVariant.SecondaryDark,
                    icon = Icons.Default.FavoriteBorder,
                    onClick = { (onAddToList ?: onFavorite)?.invoke(movie) ?: onDetails(movie) },
                    modifier = Modifier
                        .focusRequester(buttonRequesters[2])
                        .onFocusChanged { if (it.isFocused) focusedButton = 2 },
                )
            }

            if (canRotate) {
                HeroSlideIndicators(
                    count = movies.size,
                    selected = currentIndex,
                    progress = { slideProgress.value },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroText(movie: MovieItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.lumora_movies_label),
            style = MaterialTheme.typography.labelLarge.copy(
                color = CinemaColors.Secondary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = movie.title,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.White,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (movie.is4K) {
                BadgeChip(text = stringResource(R.string.badge_4k), backgroundColor = CinemaColors.AccentDeep)
            }
            HeroCarouselLogic.ratingBadge(movie.rating)?.let { rating ->
                BadgeChip(text = rating, backgroundColor = CinemaColors.SurfaceGlass)
            }
            MetadataRow(items = HeroCarouselLogic.metadata(movie))
        }
        if (movie.plot.isNotBlank()) {
            Text(
                text = movie.plot,
                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Bottom and side scrims for text contrast, drawn in one pass with no extra layout nodes. */
@Composable
private fun HeroScrims(isRtl: Boolean) {
    val bottomBrush = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.45f to Color.Transparent,
            0.75f to CinemaColors.Background.copy(alpha = 0.7f),
            1f to CinemaColors.Background,
        )
    }
    val sideBrush = remember(isRtl) {
        val stops = arrayOf(
            0f to CinemaColors.Background.copy(alpha = 0.9f),
            0.35f to CinemaColors.Background.copy(alpha = 0.45f),
            0.65f to Color.Transparent,
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

/**
 * Slide dots; the active one is a pill whose fill tracks time to the next slide.
 * [progress] is only read while drawing, so the fill animates without recomposition.
 */
@Composable
private fun HeroSlideIndicators(count: Int, selected: Int, progress: () -> Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            if (i == selected) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CinemaShapes.Small)
                        .drawBehind {
                            drawRect(CinemaColors.White.copy(alpha = 0.3f))
                            val fillWidth = size.width * progress().coerceIn(0f, 1f)
                            val left = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
                            drawRect(CinemaColors.White, topLeft = Offset(left, 0f), size = Size(fillWidth, size.height))
                        },
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 4.dp)
                        .clip(CinemaShapes.Small)
                        .drawBehind { drawRect(CinemaColors.TextMuted.copy(alpha = 0.5f)) },
                )
            }
        }
    }
}
