package com.iptvcinema.tv.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.RemoteHint
import com.iptvcinema.tv.core.design.components.defaultRemoteHints
import com.iptvcinema.tv.core.design.components.myListRemoteHints
import com.iptvcinema.tv.core.design.components.searchRemoteHints
import com.iptvcinema.tv.core.navigation.NavItem.MyList
import com.iptvcinema.tv.core.navigation.NavItem.Search

private fun shellNavigate(
    navController: NavController,
    route: String,
) {
    navController.navigateMainShellTab(route)
}

@Composable
fun MainShellScaffold(
    navController: NavController,
    selectedNavItem: NavItem,
    remoteHints: List<RemoteHint>? = null,
    content: @Composable () -> Unit,
) {
    // The left navigation rail now covers Favorites/Search/etc., so the legacy
    // bottom browse footer is redundant chrome. Keep it off for a cleaner,
    // more cinematic layout that gives content rails more vertical room.
    val showBrowseFooter = false
    val showRemoteHints = true

    CinemaScreen(
        selectedNavItem = selectedNavItem,
        onNavigate = { item ->
            val route = AppRoute.navItemToRoute(item)
            val currentItem = AppRoute.routeToNavItem(navController.currentDestination?.route)
            if (item != currentItem) {
                shellNavigate(navController, route)
            }
        },
        onSettingsClick = { shellNavigate(navController, AppRoute.SETTINGS) },
        onSearchClick = { shellNavigate(navController, AppRoute.SEARCH) },
        onProfileClick = {
            navController.navigate(AppRoute.profileSelection(ProfileSelectionMode.SwitchProfile))
        },
        showRemoteHints = showRemoteHints,
        showBrowseFooter = showBrowseFooter,
        onFavoritesClick = { shellNavigate(navController, AppRoute.MY_LIST) },
        onRecentlyAddedClick = { shellNavigate(navController, AppRoute.movies()) },
        onTopRatedClick = { shellNavigate(navController, AppRoute.movies("Top Rated")) },
        remoteHints = remoteHints ?: when (selectedNavItem) {
            Search -> searchRemoteHints()
            MyList -> myListRemoteHints()
            else -> defaultRemoteHints()
        },
    ) {
        content()
    }
}
