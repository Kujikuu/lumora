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
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.ChannelTile
import com.iptvcinema.tv.core.design.components.ContentRail
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.HeroCarousel
import com.iptvcinema.tv.core.design.components.PosterCard
import com.iptvcinema.tv.core.design.components.PosterCardVariant
import com.iptvcinema.tv.core.design.components.SyncStatusBanner
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
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
        uiState.trending.isNotEmpty() ||
        uiState.featuredSeries.isNotEmpty() ||
        uiState.liveChannels.isNotEmpty() ||
        uiState.newReleases.isNotEmpty()

    LaunchedEffect(focusState.hasSavedFocus, hasHeroFocusTarget, hasFallbackFocusTarget) {
        val target = if (hasHeroFocusTarget) watchNowFocus else fallbackContentFocus
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(target)
        } else {
            focusState.requestInitialFocus(target)
            focusState.saveFocusIndex(0)
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
            onTryDemo = catalogCallbacks.onTryDemo,
            onRetry = catalogCallbacks.onRetry,
            onManageSources = catalogCallbacks.onManageSources,
            onEditSource = catalogCallbacks.onEditSource,
        ) {
            val heroMovies = uiState.heroMovies
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            ) {
                val refreshBannerText = when (val refreshState = uiState.refreshState) {
                    CatalogRefreshState.Idle -> uiState.syncBannerText
                    CatalogRefreshState.Refreshing -> stringResource(R.string.refresh_in_progress)
                    is CatalogRefreshState.Success -> refreshState.message
                    is CatalogRefreshState.Failed -> refreshState.message
                }
                refreshBannerText?.let { bannerText ->
                    SyncStatusBanner(
                        text = bannerText,
                        isRefreshing = uiState.refreshState == CatalogRefreshState.Refreshing,
                        onClick = viewModel::refreshCurrentSource,
                    )
                }
                if (heroMovies.isNotEmpty()) {
                    HeroCarousel(
                        movies = heroMovies,
                        onWatchNow = { movie ->
                            navController.navigate(AppRoute.player(movie.id, "movie"))
                        },
                        onDetails = { movie ->
                            navController.navigate(AppRoute.movieDetails(movie.id))
                        },
                        watchNowFocusRequester = watchNowFocus,
                    )
                }

                if (uiState.continueWatching.isNotEmpty()) {
                    ContentRail(
                        title = stringResource(R.string.home_continue_watching),
                        items = uiState.continueWatching.map { it.poster },
                        countLabel = uiState.continueWatching.size.toString(),
                    ) { poster ->
                        val item = uiState.continueWatching.find { it.poster.contentId == poster.contentId }
                        PosterCard(
                            data = poster,
                            variant = PosterCardVariant.LandscapePoster,
                            modifier = if (!hasHeroFocusTarget && poster == uiState.continueWatching.firstOrNull()?.poster) {
                                Modifier.focusRequester(fallbackContentFocus)
                            } else {
                                Modifier
                            },
                            onClick = {
                                item?.let {
                                    navController.navigate(
                                        AppRoute.player(it.contentId, it.contentType, it.seriesId),
                                    )
                                }
                            },
                        )
                    }
                }

                if (uiState.trending.isNotEmpty()) {
                    ContentRail(
                        title = stringResource(R.string.home_trending),
                        items = uiState.trending,
                        countLabel = uiState.trending.size.toString(),
                    ) { poster ->
                        PosterCard(
                            data = poster,
                            modifier = if (!hasHeroFocusTarget &&
                                uiState.continueWatching.isEmpty() &&
                                poster == uiState.trending.firstOrNull()
                            ) {
                                Modifier.focusRequester(fallbackContentFocus)
                            } else {
                                Modifier
                            },
                            onClick = { poster.contentId?.let { navController.navigate(AppRoute.movieDetails(it)) } },
                        )
                    }
                }

                if (uiState.featuredSeries.isNotEmpty()) {
                    ContentRail(
                        title = stringResource(R.string.home_popular_series),
                        items = uiState.featuredSeries,
                        countLabel = uiState.featuredSeries.size.toString(),
                    ) { poster ->
                        PosterCard(
                            data = poster,
                            modifier = if (!hasHeroFocusTarget &&
                                uiState.continueWatching.isEmpty() &&
                                uiState.trending.isEmpty() &&
                                poster == uiState.featuredSeries.firstOrNull()
                            ) {
                                Modifier.focusRequester(fallbackContentFocus)
                            } else {
                                Modifier
                            },
                            onClick = {
                                poster.contentId?.let { navController.navigate(AppRoute.seriesDetails(it)) }
                            },
                        )
                    }
                }

                if (uiState.liveChannels.isNotEmpty()) {
                    ContentRail(
                        title = stringResource(R.string.home_live_channels),
                        items = uiState.liveChannels,
                        countLabel = uiState.liveChannels.size.toString(),
                    ) { channel ->
                        ChannelTile(
                            data = channel,
                            modifier = if (!hasHeroFocusTarget &&
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
                    ContentRail(
                        title = stringResource(R.string.home_new_releases),
                        items = uiState.newReleases,
                        countLabel = uiState.newReleases.size.toString(),
                    ) { poster ->
                        PosterCard(
                            data = poster,
                            modifier = if (!hasHeroFocusTarget &&
                                uiState.continueWatching.isEmpty() &&
                                uiState.trending.isEmpty() &&
                                uiState.featuredSeries.isEmpty() &&
                                uiState.liveChannels.isEmpty() &&
                                poster == uiState.newReleases.firstOrNull()
                            ) {
                                Modifier.focusRequester(fallbackContentFocus)
                            } else {
                                Modifier
                            },
                            onClick = { poster.contentId?.let { navController.navigate(AppRoute.movieDetails(it)) } },
                        )
                    }
                }

                if (uiState.loadState == CatalogLoadState.Ready &&
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
                        secondaryAction = stringResource(R.string.btn_try_demo),
                        onPrimary = catalogCallbacks.onManageSources,
                        onSecondary = catalogCallbacks.onTryDemo,
                    )
                }
            }
        }
    }
}
