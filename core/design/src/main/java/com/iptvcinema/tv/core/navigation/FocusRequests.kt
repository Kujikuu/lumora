package com.iptvcinema.tv.core.navigation

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

private const val DEFAULT_FOCUS_FRAME_ATTEMPTS = 90

/**
 * Requests focus once the target is attached, trying once per frame for a short while.
 *
 * Asking a [FocusRequester] whose node is not composed yet throws ("FocusRequester is not
 * initialized"). That happens in effects that run before a dialog window or a conditional panel
 * has laid out, most often on slow TVs. Returns whether focus landed.
 */
suspend fun FocusRequester.requestFocusWhenReady(maxFrames: Int = DEFAULT_FOCUS_FRAME_ATTEMPTS): Boolean {
    repeat(maxFrames) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.getOrDefault(false)) return true
    }
    return false
}
