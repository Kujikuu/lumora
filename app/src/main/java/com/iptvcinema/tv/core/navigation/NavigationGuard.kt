package com.iptvcinema.tv.core.navigation

import androidx.navigation.NavController

fun NavController.navigateOnboardingClearingStack(route: String) {
    navigate(route) {
        popUpTo(AppRoute.SPLASH) { inclusive = true }
    }
}

fun NavController.navigateToMainShell(route: String = AppRoute.HOME) {
    navigate(route) {
        popUpTo(AppRoute.SPLASH) { inclusive = true }
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

enum class ProfileSelectionMode {
    Onboarding,
    SwitchProfile,
}

enum class AddSourceMode {
    Onboarding,
    FromSettings,
}
