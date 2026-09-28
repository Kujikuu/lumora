package com.iptvcinema.tv.core.navigation

import androidx.navigation.NavController

// Pops the whole graph, not just SPLASH: SPLASH is gone after the first navigation, and
// popping to a missing route is a no-op that lets the back stack grow forever.
fun NavController.navigateOnboardingClearingStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}

fun NavController.navigateToMainShell(route: String = AppRoute.HOME) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateMainShellHome() {
    if (AppRoute.routeToNavItem(currentDestination?.route) == NavItem.Home) return
    if (!popBackStack(AppRoute.HOME, inclusive = false)) {
        navigate(AppRoute.HOME) {
            popUpTo(AppRoute.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

fun NavController.navigateMainShellTab(route: String) {
    if (route == AppRoute.HOME) {
        navigateMainShellHome()
        return
    }
    navigate(route) {
        popUpTo(AppRoute.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToLiveChannel(channelId: String) {
    navigate(AppRoute.liveTv(channelId)) {
        popUpTo(AppRoute.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = false
    }
}

enum class ProfileSelectionMode {
    Onboarding,
    SwitchProfile,
    Manage,
}

enum class AddSourceMode {
    Onboarding,
    FromSettings,
}
