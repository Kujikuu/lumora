package com.iptvcinema.tv.core.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

@Composable
fun BlockBackHandler() {
    BackHandler { }
}

@Composable
fun PopBackHandler(onBack: () -> Unit) {
    BackHandler { onBack() }
}

@Composable
fun OnboardingBackHandler(
    allowBack: Boolean,
    onBack: () -> Unit,
) {
    if (allowBack) {
        BackHandler { onBack() }
    } else {
        BlockBackHandler()
    }
}

@Composable
fun MainShellBackHandler(
    navController: NavController,
    isHomeTab: Boolean,
) {
    val context = LocalContext.current
    BackHandler {
        if (isHomeTab) {
            (context as? Activity)?.finish()
        } else {
            navController.navigateMainShellHome()
        }
    }
}
