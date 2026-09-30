package com.iptvcinema.tv.features.catalog

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.catalogContentStart
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import kotlinx.coroutines.launch

/**
 * The full catalog grid behind a browse tab's category tiles: category chips, sort, and every
 * title of the selected category. Back returns to the tab's landing.
 */
@Composable
fun CatalogGridScreen(
    navController: NavController,
    navItem: NavItem,
    focusKey: String,
    title: String,
    initialCategory: String,
    emptyTitle: String,
    emptyDescription: String,
    viewModel: CatalogGridViewModel<*>,
    onPosterClick: (contentId: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    // The grid has no Continue Watching rail or hero; these stay unattached.
    val unusedContinueFocus = remember { FocusRequester() }
    val unusedWatchNowFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState(focusKey)
    val categories = uiState.categories
    val sortOptions = rememberCatalogSortOptions()
    val scope = rememberCoroutineScope()
    var restoreCatalogScroll by remember { mutableStateOf<suspend () -> Unit>({}) }
    val contentStart = catalogContentStart()

    PopBackHandler(onBack = { navController.popBackStack() })

    var selectedFilter by remember(initialCategory, focusState.focusIndex, categories) {
        mutableIntStateOf(
            when {
                focusState.hasSavedFocus && categories.isNotEmpty() ->
                    focusState.focusIndex.coerceIn(0, categories.lastIndex)
                categories.isNotEmpty() ->
                    categories.indexOfFirst { it.equals(initialCategory, ignoreCase = true) }.coerceAtLeast(0)
                else -> 0
            },
        )
    }

    LaunchedEffect(categories.getOrNull(selectedFilter)) {
        // Before the chips load, ask for the requested category directly so the grid does not
        // flash the provider's first category.
        viewModel.selectCategory(categories.getOrNull(selectedFilter) ?: initialCategory.takeIf { it.isNotBlank() })
    }

    LaunchedEffect(uiState.loadState) {
        if (uiState.loadState != CatalogLoadState.Ready || focusState.initialFocusHandled) return@LaunchedEffect
        val target = when {
            focusState.sectionId == CatalogBrowseSections.GRID && focusState.focusedContentId.isNotBlank() -> gridFocus
            categories.isNotEmpty() -> categoryFocus
            else -> gridFocus
        }
        if (focusState.hasSavedFocus) {
            restoreCatalogScroll()
            focusState.restoreFocus(target)
        } else {
            focusState.requestInitialFocus(target)
            focusState.saveFocusIndex(selectedFilter)
        }
    }

    val catalogCallbacks = rememberCatalogStateCallbacks(
        navController = navController,
        onRetry = viewModel::refreshCurrentSource,
    )

    MainShellScaffold(
        navController = navController,
        selectedNavItem = navItem,
        onRailExitRight = {
            scope.launch { runCatching { (if (categories.isNotEmpty()) categoryFocus else gridFocus).requestFocus() } }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = contentStart, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CinemaSerifTitle(text = title)
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
                emptyTitle = emptyTitle,
                emptyDescription = emptyDescription,
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
                    continueWatchingItems = emptyList(),
                    categories = categories,
                    selectedFilter = selectedFilter,
                    onCategorySelected = {
                        selectedFilter = it
                        focusState.saveFocusIndex(it)
                    },
                    sortOptions = sortOptions,
                    selectedSortIndex = catalogSortIndex(uiState.sortOption),
                    onSortSelected = { index -> viewModel.selectSort(catalogSortFromIndex(index)) },
                    posters = uiState.posters,
                    onPosterClick = { poster -> poster.contentId?.let(onPosterClick) },
                    onPosterLongClick = viewModel::togglePosterFavorite,
                    onContinueWatchNow = {},
                    onContinueCardClick = {},
                    onContinueAddToList = {},
                    onContinueFavorite = {},
                    watchNowFocus = unusedWatchNowFocus,
                    continueWatchingFocus = unusedContinueFocus,
                    categoryFocus = categoryFocus,
                    gridFocus = gridFocus,
                    onRestoreScrollReady = { restoreCatalogScroll = it },
                )
            }
        }
    }
}
