package com.iptvcinema.tv.features.series

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
import androidx.compose.ui.res.pluralStringResource
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
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
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
    val watchNowFocus = remember { FocusRequester() }
    val continueWatchingFocus = remember { FocusRequester() }
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("series")
    val categories = uiState.categories
    val hasFeatured = uiState.featured != null
    val catalogCallbacks = rememberCatalogStateCallbacks(
        navController = navController,
        onRetry = viewModel::refreshCurrentSource,
    )

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

    LaunchedEffect(
        uiState.loadState,
        focusState.hasSavedFocus,
        hasFeatured,
        uiState.continueSeries.isNotEmpty(),
        categories.isNotEmpty(),
        uiState.posters.isNotEmpty(),
    ) {
        if (uiState.loadState != CatalogLoadState.Ready) return@LaunchedEffect
        val target = when {
            hasFeatured -> watchNowFocus
            uiState.continueSeries.isNotEmpty() -> continueWatchingFocus
            categories.isNotEmpty() -> categoryFocus
            else -> gridFocus
        }
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(target)
        } else {
            focusState.requestInitialFocus(target)
            if (!hasFeatured && uiState.continueSeries.isEmpty()) {
                focusState.saveFocusIndex(selectedFilter)
            }
        }
    }

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
                    uiState.featured?.let { series ->
                        SeriesFeaturedHero(
                            series = series,
                            watchNowFocusRequester = watchNowFocus,
                            onWatchNow = {
                                navController.navigate(AppRoute.seriesDetails(series.id))
                            },
                            onDetails = {
                                navController.navigate(AppRoute.seriesDetails(series.id))
                            },
                        )
                    }
                    if (uiState.continueSeries.isNotEmpty()) {
                        FocusAwareContentRail(
                            title = stringResource(R.string.home_continue_watching),
                            items = uiState.continueSeries,
                            variant = ExpandedPosterCardVariant.Landscape,
                            countLabel = uiState.continueSeries.size.toString(),
                            firstItemFocusRequester = if (!hasFeatured) continueWatchingFocus else null,
                            onWatchNow = { card -> navigateSeriesCardToPlayer(navController, card) },
                            onAddToList = viewModel::toggleFavorite,
                            onFavorite = viewModel::toggleFavorite,
                            onCardClick = { card -> navigateSeriesCardToPlayer(navController, card) },
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
                            chipFocusRequester = if (!hasFeatured && uiState.continueSeries.isEmpty()) {
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
                            uiState.continueSeries.isEmpty() &&
                            categories.isEmpty()
                        ) {
                            gridFocus
                        } else {
                            null
                        },
                        contentPadding = PaddingValues(bottom = CinemaSpacing.SectionGap),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeriesFeaturedHero(
    series: SeriesItem,
    onWatchNow: () -> Unit,
    onDetails: () -> Unit,
    watchNowFocusRequester: FocusRequester,
) {
    HeroBanner(
        title = series.title,
        metadata = listOfNotNull(
            series.year.takeIf { it > 0 }?.toString(),
            series.genres.joinToString(" ").takeIf { it.isNotBlank() },
            series.seasonCount.takeIf { it > 0 }?.let { count ->
                pluralStringResource(R.plurals.details_season_count, count, count)
            },
        ),
        description = series.plot,
        onWatchNow = onWatchNow,
        onDetails = onDetails,
        height = CinemaSpacing.HeroFeaturedMinHeight,
        watchNowFocusRequester = watchNowFocusRequester,
        backdropUrl = series.backdropUrl ?: series.imageUrl,
    )
}

private fun navigateSeriesCardToPlayer(navController: NavController, card: HomeContentCard) {
    navController.navigate(AppRoute.player(card.contentId, card.contentType, card.seriesId))
}
