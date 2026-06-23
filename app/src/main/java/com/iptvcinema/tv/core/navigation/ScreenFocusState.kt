package com.iptvcinema.tv.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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

    suspend fun restoreFocus(focusRequester: FocusRequester) {
        delay(FOCUS_RESTORE_DELAY_MS)
        runCatching { focusRequester.requestFocus() }
    }

    suspend fun requestInitialFocus(focusRequester: FocusRequester) {
        delay(FOCUS_RESTORE_DELAY_MS)
        runCatching { focusRequester.requestFocus() }
    }

    companion object {
        const val NO_SAVED_FOCUS = -1
        private const val FOCUS_RESTORE_DELAY_MS = 50L

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
