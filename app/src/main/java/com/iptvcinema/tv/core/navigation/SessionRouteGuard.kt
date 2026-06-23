package com.iptvcinema.tv.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iptvcinema.tv.core.datastore.SessionRequirement

@Composable
fun SessionRouteGuard(
    navController: NavController,
    requirement: SessionRequirement,
    sessionViewModel: SessionViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    content: @Composable () -> Unit,
) {
    val session by sessionViewModel.sessionState.collectAsState()
    val isHydrated by sessionViewModel.isHydrated.collectAsState()

    LaunchedEffect(session, requirement, isHydrated) {
        if (!isHydrated) return@LaunchedEffect
        val redirectRoute = session.redirectRouteFor(requirement) ?: return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route.orEmpty()
        val currentBase = currentRoute.substringBefore("?").substringBefore("/")
        val redirectBase = redirectRoute.substringBefore("?").substringBefore("/")
        if (currentBase == redirectBase) return@LaunchedEffect
        navController.navigateOnboardingClearingStack(redirectRoute)
    }

    if (!isHydrated || session.meetsRequirement(requirement)) {
        content()
    }
}
