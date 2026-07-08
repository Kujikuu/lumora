package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.delay

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

/** Smooth horizontal rail scroll when D-pad focus moves between cards. */
suspend fun LazyListState.animateToFocusedItem(index: Int) {
    animateScrollToItem(index)
}

/** Smooth horizontal scroll for non-lazy rows (e.g. mood tiles). */
suspend fun ScrollState.animateScrollToValue(
    target: Int,
    durationMillis: Int = TvScrollMotion.HORIZONTAL_MS,
) {
    val start = value
    if (start == target) return
    val steps = (durationMillis / 16).coerceAtLeast(4)
    val stepDelay = durationMillis / steps
    for (step in 1..steps) {
        val fraction = FastOutSlowInEasing.transform(step.toFloat() / steps)
        scrollTo((start + (target - start) * fraction).toInt())
        if (step < steps) delay(stepDelay.toLong())
    }
    scrollTo(target)
}

/** Skip redundant parent scroll when the section is already the primary visible row. */
fun LazyListState.isSectionVisible(sectionIndex: Int): Boolean =
    sectionIndex >= 0 && firstVisibleItemIndex == sectionIndex
