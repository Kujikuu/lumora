package com.iptvcinema.tv.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.iptvcinema.tv.core.design.components.CatalogStateCallbacks

@Composable
fun rememberCatalogStateCallbacks(
    navController: NavController,
    onRetry: () -> Unit = {},
): CatalogStateCallbacks = remember(navController, onRetry) {
    CatalogStateCallbacks(
        onAddSource = { navController.navigate(AppRoute.ADD_SOURCE) },
        onTryDemo = { navController.navigate(AppRoute.HOME) },
        onRetry = onRetry,
        onManageSources = { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) },
        onEditSource = { navController.navigate(AppRoute.M3U_FORM) },
    )
}
