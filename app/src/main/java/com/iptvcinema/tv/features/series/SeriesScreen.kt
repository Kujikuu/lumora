package com.iptvcinema.tv.features.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun SeriesScreen(
    navController: NavController,
    initialFilter: String = "",
    viewModel: SeriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val chipFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("series")
    val categories = uiState.categories
    val catalogCallbacks = rememberCatalogStateCallbacks(navController)
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

    LaunchedEffect(focusState.hasSavedFocus, selectedFilter, categories.isNotEmpty(), uiState.posters.isNotEmpty()) {
        val target = if (categories.isNotEmpty()) chipFocus else gridFocus
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(target)
        } else {
            focusState.requestInitialFocus(target)
            focusState.saveFocusIndex(selectedFilter)
        }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Series,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            CinemaSerifTitle(text = stringResource(R.string.nav_series))
            CatalogStateContent(
                loadState = uiState.loadState,
                message = uiState.message,
                sourceStatus = uiState.sourceStatus,
                sourceType = uiState.sourceType,
                skeletonStyle = CatalogSkeletonStyle.PosterGridWithFeatured,
                emptyTitle = stringResource(R.string.series_empty_title),
                emptyDescription = stringResource(R.string.catalog_empty_sync_desc),
                onAddSource = catalogCallbacks.onAddSource,
                onTryDemo = catalogCallbacks.onTryDemo,
                onRetry = catalogCallbacks.onRetry,
                onManageSources = catalogCallbacks.onManageSources,
                onEditSource = catalogCallbacks.onEditSource,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap)) {
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
                            modifier = Modifier.padding(start = CinemaSpacing.ContentStart),
                        )
                    }
                    PosterGrid(
                        items = uiState.posters,
                        firstItemFocusRequester = if (categories.isEmpty()) gridFocus else null,
                        onItemClick = { poster ->
                            poster.contentId?.let { seriesId ->
                                navController.navigate(AppRoute.seriesDetails(seriesId))
                            }
                        },
                    )
                }
            }
        }
    }
}
