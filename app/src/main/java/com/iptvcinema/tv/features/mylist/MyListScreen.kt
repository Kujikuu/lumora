package com.iptvcinema.tv.features.mylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.ContentRail
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.PosterCard
import com.iptvcinema.tv.core.design.components.PosterGrid
import com.iptvcinema.tv.core.design.components.SkeletonPosterGrid
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyListScreen(
    navController: NavController,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val chipFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("my_list")
    val filters = rememberMyListFilters()
    var selectedFilter by remember(focusState.focusIndex) {
        mutableIntStateOf(
            if (focusState.hasSavedFocus) {
                focusState.focusIndex.coerceIn(0, filters.lastIndex)
            } else {
                0
            },
        )
    }
    val uiState by viewModel.uiState.collectAsState()

    MainShellBackHandler(navController = navController, isHomeTab = false)
    val selectedMyListFilter = filters[selectedFilter.coerceIn(0, filters.lastIndex)]

    LaunchedEffect(focusState.hasSavedFocus, selectedFilter) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(chipFocus)
        } else {
            focusState.requestInitialFocus(chipFocus)
            focusState.saveFocusIndex(selectedFilter)
        }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.MyList,
    ) {
        when (val state = uiState) {
            MyListUiState.Loading -> {
                Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap)) {
                    CinemaSerifTitle(text = stringResource(R.string.mylist_title))
                    SkeletonPosterGrid(columns = 4, rows = 2)
                }
            }
            is MyListUiState.Error -> {
                ErrorState(
                    title = stringResource(R.string.error_load_list),
                    description = state.message,
                    errorCode = "MY_LIST_ERROR",
                    onRetry = viewModel::loadMyList,
                    onSwitchStream = { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) },
                    onBack = { navController.popBackStack() },
                    showSwitchStream = false,
                    backLabel = stringResource(R.string.btn_back),
                )
            }
            is MyListUiState.Ready -> {
                val filteredItems = filterFavorites(selectedMyListFilter, state.favorites).map { it.toPosterCardData() }
                val recentlyWatched = state.recentlyWatchedPosters

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    CinemaSerifTitle(text = stringResource(R.string.mylist_title))
                    Text(
                        text = stringResource(R.string.mylist_subtitle),
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.saved_items_count,
                            filteredItems.size,
                            filteredItems.size,
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.Gold),
                    )
                    FilterChipRow(
                        items = filters.map { it.label },
                        selectedIndex = selectedFilter,
                        onSelected = {
                            selectedFilter = it
                            focusState.saveFocusIndex(it)
                        },
                        chipFocusRequester = chipFocus,
                        focusedChipIndex = selectedFilter,
                    )
                    if (filteredItems.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.mylist_nothing_saved),
                            description = stringResource(R.string.mylist_nothing_saved_desc),
                            primaryAction = stringResource(R.string.nav_movies),
                            secondaryAction = null,
                            onPrimary = { navController.navigate(AppRoute.MOVIES) },
                            onSecondary = null,
                        )
                    } else {
                        PosterGrid(
                            items = filteredItems,
                            onItemClick = { poster ->
                                poster.contentId?.let { contentId ->
                                    val favorite = state.favorites.find { it.contentId == contentId } ?: return@let
                                    when (favorite.contentType) {
                                        FavoriteContentType.MOVIE -> navController.navigate(AppRoute.movieDetails(contentId))
                                        FavoriteContentType.SERIES, FavoriteContentType.EPISODE ->
                                            navController.navigate(AppRoute.seriesDetails(contentId))
                                        FavoriteContentType.CHANNEL -> navController.navigate(AppRoute.liveTv(contentId))
                                    }
                                }
                            },
                        )
                    }
                    if (recentlyWatched.isNotEmpty()) {
                        ContentRail(
                            title = stringResource(R.string.rail_recently_watched),
                            items = recentlyWatched,
                            countLabel = recentlyWatched.size.toString(),
                        ) { poster ->
                            PosterCard(
                                data = poster,
                                onClick = {
                                    state.recentlyWatched.find { it.contentId == poster.contentId }?.let { item ->
                                        when (item.contentType) {
                                            com.iptvcinema.tv.core.model.WatchHistoryContentType.MOVIE ->
                                                navController.navigate(AppRoute.player(item.contentId, "movie"))
                                            com.iptvcinema.tv.core.model.WatchHistoryContentType.EPISODE ->
                                                navController.navigate(
                                                    AppRoute.player(item.contentId, "episode", item.seriesId),
                                                )
                                            com.iptvcinema.tv.core.model.WatchHistoryContentType.CHANNEL ->
                                                navController.navigate(AppRoute.player(item.contentId, "live"))
                                        }
                                    }
                                },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.mylist_remove_tip),
                        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberMyListFilters(): List<MyListFilter> = listOf(
    MyListFilter(stringResource(R.string.mylist_filter_all), null),
    MyListFilter(stringResource(R.string.mylist_filter_movies), FavoriteContentType.MOVIE),
    MyListFilter(stringResource(R.string.mylist_filter_series), FavoriteContentType.SERIES),
    MyListFilter(stringResource(R.string.mylist_filter_channels), FavoriteContentType.CHANNEL),
)

private data class MyListFilter(
    val label: String,
    val contentType: FavoriteContentType?,
)

private fun filterFavorites(filter: MyListFilter, favorites: List<FavoriteItem>): List<FavoriteItem> = when (filter.contentType) {
    null -> favorites
    FavoriteContentType.MOVIE -> favorites.filter { it.contentType == FavoriteContentType.MOVIE }
    FavoriteContentType.SERIES -> favorites.filter {
        it.contentType == FavoriteContentType.SERIES || it.contentType == FavoriteContentType.EPISODE
    }
    FavoriteContentType.CHANNEL -> favorites.filter { it.contentType == FavoriteContentType.CHANNEL }
    FavoriteContentType.EPISODE -> favorites
}
