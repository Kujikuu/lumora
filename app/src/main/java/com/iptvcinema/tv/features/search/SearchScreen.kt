package com.iptvcinema.tv.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.platform.rememberHasMultipleInputLanguages
import com.iptvcinema.tv.core.platform.showDeviceKeyboard

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
    val hasMultipleInputLanguages = rememberHasMultipleInputLanguages()
    val keyboardController = LocalSoftwareKeyboardController.current
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = CinemaSpacing.NavRailWidth + 16.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 8.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.38f),
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
                    modifier = Modifier.focusRequester(searchFocus),
                )
                SearchKeyboard(
                    layout = uiState.keyboardLayout,
                    onLayoutToggle = viewModel::toggleKeyboardLayout,
                    onDeviceKeyboard = {
                        showDeviceKeyboard(searchFocus, keyboardController)
                    },
                    showDeviceKeyboard = hasMultipleInputLanguages,
                    onKeyPress = viewModel::appendToQuery,
                    onBackspace = viewModel::backspaceQuery,
                    onClear = viewModel::clearSearch,
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.62f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                    onTryDemo = catalogCallbacks.onTryDemo,
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
}
