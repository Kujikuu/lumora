package com.iptvcinema.tv.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.openContinueWatchingDetails
import com.iptvcinema.tv.core.navigation.rememberCatalogScrollState
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import kotlinx.coroutines.launch

private object HomeSections {
    const val CONTINUE = "home_continue"
    const val RECOMMENDED = "home_recommended"
    const val RECENTLY_ADDED = "home_recently_added"
    const val TRENDING = "home_trending"
}

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
    val scrollState = rememberCatalogScrollState(focusState)
    val catalogCallbacks = rememberCatalogStateCallbacks(navController)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val shellImmersion = LocalShellImmersion.current
    var continueMenuCard by remember { mutableStateOf<HomeContentCard?>(null) }

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

    MainShellBackHandler(navController = navController, isHomeTab = true)

    val hasHeroFocusTarget = uiState.heroMovies.isNotEmpty()
    val hasContinueWatching = uiState.continueWatching.isNotEmpty()
    val hasRecommendedSeries = uiState.featuredSeries.isNotEmpty()
    val hasRecentlyAdded = uiState.recentlyAdded.isNotEmpty()
    val hasTop10 = uiState.trending.isNotEmpty()
    val hasAnyRail = hasContinueWatching || hasRecommendedSeries || hasRecentlyAdded ||
        hasTop10
    val hasFallbackFocusTarget = hasAnyRail

    val focusHeroOrFirstRail: () -> Unit = {
        scope.launch {
            shellImmersion?.showNavRail()
            scrollState.scrollTo(0)
            val target = if (hasHeroFocusTarget) watchNowFocus else fallbackContentFocus
            runCatching { target.requestFocus() }
        }
    }
    val firstRailFocused: (HomeContentCard?) -> Unit = {
        scope.launch {
            scrollState.scrollTo(with(density) { 180.dp.roundToPx() })
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

    val railFocusCounter = remember { mutableIntStateOf(0) }
    railFocusCounter.intValue = 0
    fun consumeRailFocus(): Pair<Modifier, FocusRequester?> {
        val index = railFocusCounter.intValue++
        val upModifier = if (index == 0) returnToHeroOnUp else Modifier
        val focusRequester = if (!hasHeroFocusTarget && index == 0) fallbackContentFocus else null
        return upModifier to focusRequester
    }

    LaunchedEffect(uiState.loadState) {
        if (uiState.loadState != CatalogLoadState.Ready || focusState.initialFocusHandled) return@LaunchedEffect
        val target = if (hasHeroFocusTarget) watchNowFocus else fallbackContentFocus
        if (focusState.hasSavedFocus) {
            scrollState.scrollTo(focusState.scrollOffset)
            focusState.restoreFocus(target)
        } else {
            scrollState.scrollTo(0)
            focusState.requestInitialFocus(target)
        }
    }

    LaunchedEffect(scrollState.value) {
        focusState.saveBrowseFocus(
            sectionId = focusState.sectionId,
            itemIndex = focusState.itemIndex,
            scrollOffset = scrollState.value,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                CatalogRefreshBanner(
                    syncBannerText = uiState.syncBannerText,
                    refreshState = uiState.refreshState,
                    onRefresh = viewModel::refreshCurrentSource,
                )
                if (heroMovies.isNotEmpty()) {
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

                if (hasContinueWatching) {
                    val (railModifier, railFocus) = consumeRailFocus()
                    FocusAwareContentRail(
                        modifier = railModifier,
                        title = stringResource(R.string.home_continue_watching),
                        items = uiState.continueWatching,
                        variant = ExpandedPosterCardVariant.Landscape,
                        sectionId = HomeSections.CONTINUE,
                        firstItemFocusRequester = railFocus,
                        onItemFocused = firstRailFocused,
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToPlayer(navController, card) },
                        onCardLongClick = { card -> continueMenuCard = card },
                    )
                }

                if (hasRecommendedSeries) {
                    val (railModifier, railFocus) = consumeRailFocus()
                    FocusAwareContentRail(
                        modifier = railModifier,
                        title = stringResource(R.string.home_recommended_series),
                        items = uiState.featuredSeries,
                        variant = ExpandedPosterCardVariant.LandscapePoster,
                        sectionId = HomeSections.RECOMMENDED,
                        firstItemFocusRequester = railFocus,
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                if (hasTop10) {
                    val (railModifier, railFocus) = consumeRailFocus()
                    Top10Rail(
                        modifier = railModifier,
                        title = stringResource(R.string.home_top_10_lumora),
                        items = uiState.trending,
                        firstItemFocusRequester = railFocus,
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                if (hasRecentlyAdded) {
                    val (railModifier, railFocus) = consumeRailFocus()
                    FocusAwareContentRail(
                        modifier = railModifier,
                        title = stringResource(R.string.home_recently_added),
                        items = uiState.recentlyAdded,
                        variant = ExpandedPosterCardVariant.LandscapePoster,
                        sectionId = HomeSections.RECENTLY_ADDED,
                        firstItemFocusRequester = railFocus,
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                if (
                    uiState.loadState == CatalogLoadState.Ready &&
                    heroMovies.isEmpty() &&
                    !hasAnyRail
                ) {
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
