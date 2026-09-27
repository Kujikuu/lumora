package com.iptvcinema.tv.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Stops the TV screensaver and sleep timer while [enabled]. Video playback does not
 * count as user activity on Android TV, so without this the screensaver starts mid-film.
 */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
