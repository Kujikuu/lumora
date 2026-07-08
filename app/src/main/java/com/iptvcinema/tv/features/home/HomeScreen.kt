package com.iptvcinema.tv.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.ContinueWatchingMenuDialog
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ExpandedPosterCardVariant
import com.iptvcinema.tv.core.design.components.FocusAwareContentRail
import com.iptvcinema.tv.core.design.components.HeroCarousel
import com.iptvcinema.tv.core.design.components.Top10Rail
import com.iptvcinema.tv.core.design.components.LocalShellImmersion
import com.iptvcinema.tv.core.design.components.TrackShellVerticalScroll
import com.iptvcinema.tv.core.design.components.isSectionVisible
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.openContinueWatchingDetails
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import kotlinx.coroutines.launch

private object HomeSections {
    const val CONTINUE = "home_continue"
    const val RECOMMENDED = "home_recommended"
    const val RECENTLY_ADDED = "home_recently_added"
    const val TRENDING = "home_trending"
}

private data class HomeSectionIndices(
    val hero: Int,
    val continueWatching: Int,
    val recommended: Int,
    val top10: Int,
    val recentlyAdded: Int,
    val maxIndex: Int,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchNowFocus = remember { FocusRequester() }
    val fallbackContentFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("home")
    val catalogCallbacks = rememberCatalogStateCallbacks(navController)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val shellImmersion = LocalShellImmersion.current
    var continueMenuCard by remember { mutableStateOf<HomeContentCard?>(null) }

    val hasHeroFocusTarget = uiState.heroMovies.isNotEmpty()
    val hasContinueWatching = uiState.continueWatching.isNotEmpty()
    val hasRecommendedSeries = uiState.featuredSeries.isNotEmpty()
    val hasRecentlyAdded = uiState.recentlyAdded.isNotEmpty()
    val hasTop10 = uiState.trending.isNotEmpty()
    val hasAnyRail = hasContinueWatching || hasRecommendedSeries || hasRecentlyAdded || hasTop10
    val hasFallbackFocusTarget = hasAnyRail

    val sectionIndices = remember(
        hasHeroFocusTarget,
        hasContinueWatching,
        hasRecommendedSeries,
        hasTop10,
        hasRecentlyAdded,
    ) {
        var index = 1 // banner
        val hero = if (hasHeroFocusTarget) index++ else -1
        val continueWatching = if (hasContinueWatching) index++ else -1
        val recommended = if (hasRecommendedSeries) index++ else -1
        val top10 = if (hasTop10) index++ else -1
        val recentlyAdded = if (hasRecentlyAdded) index++ else -1
        HomeSectionIndices(
            hero = hero,
            continueWatching = continueWatching,
            recommended = recommended,
            top10 = top10,
            recentlyAdded = recentlyAdded,
            maxIndex = (index - 1).coerceAtLeast(0),
        )
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = focusState.scrollOffset.coerceIn(0, sectionIndices.maxIndex),
    )
    TrackShellVerticalScroll(listState)

    val scrollToSection: suspend (Int) -> Unit = { sectionIndex ->
        if (sectionIndex >= 0 && !listState.isSectionVisible(sectionIndex)) {
            listState.animateScrollToItem(sectionIndex)
        }
    }
    val focusHeroOrFirstRail: () -> Unit = {
        scope.launch {
            shellImmersion?.showNavRail()
            val targetSection = when {
                sectionIndices.hero >= 0 -> sectionIndices.hero
                sectionIndices.continueWatching >= 0 -> sectionIndices.continueWatching
                sectionIndices.recommended >= 0 -> sectionIndices.recommended
                sectionIndices.top10 >= 0 -> sectionIndices.top10
                sectionIndices.recentlyAdded >= 0 -> sectionIndices.recentlyAdded
                else -> 0
            }
            listState.animateScrollToItem(targetSection)
            val target = if (hasHeroFocusTarget) watchNowFocus else fallbackContentFocus
            runCatching { target.requestFocus() }
        }
    }
    val returnToHeroOnUp = if (hasHeroFocusTarget) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                focusHeroOrFirstRail()
                true
            } else {
                false
            }
        }
    } else {
        Modifier
    }

    val firstRailSection = when {
        hasContinueWatching -> HomeSections.CONTINUE
        hasRecommendedSeries -> HomeSections.RECOMMENDED
        hasTop10 -> HomeSections.TRENDING
        hasRecentlyAdded -> HomeSections.RECENTLY_ADDED
        else -> null
    }
    fun railModifierAndFocus(sectionId: String): Pair<Modifier, FocusRequester?> {
        val isFirstRail = sectionId == firstRailSection
        return (if (isFirstRail) returnToHeroOnUp else Modifier) to
            (if (isFirstRail && !hasHeroFocusTarget) fallbackContentFocus else null)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshContinueWatching()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.loadState) {
        if (uiState.loadState != CatalogLoadState.Ready || focusState.initialFocusHandled) return@LaunchedEffect
        val target = if (hasHeroFocusTarget) watchNowFocus else fallbackContentFocus
        if (focusState.hasSavedFocus) {
            listState.scrollToItem(focusState.scrollOffset.coerceIn(0, sectionIndices.maxIndex))
            focusState.restoreFocus(target)
        } else {
            listState.scrollToItem(0)
            focusState.requestInitialFocus(target)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        focusState.saveBrowseFocus(
            sectionId = focusState.sectionId,
            itemIndex = focusState.itemIndex,
            scrollOffset = listState.firstVisibleItemIndex,
            categoryIndex = focusState.focusIndex,
        )
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Home,
        onRailExitRight = focusHeroOrFirstRail,
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
            val heroMovies = uiState.heroMovies
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                item(key = "banner") {
                    CatalogRefreshBanner(
                        syncBannerText = uiState.syncBannerText,
                        refreshState = uiState.refreshState,
                        onRefresh = viewModel::refreshCurrentSource,
                    )
                }
                if (heroMovies.isNotEmpty()) {
                    item(key = "hero") {
                        HeroCarousel(
                            movies = heroMovies,
                            onWatchNow = { movie ->
                                navController.navigate(AppRoute.player(movie.id, "movie"))
                            },
                            onDetails = { movie ->
                                navController.navigate(AppRoute.movieDetails(movie.id))
                            },
                            onAddToList = { movie ->
                                viewModel.addHeroToList(movie)
                            },
                            onFavorite = { movie ->
                                viewModel.toggleHeroFavorite(movie)
                            },
                            watchNowFocusRequester = watchNowFocus,
                        )
                    }
                }

                if (hasContinueWatching) {
                    item(key = HomeSections.CONTINUE) {
                        val (railModifier, railFocus) = railModifierAndFocus(HomeSections.CONTINUE)
                        FocusAwareContentRail(
                            modifier = railModifier,
                            title = stringResource(R.string.home_continue_watching),
                            items = uiState.continueWatching,
                            variant = ExpandedPosterCardVariant.Landscape,
                            sectionId = HomeSections.CONTINUE,
                            firstItemFocusRequester = railFocus,
                            onEnteringRail = { scrollToSection(sectionIndices.continueWatching) },
                            parentHandlesVerticalScroll = true,
                            onWatchNow = { card -> navigateToPlayer(navController, card) },
                            onAddToList = { card -> viewModel.toggleFavorite(card) },
                            onFavorite = { card -> viewModel.toggleFavorite(card) },
                            onCardClick = { card -> navigateToPlayer(navController, card) },
                            onCardLongClick = { card -> continueMenuCard = card },
                        )
                    }
                }

                if (hasRecommendedSeries) {
                    item(key = HomeSections.RECOMMENDED) {
                        val (railModifier, railFocus) = railModifierAndFocus(HomeSections.RECOMMENDED)
                        FocusAwareContentRail(
                            modifier = railModifier,
                            title = stringResource(R.string.home_recommended_series),
                            items = uiState.featuredSeries,
                            variant = ExpandedPosterCardVariant.LandscapePoster,
                            sectionId = HomeSections.RECOMMENDED,
                            firstItemFocusRequester = railFocus,
                            onEnteringRail = { scrollToSection(sectionIndices.recommended) },
                            parentHandlesVerticalScroll = true,
                            onWatchNow = { card -> navigateToPlayer(navController, card) },
                            onAddToList = { card -> viewModel.toggleFavorite(card) },
                            onFavorite = { card -> viewModel.toggleFavorite(card) },
                            onCardClick = { card -> navigateToDetails(navController, card) },
                        )
                    }
                }

                if (hasTop10) {
                    item(key = HomeSections.TRENDING) {
                        val (railModifier, railFocus) = railModifierAndFocus(HomeSections.TRENDING)
                        Top10Rail(
                            modifier = railModifier,
                            title = stringResource(R.string.home_top_10_lumora),
                            items = uiState.trending,
                            firstItemFocusRequester = railFocus,
                            onEnteringRail = { scrollToSection(sectionIndices.top10) },
                            parentHandlesVerticalScroll = true,
                            onCardClick = { card -> navigateToDetails(navController, card) },
                        )
                    }
                }

                if (hasRecentlyAdded) {
                    item(key = HomeSections.RECENTLY_ADDED) {
                        val (railModifier, railFocus) = railModifierAndFocus(HomeSections.RECENTLY_ADDED)
                        FocusAwareContentRail(
                            modifier = railModifier,
                            title = stringResource(R.string.home_recently_added),
                            items = uiState.recentlyAdded,
                            variant = ExpandedPosterCardVariant.LandscapePoster,
                            sectionId = HomeSections.RECENTLY_ADDED,
                            firstItemFocusRequester = railFocus,
                            onEnteringRail = { scrollToSection(sectionIndices.recentlyAdded) },
                            parentHandlesVerticalScroll = true,
                            onWatchNow = { card -> navigateToPlayer(navController, card) },
                            onAddToList = { card -> viewModel.toggleFavorite(card) },
                            onFavorite = { card -> viewModel.toggleFavorite(card) },
                            onCardClick = { card -> navigateToDetails(navController, card) },
                        )
                    }
                }

                if (
                    uiState.loadState == CatalogLoadState.Ready &&
                    heroMovies.isEmpty() &&
                    !hasAnyRail
                ) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.home_empty_title),
                            description = stringResource(R.string.home_empty_desc),
                            primaryAction = stringResource(R.string.btn_manage_sources),
                            secondaryAction = null,
                            onPrimary = catalogCallbacks.onManageSources,
                            onSecondary = null,
                        )
                    }
                }
            }
        }
    }
}

private fun navigateToPlayer(navController: NavController, card: HomeContentCard) {
    when (card.contentType) {
        "movie" -> navController.navigate(AppRoute.player(card.contentId, "movie"))
        "series" -> navController.navigate(AppRoute.seriesDetails(card.contentId))
        "episode" -> navController.navigate(
            AppRoute.player(card.contentId, "episode", card.seriesId),
        )
        else -> navigateToDetails(navController, card)
    }
}

private fun navigateToDetails(navController: NavController, card: HomeContentCard) {
    when (card.contentType) {
        "series" -> navController.navigate(AppRoute.seriesDetails(card.contentId))
        else -> navController.navigate(AppRoute.movieDetails(card.contentId))
    }
}
