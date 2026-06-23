package com.iptvcinema.tv.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
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
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                CinemaSerifTitle(text = stringResource(R.string.search_title))
                Text(
                    text = stringResource(R.string.search_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                Text(text = stringResource(R.string.search_recent), style = MaterialTheme.typography.titleMedium)
                if (uiState.recentSearches.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_recent_empty),
                        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                        uiState.recentSearches.forEach { term ->
                            RecentSearchChip(query = term, onClick = { viewModel.applyRecentSearch(term) })
                        }
                    }
                }
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
                            Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap)) {
                                if (uiState.movieResults.isNotEmpty()) {
                                    ContentRail(
                                        title = stringResource(R.string.search_top_results),
                                        items = uiState.movieResults,
                                        countLabel = uiState.movieResults.size.toString(),
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
                                        countLabel = uiState.seriesResults.size.toString(),
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
                                        countLabel = uiState.channelResults.size.toString(),
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
