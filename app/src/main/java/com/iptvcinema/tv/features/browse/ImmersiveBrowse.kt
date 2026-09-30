package com.iptvcinema.tv.features.browse

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.ScreenFocusState
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.components.HomeBackdrop
import com.iptvcinema.tv.features.home.components.HomeDimens
import com.iptvcinema.tv.features.home.components.HomeRail
import com.iptvcinema.tv.features.home.components.HomeSpotlightPanel
import com.iptvcinema.tv.features.home.components.HomeSpotlightState
import com.iptvcinema.tv.features.home.components.glideItemToStart
import com.iptvcinema.tv.features.home.components.rememberHomeSpotlightState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Room below the last rail so it can scroll up to the top of the rails area too. */
private val RailsBottomPadding = 200.dp
private const val TOP_PANEL_FADE_MS = 180
private const val RECOVERY_SETTLE_FRAMES = 3

/** What the hero buttons do. Only Home has a hero; other screens pass none. */
class ImmersiveHeroActions(
    val onWatchNow: (MovieItem) -> Unit,
    val onDetails: (MovieItem) -> Unit,
    val onAddToList: (MovieItem) -> Unit,
)

/**
 * Focus and scroll state of an [ImmersiveBrowse] screen. Held by the screen so it can move focus
 * in from the nav rail ([enter]) and so tests of focus rules stay out of the layout.
 */
@Stable
class ImmersiveBrowseState internal constructor(
    val spotlight: HomeSpotlightState,
    internal val listState: LazyListState,
    internal val focusState: ScreenFocusState,
    private val scope: CoroutineScope,
) {
    internal val watchNowFocus = FocusRequester()
    private val railRequesters = mutableMapOf<String, FocusRequester>()
    internal var sections: List<HomeSection> = emptyList()
    internal var hasHero: Boolean = false
    internal var upFromFirstRail: (() -> Unit)? = null

    /** Whether focus is on a rail card (as opposed to the hero, a top panel or elsewhere). */
    var railsHaveFocus by mutableStateOf(false)
        internal set

    // Only the latest move may land. On a slow TV a focus request can retry for several frames;
    // without this, an older request could pull focus back after a newer key press.
    private var pendingMove: Job? = null

    internal fun requesterFor(sectionId: String): FocusRequester =
        railRequesters.getOrPut(sectionId) { FocusRequester() }

    internal fun railRequester(index: Int): FocusRequester? =
        sections.getOrNull(index)?.let { requesterFor(it.id) }

    /** Moves focus in from the nav rail: to the hero when there is one, else to the first rail. */
    fun enter() {
        if (hasHero) toHero() else toRail(0)
    }

    internal fun toRail(index: Int) {
        val requester = railRequester(index) ?: return
        move(index, requester)
    }

    internal fun toHero() {
        if (hasHero) move(0, watchNowFocus)
    }

    /** Up from the first rail: to the hero, or to whatever the screen puts above the rails. */
    internal fun upFromRails() {
        upFromFirstRail?.let { move -> move() } ?: toHero()
    }

    private fun move(railIndex: Int, target: FocusRequester) {
        pendingMove?.cancel()
        pendingMove = scope.launch {
            launch { listState.glideItemToStart(railIndex) }
            focusState.restoreFocus(target)
        }
    }

    /**
     * Puts focus back on the rails after the focused card or rail went away (removed from a
     * list, or rebuilt by a sync): on the same rail, or the nearest one that is left. Waits a few
     * frames so a closing dialog and the new rails have settled first.
     */
    fun recoverFocus() {
        pendingMove?.cancel()
        pendingMove = scope.launch {
            repeat(RECOVERY_SETTLE_FRAMES) { withFrameNanos { } }
            if (railsHaveFocus || sections.isEmpty()) return@launch
            val saved = sections.indexOfFirst { it.id == focusState.sectionId }
            val index = if (saved >= 0) saved else focusState.scrollOffset.coerceIn(0, sections.lastIndex)
            val requester = railRequester(index) ?: return@launch
            launch { listState.glideItemToStart(index) }
            focusState.restoreFocus(requester)
        }
    }

    /** Keeps the focused rail at the top of the rails area. */
    internal fun alignRail(index: Int) {
        if (listState.firstVisibleItemIndex == index && listState.firstVisibleItemScrollOffset == 0) return
        scope.launch { listState.glideItemToStart(index) }
    }
}

@Composable
fun rememberImmersiveBrowseState(focusState: ScreenFocusState): ImmersiveBrowseState {
    val spotlight = rememberHomeSpotlightState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    return remember(focusState, spotlight, listState, scope) {
        ImmersiveBrowseState(spotlight, listState, focusState, scope)
    }
}

/**
 * The Home layout, shared by every browse screen: a fixed spotlight (backdrop, title, optional
 * hero buttons) over a rails area.
 *
 * Only the rails area scrolls. The focused rail glides to the top of that area and the spotlight
 * follows the focused card. Up and Down between rails are handled here instead of by geometric
 * focus search, which could otherwise jump from a rail to the hero buttons or the screen header.
 *
 * Focus is placed once, when [isReady] first becomes true: back on the rail and card the viewer
 * left (for example to open details), else on [initialFocus] (a section id and card index), else
 * on the hero or the first rail.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImmersiveBrowse(
    state: ImmersiveBrowseState,
    sections: List<HomeSection>,
    isReady: Boolean,
    onCardClick: (HomeSection, HomeContentCard) -> Unit,
    modifier: Modifier = Modifier,
    onCardLongClick: ((HomeSection, HomeContentCard) -> Unit)? = null,
    hero: List<MovieItem> = emptyList(),
    heroActions: ImmersiveHeroActions? = null,
    spotlightWeight: Float = HomeDimens.SPOTLIGHT_WEIGHT,
    initialFocus: Pair<String, Int>? = null,
    onUpFromFirstRail: (() -> Unit)? = null,
    topContent: (@Composable BoxScope.() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val focusState = state.focusState
    val hasHero = hero.isNotEmpty() && heroActions != null
    val railsChangedUnderFocus = state.railsHaveFocus && state.sections.isNotEmpty() && sections != state.sections
    state.sections = sections
    state.hasHero = hasHero
    state.upFromFirstRail = onUpFromFirstRail
    // With a top panel (for example Search's keyboard), the panel shows while focus is above the
    // rails and the spotlight takes its place once focus is on a card; both keep their layout.
    val spotlightAlpha by animateFloatAsState(
        targetValue = if (topContent == null || state.railsHaveFocus) 1f else 0f,
        animationSpec = tween(TOP_PANEL_FADE_MS),
        label = "spotlightAlpha",
    )
    // Captured once: where focus was when the viewer left the screen, or where it should start.
    val restoreTarget = remember {
        if (focusState.hasSavedFocus || initialFocus == null) {
            focusState.sectionId to focusState.itemIndex
        } else {
            initialFocus
        }
    }
    var focusPlaced by remember { mutableStateOf(false) }
    // Read through state so rail lambdas stay equal across recompositions and rails can skip.
    val currentSections by rememberUpdatedState(sections)

    // New rails while a card had focus: that card may be gone, taking focus with it.
    LaunchedEffect(sections) {
        if (focusPlaced && railsChangedUnderFocus) state.recoverFocus()
    }

    LaunchedEffect(isReady, sections.isNotEmpty(), hasHero) {
        if (focusPlaced || !isReady) return@LaunchedEffect
        val savedRail = sections.indexOfFirst { it.id == restoreTarget.first }
        focusPlaced = if ((focusState.hasSavedFocus || initialFocus != null) && savedRail >= 0) {
            state.listState.scrollToItem(savedRail)
            state.railRequester(savedRail)?.let { focusState.restoreFocus(it) } ?: false
        } else {
            val target = if (hasHero) state.watchNowFocus else state.railRequester(0)
            target?.let { focusState.requestInitialFocus(it) } ?: false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeBackdrop(state = state.spotlight, hero = hero)
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(spotlightWeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomStart,
            ) {
                HomeSpotlightPanel(
                    state = state.spotlight,
                    hero = if (hasHero) hero else emptyList(),
                    watchNowFocusRequester = state.watchNowFocus,
                    onWatchNow = { heroActions?.onWatchNow?.invoke(it) },
                    onDetails = { heroActions?.onDetails?.invoke(it) },
                    onAddToList = { heroActions?.onAddToList?.invoke(it) },
                    onMoveDown = { state.toRail(0) },
                    modifier = if (topContent == null) Modifier else Modifier.graphicsLayer { alpha = spotlightAlpha },
                )
                if (topContent != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = 1f - spotlightAlpha },
                        content = topContent,
                    )
                }
            }
            // Rails and cards are placed by ImmersiveBrowseState and HomeRail; the default focus
            // bring-into-view would fight those glides and leave rows half-aligned, so it is
            // switched off inside the rails area.
            CompositionLocalProvider(LocalBringIntoViewSpec provides NoBringIntoView) {
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .weight(1f - spotlightWeight)
                        .fillMaxWidth()
                        .onFocusChanged { state.railsHaveFocus = it.hasFocus },
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                    contentPadding = PaddingValues(bottom = RailsBottomPadding),
                ) {
                    itemsIndexed(sections, key = { _, section -> section.id }) { index, section ->
                        HomeRail(
                            section = section,
                            entryRequester = state.requesterFor(section.id),
                            initialFocusIndex = if (section.id == restoreTarget.first) restoreTarget.second else 0,
                            modifier = Modifier.onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.DirectionUp -> {
                                        if (index > 0) state.toRail(index - 1) else state.upFromRails()
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        if (index < currentSections.lastIndex) state.toRail(index + 1)
                                        true
                                    }
                                    else -> false
                                }
                            },
                            onCardFocused = { card, itemIndex ->
                                state.spotlight.showCard(card)
                                focusState.saveBrowseFocus(
                                    sectionId = section.id,
                                    itemIndex = itemIndex,
                                    scrollOffset = index,
                                    focusedContentId = card.contentId,
                                )
                                state.alignRail(index)
                            },
                            onCardClick = { card -> onCardClick(section, card) },
                            onCardLongClick = onCardLongClick?.let { callback -> { card -> callback(section, card) } },
                        )
                    }
                }
            }
        }
        overlay()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val NoBringIntoView = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}
