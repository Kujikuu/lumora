package com.iptvcinema.tv.features.series

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.features.browse.BrowseTabScreen

/** The Series tab landing; category tiles open [SeriesScreen], the full grid. */
@Composable
fun SeriesBrowseScreen(
    navController: NavController,
    viewModel: SeriesBrowseViewModel = hiltViewModel(),
) {
    BrowseTabScreen(
        navController = navController,
        navItem = NavItem.Series,
        focusKey = "series_browse",
        title = stringResource(R.string.nav_series),
        emptyTitle = stringResource(R.string.series_empty_title),
        emptyDescription = stringResource(R.string.catalog_empty_sync_desc),
        viewModel = viewModel,
    )
}
