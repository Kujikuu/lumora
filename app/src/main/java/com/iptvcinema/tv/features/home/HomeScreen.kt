package com.iptvcinema.tv.features.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
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
import com.iptvcinema.tv.core.navigation.openContinueWatchingDetails
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.features.browse.ImmersiveBrowse
import com.iptvcinema.tv.features.browse.ImmersiveHeroActions
import com.iptvcinema.tv.features.browse.rememberImmersiveBrowseState

/**
 * Home: the shared immersive layout ([ImmersiveBrowse]) with a hero carousel of the newest movies
 * above rails built from real signals (see [HomeSectionsBuilder]).
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusState = rememberScreenFocusState("home")
    val browse = rememberImmersiveBrowseState(focusState)
    val catalogCallbacks = rememberCatalogStateCallbacks(navController)
    val lifecycleOwner = LocalLifecycleOwner.current
    var continueMenuCard by remember { mutableStateOf<HomeContentCard?>(null) }
    val heroActions = remember(navController, viewModel) {
        ImmersiveHeroActions(
            onWatchNow = { movie -> navController.navigate(AppRoute.player(movie.id, "movie")) },
            onDetails = { movie -> navController.navigate(AppRoute.movieDetails(movie.id)) },
            onAddToList = viewModel::addHeroToList,
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshContinueWatching()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Home,
        onRailExitRight = browse::enter,
    ) {
        ContinueWatchingMenuDialog(
            card = continueMenuCard,
            onDismiss = { continueMenuCard = null },
            onResume = { card -> navigateToPlayer(navController, card) },
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
            ImmersiveBrowse(
                state = browse,
                sections = uiState.sections,
                isReady = uiState.loadState == CatalogLoadState.Ready,
                hero = uiState.hero,
                heroActions = heroActions,
                onCardClick = { section, card -> openCard(navController, section, card) },
                onCardLongClick = { section, card ->
                    if (section is HomeSection.ContinueWatching) {
                        continueMenuCard = card
                    } else {
                        viewModel.toggleFavorite(card)
                    }
                },
                overlay = {
                    CatalogRefreshBanner(
                        syncBannerText = uiState.syncBannerText,
                        refreshState = uiState.refreshState,
                        onRefresh = viewModel::refreshCurrentSource,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = CinemaSpacing.ScreenPaddingVertical, end = CinemaSpacing.ScreenPadding),
                    )
                },
            )
        }
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
