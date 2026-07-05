package com.iptvcinema.tv.features.states

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.navigation.AppRoute

@Composable
fun EmptyStateScreen(
    navController: NavController,
) {
    BackHandler { navController.popBackStack() }

    CinemaScreen(showTopNav = false) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                title = stringResource(R.string.empty_title),
                description = stringResource(R.string.empty_description),
                primaryAction = stringResource(R.string.btn_add_source),
                secondaryAction = null,
                onPrimary = { navController.navigate(AppRoute.ADD_SOURCE) },
                onSecondary = null,
                footerNote = stringResource(R.string.source_footer),
            )
        }
    }
}

@Composable
fun ErrorStateScreen(
    navController: NavController,
) {
    BackHandler { navController.popBackStack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        ErrorState(
            title = stringResource(R.string.error_title),
            description = stringResource(R.string.error_description),
            errorCode = stringResource(R.string.error_code),
            onRetry = { navController.popBackStack() },
            onSwitchStream = { navController.navigate(AppRoute.liveTv()) },
            onBack = { navController.navigate(AppRoute.liveTv()) },
        )
    }
}
