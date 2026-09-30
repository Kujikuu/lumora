package com.iptvcinema.tv.features.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.BadgeChip
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.MetadataRow
import com.iptvcinema.tv.core.design.components.shellHeroContentStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.features.home.HeroCarouselLogic
import com.iptvcinema.tv.features.home.HomeSpotlight
import com.iptvcinema.tv.features.home.HomeSpotlightKind
import com.iptvcinema.tv.core.util.isolateDirection

private const val SLIDE_DURATION_MS = 8_000
private const val TEXT_FADE_IN_MS = 180
private const val TEXT_FADE_OUT_MS = 90
private const val BUTTON_COUNT = 3

/**
 * The top part of Home: text for the hero slide or the focused card, and the hero buttons.
 *
 * The text block has a fixed height and is bottom aligned, so nothing below it moves when the
 * content changes. The buttons stay in the layout when focus is in the rails (only faded out),
 * for the same reason.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeSpotlightPanel(
    state: HomeSpotlightState,
    hero: List<MovieItem>,
    watchNowFocusRequester: FocusRequester,
    onWatchNow: (MovieItem) -> Unit,
    onDetails: (MovieItem) -> Unit,
    onAddToList: (MovieItem) -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spotlight = state.current(hero)
    val heroMovie = hero.getOrNull(state.heroIndex.coerceIn(0, (hero.size - 1).coerceAtLeast(0)))
    val buttonsAlpha by animateFloatAsState(
        targetValue = if (state.isOnHero && heroMovie != null) 1f else 0f,
        animationSpec = tween(TEXT_FADE_IN_MS),
        label = "heroButtonsAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth(0.58f)
            .padding(start = shellHeroContentStart(), bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HomeDimens.SpotlightTextHeight),
            contentAlignment = Alignment.BottomStart,
        ) {
            AnimatedContent(
                targetState = spotlight,
                contentKey = { it?.key },
                transitionSpec = {
                    fadeIn(tween(TEXT_FADE_IN_MS, delayMillis = TEXT_FADE_OUT_MS)) togetherWith
                        fadeOut(tween(TEXT_FADE_OUT_MS))
                },
                contentAlignment = Alignment.BottomStart,
                label = "spotlightText",
            ) { shown ->
                if (shown != null) SpotlightText(shown)
            }
        }
        if (heroMovie != null) {
            HeroButtons(
                state = state,
                hero = hero,
                movie = heroMovie,
                watchNowFocusRequester = watchNowFocusRequester,
                onWatchNow = onWatchNow,
                onDetails = onDetails,
                onAddToList = onAddToList,
                onMoveDown = onMoveDown,
                modifier = Modifier.graphicsLayer { alpha = buttonsAlpha },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SpotlightText(spotlight: HomeSpotlight) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(spotlight.kind.labelRes()),
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (spotlight.kind == HomeSpotlightKind.Channel) CinemaColors.LiveRed else CinemaColors.Secondary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = spotlight.title.isolateDirection(),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.White,
            ),
            // One line keeps room for the plot inside the fixed text block.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (spotlight.is4K) {
                BadgeChip(text = stringResource(R.string.badge_4k), backgroundColor = CinemaColors.AccentDeep)
            }
            spotlight.ratingBadge?.let { BadgeChip(text = it, backgroundColor = CinemaColors.SurfaceGlass) }
            MetadataRow(items = spotlight.metadata)
        }
        spotlight.progress?.let { progress -> SpotlightProgress(progress) }
        spotlight.plot?.let { plot ->
            Text(
                text = plot.isolateDirection(),
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SpotlightProgress(progress: Float) {
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 4.dp)
            .clip(CinemaShapes.Small)
            .drawBehind {
                drawRect(CinemaColors.White.copy(alpha = 0.25f))
                val fillWidth = size.width * progress.coerceIn(0f, 1f)
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
                drawRect(CinemaColors.Accent, topLeft = Offset(left, 0f), size = Size(fillWidth, size.height))
            },
    )
}

/**
 * Hero actions with the carousel rules from the old HeroCarousel: buttons move explicitly so
 * focus never slips diagonally into the rails, Down enters the rails through [onMoveDown],
 * pressing away from the nav rail on the last button shows the next slide, and slides
 * auto-advance only while a button is focused.
 */
@Composable
private fun HeroButtons(
    state: HomeSpotlightState,
    hero: List<MovieItem>,
    movie: MovieItem,
    watchNowFocusRequester: FocusRequester,
    onWatchNow: (MovieItem) -> Unit,
    onDetails: (MovieItem) -> Unit,
    onAddToList: (MovieItem) -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canRotate = hero.size > 1
    val slideProgress = remember { Animatable(0f) }
    var focusedButton by remember { mutableIntStateOf(-1) }
    var interactionTick by remember { mutableIntStateOf(0) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val awayFromRailKey = if (isRtl) Key.DirectionLeft else Key.DirectionRight
    val towardRailKey = if (isRtl) Key.DirectionRight else Key.DirectionLeft
    val requesters = listOf(watchNowFocusRequester, remember { FocusRequester() }, remember { FocusRequester() })

    fun advance() {
        state.heroIndex = HeroCarouselLogic.nextIndex(state.heroIndex, hero.size)
    }

    LaunchedEffect(state.heroIndex, state.heroFocused, interactionTick, canRotate) {
        slideProgress.snapTo(0f)
        if (!canRotate || !state.heroFocused) return@LaunchedEffect
        slideProgress.animateTo(1f, tween(durationMillis = SLIDE_DURATION_MS, easing = LinearEasing))
        advance()
    }

    Column(
        modifier = modifier
            .onFocusChanged { focus ->
                state.heroFocused = focus.hasFocus
                if (focus.hasFocus) state.showHero()
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                interactionTick++
                when {
                    event.key == Key.DirectionDown -> {
                        onMoveDown()
                        true
                    }
                    event.key == awayFromRailKey && focusedButton == BUTTON_COUNT - 1 -> {
                        if (canRotate) advance()
                        true
                    }
                    event.key == awayFromRailKey && focusedButton in 0 until BUTTON_COUNT - 1 -> {
                        runCatching { requesters[focusedButton + 1].requestFocus() }
                        true
                    }
                    event.key == towardRailKey && focusedButton > 0 -> {
                        runCatching { requesters[focusedButton - 1].requestFocus() }
                        true
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            CinemaButton(
                text = stringResource(R.string.btn_watch_now),
                variant = CinemaButtonVariant.PrimaryAccent,
                icon = Icons.Default.PlayArrow,
                onClick = { onWatchNow(movie) },
                modifier = Modifier
                    .focusRequester(requesters[0])
                    .onFocusChanged { if (it.isFocused) focusedButton = 0 },
            )
            CinemaButton(
                text = stringResource(R.string.btn_details),
                variant = CinemaButtonVariant.SecondaryDark,
                icon = Icons.Default.Info,
                onClick = { onDetails(movie) },
                modifier = Modifier
                    .focusRequester(requesters[1])
                    .onFocusChanged { if (it.isFocused) focusedButton = 1 },
            )
            CinemaButton(
                text = stringResource(R.string.btn_watch_later),
                variant = CinemaButtonVariant.SecondaryDark,
                icon = Icons.Default.FavoriteBorder,
                onClick = { onAddToList(movie) },
                modifier = Modifier
                    .focusRequester(requesters[2])
                    .onFocusChanged { if (it.isFocused) focusedButton = 2 },
            )
        }
        if (canRotate) {
            SlideIndicators(count = hero.size, selected = state.heroIndex, progress = { slideProgress.value })
        }
    }
}

/** The active dot is a pill filled by time to the next slide; [progress] is read only while drawing. */
@Composable
private fun SlideIndicators(count: Int, selected: Int, progress: () -> Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val active = i == selected
            Box(
                modifier = Modifier
                    .size(width = if (active) 32.dp else 8.dp, height = 4.dp)
                    .clip(CinemaShapes.Small)
                    .drawBehind {
                        if (!active) {
                            drawRect(CinemaColors.TextMuted.copy(alpha = 0.5f))
                            return@drawBehind
                        }
                        drawRect(CinemaColors.White.copy(alpha = 0.3f))
                        val fillWidth = size.width * progress().coerceIn(0f, 1f)
                        val left = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
                        drawRect(CinemaColors.White, topLeft = Offset(left, 0f), size = Size(fillWidth, size.height))
                    },
            )
        }
    }
}

private fun HomeSpotlightKind.labelRes(): Int = when (this) {
    HomeSpotlightKind.Movie -> R.string.home_kind_movie
    HomeSpotlightKind.Series -> R.string.home_kind_series
    HomeSpotlightKind.Episode -> R.string.home_kind_episode
    HomeSpotlightKind.Channel -> R.string.home_live_label
    HomeSpotlightKind.Category -> R.string.browse_kind_category
}
