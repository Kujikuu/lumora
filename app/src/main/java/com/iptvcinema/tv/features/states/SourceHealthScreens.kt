package com.iptvcinema.tv.features.states

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.ExpiredAccountState
import com.iptvcinema.tv.core.design.components.InvalidPlaylistState
import com.iptvcinema.tv.core.navigation.AppRoute

@Composable
fun ExpiredAccountScreen(navController: NavController) {
    BackHandler { navController.popBackStack() }
    CinemaScreen(showTopNav = false) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ExpiredAccountState(
                onReconnect = { navController.navigate(AppRoute.ADD_SOURCE) },
                onManageSources = { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) },
            )
        }
    }
}

@Composable
fun InvalidPlaylistScreen(navController: NavController) {
    BackHandler { navController.popBackStack() }
    CinemaScreen(showTopNav = false) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InvalidPlaylistState(
                onEditSource = { navController.navigate(AppRoute.M3U_FORM) },
                onManageSources = { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) },
            )
        }
    }
}
