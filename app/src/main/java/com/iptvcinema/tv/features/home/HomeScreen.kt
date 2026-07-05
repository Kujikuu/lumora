package com.iptvcinema.tv.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.iptvcinema.tv.core.design.components.ChannelTile
import com.iptvcinema.tv.core.design.components.ContentRail
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ExpandedPosterCardVariant
import com.iptvcinema.tv.core.design.components.FocusAwareContentRail
import com.iptvcinema.tv.core.design.components.HeroCarousel
import com.iptvcinema.tv.core.design.components.MoodCategoryRow
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.MoodBrowseTarget
import com.iptvcinema.tv.core.model.home.MoodCategory
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

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
    var homeResumeToken by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshContinueWatching()
                homeResumeToken++
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
        uiState.trending.isNotEmpty() ||
        uiState.featuredSeries.isNotEmpty() ||
        uiState.liveChannels.isNotEmpty() ||
        uiState.newReleases.isNotEmpty()

    LaunchedEffect(
        uiState.loadState,
        focusState.hasSavedFocus,
        hasHeroFocusTarget,
        hasFallbackFocusTarget,
        homeResumeToken,
    ) {
        if (uiState.loadState != CatalogLoadState.Ready) return@LaunchedEffect
        val target = if (hasHeroFocusTarget) watchNowFocus else fallbackContentFocus
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(target)
        } else {
            if (focusState.requestInitialFocus(target)) {
                focusState.saveFocusIndex(0)
            }
        }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Home,
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
                    .verticalScroll(rememberScrollState()),
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
                            viewModel.toggleHeroFavorite(movie)
                        },
                        onFavorite = { movie ->
                            viewModel.toggleHeroFavorite(movie)
                        },
                        watchNowFocusRequester = watchNowFocus,
                    )
                }

                if (uiState.continueWatching.isNotEmpty()) {
                    FocusAwareContentRail(
                        title = stringResource(R.string.home_continue_watching),
                        items = uiState.continueWatching,
                        variant = ExpandedPosterCardVariant.Landscape,
                        countLabel = uiState.continueWatching.size.toString(),
                        firstItemFocusRequester = if (!hasHeroFocusTarget) fallbackContentFocus else null,
                        onWatchNow = { card -> navigateToPlayer(navController, card) },
                        onAddToList = { card -> viewModel.toggleFavorite(card) },
                        onFavorite = { card -> viewModel.toggleFavorite(card) },
                        onCardClick = { card -> navigateToPlayer(navController, card) },
                    )
                }

                MoodCategoryRow(
                    title = stringResource(R.string.home_mood_row_title),
                    categories = uiState.moodCategories,
                    onCategoryClick = { category -> navigateMoodCategory(navController, category) },
                )

                if (uiState.trending.isNotEmpty()) {
                    FocusAwareContentRail(
                        title = stringResource(R.string.home_trending),
                        items = uiState.trending,
                        countLabel = uiState.trending.size.toString(),
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget && uiState.continueWatching.isEmpty()
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

                if (uiState.featuredSeries.isNotEmpty()) {
                    FocusAwareContentRail(
                        title = stringResource(R.string.home_popular_series),
                        items = uiState.featuredSeries,
                        countLabel = uiState.featuredSeries.size.toString(),
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget &&
                            uiState.continueWatching.isEmpty() &&
                            uiState.trending.isEmpty()
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

                if (uiState.liveChannels.isNotEmpty()) {
                    ContentRail(
                        title = stringResource(R.string.home_live_channels),
                        items = uiState.liveChannels,
                        countLabel = uiState.liveChannels.size.toString(),
                    ) { channel ->
                        ChannelTile(
                            data = channel,
                            modifier = if (
                                !hasHeroFocusTarget &&
                                uiState.continueWatching.isEmpty() &&
                                uiState.trending.isEmpty() &&
                                uiState.featuredSeries.isEmpty() &&
                                channel == uiState.liveChannels.firstOrNull()
                            ) {
                                Modifier.focusRequester(fallbackContentFocus)
                            } else {
                                Modifier
                            },
                            onClick = {
                                channel.id?.let { channelId ->
                                    navController.navigate(AppRoute.player(channelId, "live"))
                                } ?: navController.navigate(AppRoute.liveTv())
                            },
                        )
                    }
                }

                if (uiState.newReleases.isNotEmpty()) {
                    FocusAwareContentRail(
                        title = stringResource(R.string.home_new_releases),
                        items = uiState.newReleases,
                        countLabel = uiState.newReleases.size.toString(),
                        firstItemFocusRequester = if (
                            !hasHeroFocusTarget &&
                            uiState.continueWatching.isEmpty() &&
                            uiState.trending.isEmpty() &&
                            uiState.featuredSeries.isEmpty() &&
                            uiState.liveChannels.isEmpty()
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

                if (
                    uiState.loadState == CatalogLoadState.Ready &&
                    heroMovies.isEmpty() &&
                    uiState.continueWatching.isEmpty() &&
                    uiState.trending.isEmpty() &&
                    uiState.featuredSeries.isEmpty() &&
                    uiState.liveChannels.isEmpty() &&
                    uiState.newReleases.isEmpty()
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
