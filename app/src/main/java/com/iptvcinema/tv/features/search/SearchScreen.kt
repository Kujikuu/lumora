package com.iptvcinema.tv.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.ChannelTile
import com.iptvcinema.tv.core.design.components.ContentRail
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.PosterCard
import com.iptvcinema.tv.core.design.components.RecentSearchChip
import com.iptvcinema.tv.core.design.components.SearchInput
import com.iptvcinema.tv.core.design.components.SearchKeyboard
import com.iptvcinema.tv.core.design.components.SearchKeyboardLayout
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchFocus = remember { FocusRequester() }
    val chipFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("search")
    val catalogCallbacks = rememberCatalogStateCallbacks(navController, onRetry = viewModel::retry)
    var keyboardLayout by remember { mutableStateOf(SearchKeyboardLayout.English) }
    val searchFilters = listOf(
        stringResource(R.string.filter_all),
        stringResource(R.string.filter_movies),
        stringResource(R.string.filter_series),
        stringResource(R.string.filter_live_tv),
    )

    MainShellBackHandler(navController = navController, isHomeTab = false)

    LaunchedEffect(focusState.hasSavedFocus, uiState.selectedFilterIndex) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(chipFocus)
        } else {
            focusState.requestInitialFocus(searchFocus)
            focusState.saveFocusIndex(uiState.selectedFilterIndex)
        }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Search,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = CinemaSpacing.ContentStart,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CinemaColors.White,
                ),
            )
            SearchInput(
                query = uiState.query,
                onQueryChange = viewModel::updateQuery,
                onSearch = viewModel::retry,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocus),
            )
            SearchKeyboard(
                layout = keyboardLayout,
                onLayoutToggle = {
                    keyboardLayout = when (keyboardLayout) {
                        SearchKeyboardLayout.English -> SearchKeyboardLayout.Arabic
                        SearchKeyboardLayout.Arabic -> SearchKeyboardLayout.English
                    }
                },
                onDeviceKeyboard = {},
                showDeviceKeyboard = false,
                onKeyPress = { key -> viewModel.updateQuery(uiState.query + key) },
                onBackspace = {
                    if (uiState.query.isNotEmpty()) {
                        viewModel.updateQuery(uiState.query.dropLast(1))
                    }
                },
                onClear = viewModel::clearSearch,
            )
            FilterChipRow(
                items = searchFilters,
                selectedIndex = uiState.selectedFilterIndex,
                onSelected = {
                    viewModel.selectFilter(it)
                    focusState.saveFocusIndex(it)
                },
                chipFocusRequester = chipFocus,
                focusedChipIndex = uiState.selectedFilterIndex,
            )

            if (uiState.recentSearches.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.search_recent),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = CinemaColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.recentSearches.forEach { term ->
                        RecentSearchChip(query = term, onClick = { viewModel.applyRecentSearch(term) })
                    }
                }
            }

            CatalogStateContent(
                loadState = uiState.loadState,
                message = uiState.message,
                sourceStatus = uiState.sourceStatus,
                sourceType = uiState.sourceType,
                skeletonStyle = CatalogSkeletonStyle.PosterGrid,
                emptyTitle = stringResource(R.string.search_no_results),
                emptyDescription = stringResource(R.string.search_no_results_desc),
                onAddSource = catalogCallbacks.onAddSource,
                onRetry = catalogCallbacks.onRetry,
                onManageSources = catalogCallbacks.onManageSources,
                onEditSource = catalogCallbacks.onEditSource,
            ) {
                when {
                    uiState.query.trim().length >= 2 && !uiState.hasResults -> {
                        EmptyState(
                            title = stringResource(R.string.search_no_results),
                            description = stringResource(R.string.search_no_results_desc),
                            primaryAction = stringResource(R.string.btn_clear),
                            secondaryAction = null,
                            onPrimary = viewModel::clearSearch,
                            onSecondary = null,
                        )
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            if (uiState.movieResults.isNotEmpty()) {
                                ContentRail(
                                    title = stringResource(R.string.search_top_results),
                                    items = uiState.movieResults,
                                ) { movie ->
                                    PosterCard(
                                        data = movie,
                                        onClick = {
                                            movie.contentId?.let {
                                                navController.navigate(AppRoute.movieDetails(it))
                                            }
                                        },
                                    )
                                }
                            }
                            if (uiState.seriesResults.isNotEmpty()) {
                                ContentRail(
                                    title = stringResource(R.string.search_series_results),
                                    items = uiState.seriesResults,
                                ) { series ->
                                    PosterCard(
                                        data = series,
                                        onClick = {
                                            series.contentId?.let {
                                                navController.navigate(AppRoute.seriesDetails(it))
                                            }
                                        },
                                    )
                                }
                            }
                            if (uiState.channelResults.isNotEmpty()) {
                                ContentRail(
                                    title = stringResource(R.string.search_live_channels),
                                    items = uiState.channelResults,
                                ) { channel ->
                                    ChannelTile(
                                        data = channel,
                                        onClick = {
                                            channel.id?.let { channelId ->
                                                navController.navigate(AppRoute.player(channelId, "live"))
                                            } ?: navController.navigate(AppRoute.liveTv())
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
