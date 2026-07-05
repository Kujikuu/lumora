package com.iptvcinema.tv.core.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

data class ScreenFocusSnapshot(
    val focusIndex: Int = ScreenFocusState.NO_SAVED_FOCUS,
    val sectionId: String = "",
    val itemIndex: Int = 0,
    val scrollOffset: Int = 0,
    val focusedContentId: String = "",
    val initialFocusHandled: Boolean = false,
)

class ScreenFocusState(
    initial: ScreenFocusSnapshot = ScreenFocusSnapshot(),
) {
    var snapshot: ScreenFocusSnapshot = initial
        private set

    val focusIndex: Int
        get() = snapshot.focusIndex

    val sectionId: String
        get() = snapshot.sectionId

    val itemIndex: Int
        get() = snapshot.itemIndex

    val scrollOffset: Int
        get() = snapshot.scrollOffset

    val focusedContentId: String
        get() = snapshot.focusedContentId

    val hasSavedFocus: Boolean
        get() = snapshot.focusIndex != NO_SAVED_FOCUS || snapshot.sectionId.isNotBlank()

    val initialFocusHandled: Boolean
        get() = snapshot.initialFocusHandled

    fun saveFocusIndex(index: Int) {
        snapshot = snapshot.copy(focusIndex = index)
    }

    fun saveBrowseFocus(
        sectionId: String,
        itemIndex: Int = 0,
        scrollOffset: Int = snapshot.scrollOffset,
        focusedContentId: String = snapshot.focusedContentId,
        categoryIndex: Int = snapshot.focusIndex,
    ) {
        snapshot = snapshot.copy(
            sectionId = sectionId,
            itemIndex = itemIndex,
            scrollOffset = scrollOffset,
            focusedContentId = focusedContentId,
            focusIndex = categoryIndex,
        )
    }

    fun saveScrollOffset(offset: Int) {
        snapshot = snapshot.copy(scrollOffset = offset)
    }

    fun markInitialFocusHandled() {
        snapshot = snapshot.copy(initialFocusHandled = true)
    }

    suspend fun restoreFocus(focusRequester: FocusRequester): Boolean = requestFocusAfterComposition(focusRequester)

    suspend fun requestInitialFocus(focusRequester: FocusRequester): Boolean {
        val result = requestFocusAfterComposition(focusRequester)
        if (result) {
            markInitialFocusHandled()
        }
        return result
    }

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

        val Saver: Saver<ScreenFocusState, ScreenFocusSnapshot> = Saver(
            save = { it.snapshot },
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

@Composable
fun rememberCatalogScrollState(focusState: ScreenFocusState): ScrollState {
    return rememberSaveable(
        focusState.scrollOffset,
        saver = ScrollState.Saver,
    ) {
        ScrollState(focusState.scrollOffset)
    }
}
