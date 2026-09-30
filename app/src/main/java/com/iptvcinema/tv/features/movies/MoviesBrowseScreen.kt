package com.iptvcinema.tv.features.movies

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.features.browse.BrowseTabScreen

/** The Movies tab landing; category tiles open [MoviesScreen], the full grid. */
@Composable
fun MoviesBrowseScreen(
    navController: NavController,
    viewModel: MoviesBrowseViewModel = hiltViewModel(),
) {
    BrowseTabScreen(
        navController = navController,
        navItem = NavItem.Movies,
        focusKey = "movies_browse",
        title = stringResource(R.string.nav_movies),
        emptyTitle = stringResource(R.string.movies_empty_title),
        emptyDescription = stringResource(R.string.catalog_empty_sync_desc),
        viewModel = viewModel,
    )
}
