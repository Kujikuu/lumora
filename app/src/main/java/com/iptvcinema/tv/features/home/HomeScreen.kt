package com.iptvcinema.tv.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.ContinueWatchingMenuDialog
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.ScreenFocusState
import com.iptvcinema.tv.core.navigation.openContinueWatchingDetails
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.features.home.components.HomeBackdrop
import com.iptvcinema.tv.features.home.components.HomeDimens
import com.iptvcinema.tv.features.home.components.HomeRail
import com.iptvcinema.tv.features.home.components.HomeSpotlightPanel
import com.iptvcinema.tv.features.home.components.glideItemToStart
import com.iptvcinema.tv.features.home.components.rememberHomeSpotlightState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Room below the last rail so it can scroll up to the top of the rails area too. */
private val RailsBottomPadding = 200.dp

/**
 * Home: a fixed spotlight (backdrop, title, hero buttons) over a rails area.
 *
 * Only the rails area scrolls. The focused rail glides to the top of that area and the
 * spotlight follows the focused card. Up and Down between rails are handled here instead of
 * by geometric focus search, which could otherwise jump from a rail to the hero buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sections = uiState.sections
    val hero = uiState.hero
    val focusState = rememberScreenFocusState("home")
    val spotlight = rememberHomeSpotlightState()
    val catalogCallbacks = rememberCatalogStateCallbacks(navController)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val watchNowFocus = remember { FocusRequester() }
    val railRequesters = remember { mutableMapOf<String, FocusRequester>() }
    // Captured once: where focus was when the viewer left Home (for example to open details).
    val restoreTarget = remember { focusState.sectionId to focusState.itemIndex }
    var continueMenuCard by remember { mutableStateOf<HomeContentCard?>(null) }
    var focusPlaced by remember { mutableStateOf(false) }

    // Remembered so rail lambdas that capture it stay equal across recompositions (for
    // example sync banner ticks) and the rails can skip recomposing.
    val currentSections by rememberUpdatedState(sections)
    val moves = remember(hero.isNotEmpty()) {
        HomeFocusMoves(scope, listState, focusState, watchNowFocus, hasHero = hero.isNotEmpty()) { index ->
            currentSections.getOrNull(index)?.let { railRequesters.getOrPut(it.id) { FocusRequester() } }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshContinueWatching()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.loadState, sections.isNotEmpty(), hero.isNotEmpty()) {
        if (focusPlaced || uiState.loadState != CatalogLoadState.Ready) return@LaunchedEffect
        val savedRail = sections.indexOfFirst { it.id == restoreTarget.first }
        focusPlaced = if (focusState.hasSavedFocus && savedRail >= 0) {
            listState.scrollToItem(savedRail)
            moves.railRequester(savedRail)?.let { focusState.restoreFocus(it) } ?: false
        } else {
            val target = if (hero.isNotEmpty()) watchNowFocus else moves.railRequester(0)
            target?.let { focusState.requestInitialFocus(it) } ?: false
        }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Home,
        onRailExitRight = { if (hero.isNotEmpty()) moves.toHero() else moves.toRail(0) },
    ) {
        ContinueWatchingMenuDialog(
            card = continueMenuCard,
            onDismiss = { continueMenuCard = null },
            onViewDetails = { card -> openContinueWatchingDetails(navController, card) },
            onRemove = viewModel::removeContinueWatching,
        )
        CatalogStateContent(
            loadState = uiState.loadState,
            message = uiState.message,
            sourceStatus = uiState.sourceStatus,
            sourceType = uiState.sourceType,
            skeletonStyle = CatalogSkeletonStyle.Home,
            emptyTitle = stringResource(R.string.home_empty_title),
            emptyDescription = stringResource(R.string.catalog_empty_sync_desc),
            onAddSource = catalogCallbacks.onAddSource,
            onRetry = catalogCallbacks.onRetry,
            onManageSources = catalogCallbacks.onManageSources,
            onEditSource = catalogCallbacks.onEditSource,
            onRefreshCatalog = viewModel::refreshCurrentSource,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeBackdrop(state = spotlight, hero = hero)
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(HomeDimens.SPOTLIGHT_WEIGHT)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        HomeSpotlightPanel(
                            state = spotlight,
                            hero = hero,
                            watchNowFocusRequester = watchNowFocus,
                            onWatchNow = { movie -> navController.navigate(AppRoute.player(movie.id, "movie")) },
                            onDetails = { movie -> navController.navigate(AppRoute.movieDetails(movie.id)) },
                            onAddToList = viewModel::addHeroToList,
                            onMoveDown = { moves.toRail(0) },
                        )
                    }
                    // Home places rails and cards itself (see HomeFocusMoves and HomeRail); the
                    // default focus bring-into-view would fight those glides and leave rows
                    // half-aligned, so it is switched off inside the rails area.
                    CompositionLocalProvider(LocalBringIntoViewSpec provides NoBringIntoView) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(HomeDimens.RAILS_WEIGHT)
                                .fillMaxWidth(),
                            userScrollEnabled = false,
                            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                            contentPadding = PaddingValues(bottom = RailsBottomPadding),
                        ) {
                            itemsIndexed(sections, key = { _, section -> section.id }) { index, section ->
                                HomeRail(
                                    section = section,
                                    entryRequester = railRequesters.getOrPut(section.id) { FocusRequester() },
                                    initialFocusIndex = if (section.id == restoreTarget.first) restoreTarget.second else 0,
                                    modifier = Modifier.onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when (event.key) {
                                            Key.DirectionUp -> {
                                                if (index > 0) moves.toRail(index - 1) else if (hero.isNotEmpty()) moves.toHero()
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                if (index < sections.lastIndex) moves.toRail(index + 1)
                                                true
                                            }
                                            else -> false
                                        }
                                    },
                                    onCardFocused = { card, itemIndex ->
                                        spotlight.showCard(card)
                                        focusState.saveBrowseFocus(
                                            sectionId = section.id,
                                            itemIndex = itemIndex,
                                            scrollOffset = index,
                                            focusedContentId = card.contentId,
                                        )
                                        moves.alignRail(index)
                                    },
                                    onCardClick = { card -> openCard(navController, section, card) },
                                    onCardLongClick = { card ->
                                        if (section is HomeSection.ContinueWatching) {
                                            continueMenuCard = card
                                        } else {
                                            viewModel.toggleFavorite(card)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                CatalogRefreshBanner(
                    syncBannerText = uiState.syncBannerText,
                    refreshState = uiState.refreshState,
                    onRefresh = viewModel::refreshCurrentSource,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = CinemaSpacing.ScreenPaddingVertical, end = CinemaSpacing.ScreenPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val NoBringIntoView = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/** Explicit vertical focus moves on Home. Each move glides the rails list and then focuses. */
private class HomeFocusMoves(
    private val scope: CoroutineScope,
    private val listState: androidx.compose.foundation.lazy.LazyListState,
    private val focusState: ScreenFocusState,
    private val watchNowFocus: FocusRequester,
    private val hasHero: Boolean,
    val railRequester: (Int) -> FocusRequester?,
) {
    // Only the latest move may land. On a slow TV a focus request can retry for several frames;
    // without this, an older request could pull focus back after a newer key press.
    private var pendingMove: Job? = null

    fun toRail(index: Int) {
        val requester = railRequester(index) ?: return
        move(index, requester)
    }

    fun toHero() {
        if (hasHero) move(0, watchNowFocus)
    }

    private fun move(railIndex: Int, target: FocusRequester) {
        pendingMove?.cancel()
        pendingMove = scope.launch {
            launch { listState.glideItemToStart(railIndex) }
            focusState.restoreFocus(target)
        }
    }

    /** Keeps the focused rail at the top of the rails area. */
    fun alignRail(index: Int) {
        if (listState.firstVisibleItemIndex == index && listState.firstVisibleItemScrollOffset == 0) return
        scope.launch { listState.glideItemToStart(index) }
    }
}

private fun openCard(navController: NavController, section: HomeSection, card: HomeContentCard) {
    when (section) {
        is HomeSection.ContinueWatching, is HomeSection.NextEpisode -> navigateToPlayer(navController, card)
        is HomeSection.RecentChannels -> navController.navigate(AppRoute.liveTv(channelId = card.contentId))
        is HomeSection.Rail, is HomeSection.TopRated -> navigateToDetails(navController, card)
    }
}

private fun navigateToPlayer(navController: NavController, card: HomeContentCard) {
    when (card.contentType) {
        "movie" -> navController.navigate(AppRoute.player(card.contentId, "movie"))
        "episode" -> navController.navigate(AppRoute.player(card.contentId, "episode", card.seriesId))
        else -> navigateToDetails(navController, card)
    }
}

private fun navigateToDetails(navController: NavController, card: HomeContentCard) {
    when (card.contentType) {
        "series" -> navController.navigate(AppRoute.seriesDetails(card.contentId))
        else -> navController.navigate(AppRoute.movieDetails(card.contentId))
    }
}
