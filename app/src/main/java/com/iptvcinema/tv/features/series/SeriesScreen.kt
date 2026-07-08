package com.iptvcinema.tv.features.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.features.catalog.CatalogBrowseContent
import com.iptvcinema.tv.features.catalog.CatalogBrowseSections
import com.iptvcinema.tv.features.catalog.catalogSortFromIndex
import com.iptvcinema.tv.features.catalog.catalogSortIndex
import com.iptvcinema.tv.features.catalog.rememberCatalogSortOptions

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesScreen(
    navController: NavController,
    initialFilter: String = "",
    viewModel: SeriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val continueWatchingFocus = remember { FocusRequester() }
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val watchNowFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("series")
    val categories = uiState.categories
    val sortOptions = rememberCatalogSortOptions()
    val selectedSortIndex = catalogSortIndex(uiState.sortOption)

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

    MainShellBackHandler(navController = navController, isHomeTab = false)

    LaunchedEffect(uiState.loadState) {
        if (uiState.loadState != CatalogLoadState.Ready || focusState.initialFocusHandled) return@LaunchedEffect
        val target = when {
            focusState.sectionId == CatalogBrowseSections.GRID && focusState.focusedContentId.isNotBlank() -> gridFocus
            focusState.sectionId == CatalogBrowseSections.CONTINUE -> continueWatchingFocus
            uiState.continueSeries.isNotEmpty() -> continueWatchingFocus
            categories.isNotEmpty() -> categoryFocus
            else -> gridFocus
        }
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(target)
        } else {
            focusState.requestInitialFocus(target)
            if (uiState.continueSeries.isEmpty()) {
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
        selectedNavItem = NavItem.Series,
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
                CinemaSerifTitle(text = stringResource(R.string.nav_series))
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
                emptyTitle = stringResource(R.string.series_empty_title),
                emptyDescription = stringResource(R.string.catalog_empty_sync_desc),
                onAddSource = catalogCallbacks.onAddSource,
                onRetry = catalogCallbacks.onRetry,
                onManageSources = catalogCallbacks.onManageSources,
                onEditSource = catalogCallbacks.onEditSource,
                onRefreshCatalog = viewModel::refreshCurrentSource,
                modifier = Modifier.weight(1f),
            ) {
                CatalogBrowseContent(
                    focusState = focusState,
                    hasFeatured = false,
                    continueWatchingItems = uiState.continueSeries,
                    categories = categories,
                    selectedFilter = selectedFilter,
                    onCategorySelected = {
                        selectedFilter = it
                        focusState.saveFocusIndex(it)
                    },
                    sortOptions = sortOptions,
                    selectedSortIndex = selectedSortIndex,
                    onSortSelected = { index ->
                        viewModel.selectSort(catalogSortFromIndex(index))
                    },
                    posters = uiState.posters,
                    onPosterClick = { poster ->
                        poster.contentId?.let { seriesId ->
                            navController.navigate(AppRoute.seriesDetails(seriesId))
                        }
                    },
                    onPosterLongClick = { poster ->
                        poster.contentId?.let { seriesId ->
                            viewModel.togglePosterFavorite(seriesId)
                        }
                    },
                    onContinueWatchNow = { card -> navigateSeriesCardToPlayer(navController, card) },
                    onContinueCardClick = { card -> navigateSeriesCardToPlayer(navController, card) },
                    onContinueAddToList = viewModel::toggleFavorite,
                    onContinueFavorite = viewModel::toggleFavorite,
                    watchNowFocus = watchNowFocus,
                    continueWatchingFocus = continueWatchingFocus,
                    categoryFocus = categoryFocus,
                    gridFocus = gridFocus,
                )
            }
        }
    }
}

private fun navigateSeriesCardToPlayer(navController: NavController, card: HomeContentCard) {
    navController.navigate(AppRoute.player(card.contentId, card.contentType, card.seriesId))
}
