package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState

/** Shared TV scroll/focus animation timings — keep in sync with nav-rail transitions. */
object TvScrollMotion {
    const val SHELL_MS = 200
    const val HORIZONTAL_MS = 200
    const val FOCUS_SCALE_MS = 180

    val focusScaleTween = tween<Float>(
        durationMillis = FOCUS_SCALE_MS,
        easing = FastOutSlowInEasing,
    )
}

/**
 * Glides a rail so the focused card sits at the start. Uses a fixed-length tween on the
 * pixel distance, so each D-pad step takes the same time as the focus-scale animation
 * and the rail moves in step with the card instead of lurching.
 */
suspend fun LazyListState.animateToFocusedItem(index: Int) {
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo == null) {
        animateScrollToItem(index)
        return
    }
    val delta = (itemInfo.offset - layoutInfo.beforeContentPadding).toFloat()
    if (delta == 0f) return
    animateScrollBy(delta, railGlideTween)
}

/** Smooth horizontal scroll for non-lazy rows (e.g. mood tiles). */
suspend fun ScrollState.animateScrollToValue(
    target: Int,
    durationMillis: Int = TvScrollMotion.HORIZONTAL_MS,
) {
    if (value == target) return
    animateScrollTo(target, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
}

private val railGlideTween = tween<Float>(
    durationMillis = TvScrollMotion.HORIZONTAL_MS,
    easing = FastOutSlowInEasing,
)

/** Skip redundant parent scroll when the section is already the primary visible row. */
fun LazyListState.isSectionVisible(sectionIndex: Int): Boolean =
    sectionIndex >= 0 && firstVisibleItemIndex == sectionIndex
