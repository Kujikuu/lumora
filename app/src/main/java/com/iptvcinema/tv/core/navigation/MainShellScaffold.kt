package com.iptvcinema.tv.core.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.AccountDegradedBanner
import com.iptvcinema.tv.core.design.components.CinemaScreen

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
    sessionViewModel: SessionViewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
    onRailExitRight: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isCloudDegraded by sessionViewModel.isCloudDegraded.collectAsState()
    val accountDisplayName by sessionViewModel.accountDisplayName.collectAsState()
    val activeProfileName by sessionViewModel.activeProfileName.collectAsState()
    val navProfileTitle = activeProfileName
        ?: accountDisplayName.ifBlank { stringResource(R.string.nav_profile_guest) }
    val navProfileSubtitle = stringResource(R.string.nav_switch_profile)
    // The left navigation rail now covers Favorites/Search/etc., so the legacy
    // bottom browse footer is redundant chrome. Keep it off for a cleaner,
    // more cinematic layout that gives content rails more vertical room.
    val showBrowseFooter = false

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
        navProfileTitle = navProfileTitle,
        navProfileSubtitle = navProfileSubtitle,
        showBrowseFooter = showBrowseFooter,
        onFavoritesClick = { shellNavigate(navController, AppRoute.MY_LIST) },
        onRecentlyAddedClick = { shellNavigate(navController, AppRoute.movies()) },
        onTopRatedClick = { shellNavigate(navController, AppRoute.movies("Top Rated")) },
        onRailExitRight = onRailExitRight,
    ) {
        Column {
            if (isCloudDegraded) {
                AccountDegradedBanner(onRetry = sessionViewModel::retryCloudSync)
            }
            content()
        }
    }
}
