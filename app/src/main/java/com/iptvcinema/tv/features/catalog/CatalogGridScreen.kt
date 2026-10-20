package com.iptvcinema.tv.features.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import androidx.navigation.NavController
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.BackPill
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

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
    // Opened from a category tile: the page is that one category, with no chip row to wander
    // into. Opened without one, the viewer picks a category from the chips.
    val singleCategory = initialCategory.isNotBlank()
    val sortOptions = rememberCatalogSortOptions()
    var restoreCatalogScroll by remember { mutableStateOf<suspend () -> Unit>({}) }
    val contentStart = 40.dp

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

    // A single-category page always shows the tile's category, by name, so it can never fall
    // back to the provider's first category when the names differ in case or spacing.
    val requestedCategory = if (singleCategory) initialCategory else categories.getOrNull(selectedFilter)
    LaunchedEffect(requestedCategory) {
        viewModel.selectCategory(requestedCategory)
    }

    // Per visit, not saved: coming back from details must place focus again (on the poster
    // the viewer opened), which a saved "handled" flag would skip.
    var focusPlaced by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loadState) {
        if (uiState.loadState != CatalogLoadState.Ready || focusPlaced) return@LaunchedEffect
        focusPlaced = true
        val target = when {
            focusState.sectionId == CatalogBrowseSections.GRID && focusState.focusedContentId.isNotBlank() -> gridFocus
            !singleCategory && categories.isNotEmpty() -> categoryFocus
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

    // A focused, full-width page like the details screens: no side rail, so Left from the first
    // column cannot fall into the menu, and Back returns to the tab's landing.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = contentStart, end = CatalogEndPadding, top = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackPill(text = stringResource(navItem.labelRes))
                CatalogGridHeader(
                    // The category being browsed names the page; the tab name sits in the Back pill.
                    title = if (singleCategory) initialCategory else categories.getOrNull(selectedFilter) ?: title,
                    count = uiState.posters.size.takeIf { uiState.loadState == CatalogLoadState.Ready && it > 0 },
                    modifier = Modifier.weight(1f),
                )
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
                    categories = if (singleCategory) emptyList() else categories,
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
                    contentStart = contentStart,
                    columns = CatalogGridColumns,
                )
            }
        }
    }
}

/** Without the rail the grid uses the full width, so it fits more, smaller posters per row. */
private const val CatalogGridColumns = 7
private val CatalogEndPadding = 40.dp

/** Screen title with the number of titles beside it, both on one baseline. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CatalogGridHeader(
    title: String,
    count: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.alignByBaseline(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.White,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (count != null) {
            Text(
                text = pluralStringResource(R.plurals.browse_title_count, count, count),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                maxLines = 1,
            )
        }
    }
}
