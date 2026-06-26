package com.iptvcinema.tv.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

class ScreenFocusState(
    initialFocusIndex: Int = NO_SAVED_FOCUS,
) {
    var focusIndex: Int = initialFocusIndex
        private set

    val hasSavedFocus: Boolean
        get() = focusIndex != NO_SAVED_FOCUS

    fun saveFocusIndex(index: Int) {
        focusIndex = index
    }

    suspend fun restoreFocus(focusRequester: FocusRequester): Boolean = requestFocusAfterComposition(focusRequester)

    suspend fun requestInitialFocus(focusRequester: FocusRequester): Boolean = requestFocusAfterComposition(focusRequester)

    private suspend fun requestFocusAfterComposition(focusRequester: FocusRequester): Boolean {
        delay(FOCUS_RESTORE_DELAY_MS)
        withFrameNanos { }
        val firstRequest = runCatching { focusRequester.requestFocus() }.getOrDefault(false)
        withFrameNanos { }
        return firstRequest || runCatching { focusRequester.requestFocus() }.getOrDefault(false)
    }

    companion object {
        const val NO_SAVED_FOCUS = -1
        private const val FOCUS_RESTORE_DELAY_MS = 150L

        val Saver: Saver<ScreenFocusState, Int> = Saver(
            save = { it.focusIndex },
            restore = { ScreenFocusState(it) },
        )
    }
}

@Composable
fun rememberScreenFocusState(key: String): ScreenFocusState {
    return rememberSaveable(key, saver = ScreenFocusState.Saver) {
        ScreenFocusState()
    }
}

@Composable
fun rememberScreenFocusState(): ScreenFocusState {
    return rememberSaveable(saver = ScreenFocusState.Saver) {
        ScreenFocusState()
    }
}
