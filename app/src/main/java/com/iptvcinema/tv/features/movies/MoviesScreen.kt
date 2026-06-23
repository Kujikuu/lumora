package com.iptvcinema.tv.features.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.FeaturedStrip
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.PosterGrid
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
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
    val chipFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("movies")
    val categories = uiState.categories

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

    LaunchedEffect(focusState.hasSavedFocus, selectedFilter) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(chipFocus)
        } else {
            focusState.requestInitialFocus(chipFocus)
            focusState.saveFocusIndex(selectedFilter)
        }
    }

    val featured = uiState.featured
    val catalogCallbacks = rememberCatalogStateCallbacks(navController)

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Movies,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            item {
                CinemaSerifTitle(text = stringResource(R.string.nav_movies))
            }
            item {
                CatalogStateContent(
                    loadState = uiState.loadState,
                    message = uiState.message,
                    sourceStatus = uiState.sourceStatus,
                    sourceType = uiState.sourceType,
                    skeletonStyle = CatalogSkeletonStyle.PosterGridWithFeatured,
                    emptyTitle = stringResource(R.string.movies_empty_title),
                    emptyDescription = stringResource(R.string.catalog_empty_sync_desc),
                    onAddSource = catalogCallbacks.onAddSource,
                    onTryDemo = catalogCallbacks.onTryDemo,
                    onRetry = catalogCallbacks.onRetry,
                    onManageSources = catalogCallbacks.onManageSources,
                    onEditSource = catalogCallbacks.onEditSource,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap)) {
                        if (featured != null) {
                            FeaturedStrip(
                                title = featured.title,
                                metadata = listOfNotNull(
                                    featured.year.takeIf { it > 0 }?.toString(),
                                    featured.genres.joinToString(" ").takeIf { it.isNotBlank() },
                                    featured.runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
                                ),
                                onWatchNow = { navController.navigate(AppRoute.player(featured.id, "movie")) },
                                onDetails = { navController.navigate(AppRoute.movieDetails(featured.id)) },
                                backdropUrl = featured.backdropUrl ?: featured.imageUrl,
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
                                chipFocusRequester = chipFocus,
                                focusedChipIndex = selectedFilter,
                            )
                        }
                        PosterGrid(
                            items = uiState.posters,
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
}
