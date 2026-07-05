package com.iptvcinema.tv.features.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.ExpandedPosterCardVariant
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.FocusAwareContentRail
import com.iptvcinema.tv.core.design.components.HeroBanner
import com.iptvcinema.tv.core.design.components.PosterGrid
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MoviesScreen(
    navController: NavController,
    initialFilter: String = "",
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchNowFocus = remember { FocusRequester() }
    val continueWatchingFocus = remember { FocusRequester() }
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("movies")
    val categories = uiState.categories
    val hasFeatured = uiState.featured != null

    MainShellBackHandler(navController = navController, isHomeTab = false)

    var selectedFilter by remember(initialFilter, focusState.focusIndex, categories) {
        mutableIntStateOf(
            when {
                focusState.hasSavedFocus && categories.isNotEmpty() ->
                    focusState.focusIndex.coerceIn(0, categories.lastIndex)
                categories.isNotEmpty() -> {
                    val index = categories.indexOfFirst { it.equals(initialFilter, ignoreCase = true) }
                    if (index >= 0) index else 0
                }
                else -> 0
            },
        )
    }

    LaunchedEffect(categories.getOrNull(selectedFilter)) {
        viewModel.selectCategory(categories.getOrNull(selectedFilter))
    }

    LaunchedEffect(
        uiState.loadState,
        focusState.hasSavedFocus,
        hasFeatured,
        uiState.continueWatchingMovies.isNotEmpty(),
        categories.isNotEmpty(),
        uiState.posters.isNotEmpty(),
    ) {
        if (uiState.loadState != CatalogLoadState.Ready) return@LaunchedEffect
        val target = when {
            hasFeatured -> watchNowFocus
            uiState.continueWatchingMovies.isNotEmpty() -> continueWatchingFocus
            categories.isNotEmpty() -> categoryFocus
            else -> gridFocus
        }
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(target)
        } else {
            focusState.requestInitialFocus(target)
            if (!hasFeatured && uiState.continueWatchingMovies.isEmpty()) {
                focusState.saveFocusIndex(selectedFilter)
            }
        }
    }

    val catalogCallbacks = rememberCatalogStateCallbacks(
        navController = navController,
        onRetry = viewModel::refreshCurrentSource,
    )

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Movies,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = CinemaSpacing.ContentStart, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CinemaSerifTitle(text = stringResource(R.string.nav_movies))
                CatalogRefreshBanner(
                    syncBannerText = uiState.syncBannerText,
                    refreshState = uiState.refreshState,
                    onRefresh = viewModel::refreshCurrentSource,
                )
            }
            CatalogStateContent(
                loadState = uiState.loadState,
                message = uiState.message,
                sourceStatus = uiState.sourceStatus,
                sourceType = uiState.sourceType,
                skeletonStyle = CatalogSkeletonStyle.PosterGrid,
                emptyTitle = stringResource(R.string.movies_empty_title),
                emptyDescription = stringResource(R.string.catalog_empty_sync_desc),
                onAddSource = catalogCallbacks.onAddSource,
                onTryDemo = catalogCallbacks.onTryDemo,
                onRetry = catalogCallbacks.onRetry,
                onManageSources = catalogCallbacks.onManageSources,
                onEditSource = catalogCallbacks.onEditSource,
                onRefreshCatalog = viewModel::refreshCurrentSource,
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    uiState.featured?.let { movie ->
                        MoviesFeaturedHero(
                            movie = movie,
                            watchNowFocusRequester = watchNowFocus,
                            onWatchNow = {
                                navController.navigate(AppRoute.player(movie.id, "movie"))
                            },
                            onDetails = {
                                navController.navigate(AppRoute.movieDetails(movie.id))
                            },
                        )
                    }
                    if (uiState.continueWatchingMovies.isNotEmpty()) {
                        FocusAwareContentRail(
                            title = stringResource(R.string.home_continue_watching),
                            items = uiState.continueWatchingMovies,
                            variant = ExpandedPosterCardVariant.Landscape,
                            countLabel = uiState.continueWatchingMovies.size.toString(),
                            firstItemFocusRequester = if (!hasFeatured) continueWatchingFocus else null,
                            onWatchNow = { card -> navigateMoviesCardToPlayer(navController, card) },
                            onAddToList = viewModel::toggleFavorite,
                            onFavorite = viewModel::toggleFavorite,
                            onCardClick = { card -> navigateMoviesCardToPlayer(navController, card) },
                        )
                    }
                    if (categories.isNotEmpty()) {
                        FilterChipRow(
                            items = categories,
                            selectedIndex = selectedFilter.coerceIn(0, categories.lastIndex),
                            onSelected = {
                                selectedFilter = it
                                focusState.saveFocusIndex(it)
                            },
                            chipFocusRequester = if (!hasFeatured && uiState.continueWatchingMovies.isEmpty()) {
                                categoryFocus
                            } else {
                                null
                            },
                            focusedChipIndex = selectedFilter,
                            modifier = Modifier.padding(start = CinemaSpacing.ContentStart),
                        )
                    }
                    PosterGrid(
                        items = uiState.posters,
                        enableVerticalScroll = false,
                        firstItemFocusRequester = if (
                            !hasFeatured &&
                            uiState.continueWatchingMovies.isEmpty() &&
                            categories.isEmpty()
                        ) {
                            gridFocus
                        } else {
                            null
                        },
                        contentPadding = PaddingValues(bottom = CinemaSpacing.SectionGap),
                        onItemClick = { poster ->
                            poster.contentId?.let { movieId ->
                                navController.navigate(AppRoute.movieDetails(movieId))
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MoviesFeaturedHero(
    movie: MovieItem,
    onWatchNow: () -> Unit,
    onDetails: () -> Unit,
    watchNowFocusRequester: FocusRequester,
) {
    HeroBanner(
        title = movie.title,
        metadata = listOfNotNull(
            movie.year.takeIf { it > 0 }?.toString(),
            movie.genres.joinToString(" ").takeIf { it.isNotBlank() },
            movie.runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
        ),
        description = movie.plot,
        onWatchNow = onWatchNow,
        onDetails = onDetails,
        height = CinemaSpacing.HeroFeaturedMinHeight,
        watchNowFocusRequester = watchNowFocusRequester,
        backdropUrl = movie.backdropUrl ?: movie.imageUrl,
    )
}

private fun navigateMoviesCardToPlayer(navController: NavController, card: HomeContentCard) {
    navController.navigate(AppRoute.player(card.contentId, card.contentType, card.seriesId))
}
