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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ExpandedPosterCardVariant
import com.iptvcinema.tv.core.design.components.FocusAwareChannelRail
import com.iptvcinema.tv.core.design.components.FocusAwareContentRail
import com.iptvcinema.tv.core.design.components.HeroCarousel
import com.iptvcinema.tv.core.design.components.MoodCategoryRow
import com.iptvcinema.tv.core.design.components.Top10Rail
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.MoodBrowseTarget
import com.iptvcinema.tv.core.model.home.MoodCategory
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogScrollState
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import kotlinx.coroutines.launch

private object HomeSections {
    const val CONTINUE = "home_continue"
    const val LIVE_NOW = "home_live_now"
    const val POPULAR = "home_popular"
    const val TRENDING = "home_trending"
    const val SERIES = "home_series"
    const val RECENT = "home_recent"
    const val FAVORITES = "home_favorites"
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
    val hasFallbackFocusTarget = uiState.continueWatching.isNotEmpty() ||
        uiState.liveNow.isNotEmpty() ||
        uiState.popularMovies.isNotEmpty() ||
        uiState.featuredSeries.isNotEmpty() ||
        uiState.recentlyAdded.isNotEmpty() ||
        uiState.favorites.isNotEmpty()
    val focusHeroOrFirstRail: () -> Unit = {
        scope.launch {
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
        CatalogStateContent(
            loadState = uiState.loadState,
            message = uiState.message,
            sourceStatus = uiState.sourceStatus,
            sourceType = uiState.sourceType,
            skeletonStyle = CatalogSkeletonStyle.Home,
            emptyTitle = stringResource(R.string.home_empty_title),
            emptyDescription = stringResource(R.string.home_empty_desc),
            onAddSource = catalogCallbacks.onAddSource,
            onRetry = catalogCallbacks.onRetry,
            onManageSources = catalogCallbacks.onManageSources,
            onEditSource = catalogCallbacks.onEditSource,
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

                if (uiState.continueWatching.isNotEmpty()) {
                    FocusAwareContentRail(
                        modifier = returnToHeroOnUp,
                        title = stringResource(R.string.home_continue_watching),
                        items = uiState.continueWatching,
                        variant = ExpandedPosterCardVariant.Landscape,
                        countLabel = uiState.continueWatching.size.toString(),
                        sectionId = HomeSections.CONTINUE,
                        firstItemFocusRequester = if (!hasHeroFocusTarget) fallbackContentFocus else null,
                        onItemFocused = firstRailFocused,
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToPlayer(navController, card) },
                    )
                }

                if (uiState.liveNow.isNotEmpty()) {
                    FocusAwareChannelRail(
                        modifier = if (uiState.continueWatching.isEmpty()) returnToHeroOnUp else Modifier,
                        title = stringResource(R.string.home_live_now),
                        items = uiState.liveNow,
                        countLabel = uiState.liveNow.size.toString(),
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget && uiState.continueWatching.isEmpty()
                        ) {
                            fallbackContentFocus
                        } else {
                            null
                        },
                        onChannelClick = { channel ->
                            channel.id?.let { channelId ->
                                navController.navigate(AppRoute.player(channelId, "live"))
                            } ?: navController.navigate(AppRoute.liveTv())
                        },
                    )
                }

                if (uiState.popularMovies.isNotEmpty()) {
                    FocusAwareContentRail(
                        modifier = if (
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty()
                        ) returnToHeroOnUp else Modifier,
                        title = stringResource(R.string.home_popular_movies),
                        items = uiState.popularMovies,
                        variant = ExpandedPosterCardVariant.Landscape,
                        countLabel = uiState.popularMovies.size.toString(),
                        sectionId = HomeSections.POPULAR,
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget &&
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty()
                        ) {
                            fallbackContentFocus
                        } else {
                            null
                        },
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                if (uiState.trending.isNotEmpty()) {
                    Top10Rail(
                        modifier = if (
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty() &&
                            uiState.popularMovies.isEmpty()
                        ) returnToHeroOnUp else Modifier,
                        title = stringResource(R.string.home_top_10_egypt),
                        items = uiState.trending,
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                if (uiState.featuredSeries.isNotEmpty()) {
                    FocusAwareContentRail(
                        modifier = if (
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty() &&
                            uiState.popularMovies.isEmpty()
                        ) returnToHeroOnUp else Modifier,
                        title = stringResource(R.string.home_popular_series),
                        items = uiState.featuredSeries,
                        variant = ExpandedPosterCardVariant.Landscape,
                        countLabel = uiState.featuredSeries.size.toString(),
                        sectionId = HomeSections.SERIES,
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget &&
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty() &&
                            uiState.popularMovies.isEmpty()
                        ) {
                            fallbackContentFocus
                        } else {
                            null
                        },
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                if (uiState.recentlyAdded.isNotEmpty()) {
                    FocusAwareContentRail(
                        modifier = if (
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty() &&
                            uiState.popularMovies.isEmpty() &&
                            uiState.featuredSeries.isEmpty()
                        ) returnToHeroOnUp else Modifier,
                        title = stringResource(R.string.home_recently_added),
                        items = uiState.recentlyAdded,
                        variant = ExpandedPosterCardVariant.Landscape,
                        countLabel = uiState.recentlyAdded.size.toString(),
                        sectionId = HomeSections.RECENT,
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget &&
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty() &&
                            uiState.popularMovies.isEmpty() &&
                            uiState.featuredSeries.isEmpty()
                        ) {
                            fallbackContentFocus
                        } else {
                            null
                        },
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                if (uiState.favorites.isNotEmpty()) {
                    FocusAwareContentRail(
                        modifier = if (
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty() &&
                            uiState.popularMovies.isEmpty() &&
                            uiState.featuredSeries.isEmpty() &&
                            uiState.recentlyAdded.isEmpty()
                        ) returnToHeroOnUp else Modifier,
                        title = stringResource(R.string.home_favorites),
                        items = uiState.favorites,
                        variant = ExpandedPosterCardVariant.Landscape,
                        countLabel = uiState.favorites.size.toString(),
                        sectionId = HomeSections.FAVORITES,
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget &&
                            uiState.continueWatching.isEmpty() &&
                            uiState.liveNow.isEmpty() &&
                            uiState.popularMovies.isEmpty() &&
                            uiState.featuredSeries.isEmpty() &&
                            uiState.recentlyAdded.isEmpty()
                        ) {
                            fallbackContentFocus
                        } else {
                            null
                        },
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToDetails(navController, card) },
                    )
                }

                MoodCategoryRow(
                    title = stringResource(R.string.home_mood_row_title),
                    categories = uiState.moodCategories,
                    onCategoryClick = { category -> navigateMoodCategory(navController, category) },
                )

                if (
                    uiState.loadState == CatalogLoadState.Ready &&
                    heroMovies.isEmpty() &&
                    uiState.continueWatching.isEmpty() &&
                    uiState.liveNow.isEmpty() &&
                    uiState.popularMovies.isEmpty() &&
                    uiState.featuredSeries.isEmpty() &&
                    uiState.recentlyAdded.isEmpty() &&
                    uiState.favorites.isEmpty()
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

private fun navigateMoodCategory(navController: NavController, category: MoodCategory) {
    val route = when (category.target) {
        MoodBrowseTarget.Movies -> AppRoute.movies(category.filter)
        MoodBrowseTarget.Series -> AppRoute.series(category.filter)
    }
    navController.navigate(route)
}
