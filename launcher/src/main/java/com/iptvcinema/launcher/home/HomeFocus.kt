package com.iptvcinema.launcher.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.focus.FocusRequester
import com.iptvcinema.tv.core.design.components.TvScrollMotion
import com.iptvcinema.tv.core.navigation.requestFocusWhenReady
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns vertical focus moves. Up and Down are routed through one entry requester per row (and one
 * for the header) instead of geometric focus search, which could land on any card that happens to
 * be nearest; the rows area glides so the focused row sits at its top.
 */
class HomeFocusController(
    private val scope: CoroutineScope,
    private val scrollState: ScrollState,
    rowCount: Int,
) {
    val header = FocusRequester()
    val rows: List<FocusRequester> = List(rowCount) { FocusRequester() }

    /** Top of each row inside the scrolling column, written by the rows as they are laid out. */
    val rowOffsets = IntArray(rowCount)

    /** The row that holds focus, or -1 for the header. Plain field: nothing redraws from it. */
    var focusedRow = -1
        private set

    private var pendingMove: Job? = null

    fun toRow(index: Int) {
        if (index !in rows.indices) return
        move(index, rows[index])
    }

    fun toHeader() = move(-1, header)

    /** Restores focus to wherever it was (after a dialog closed, for example). */
    fun restore() = if (focusedRow >= 0) toRow(focusedRow) else toHeader()

    fun onRowFocused(index: Int) {
        focusedRow = index
        alignRow(index)
    }

    fun onHeaderFocused() {
        focusedRow = -1
        if (scrollState.value != 0) scope.launch { scrollState.animateScrollTo(0, glideTween) }
    }

    private fun move(index: Int, target: FocusRequester) {
        pendingMove?.cancel()
        pendingMove = scope.launch {
            if (index >= 0) launch { glideTo(rowOffsets[index]) }
            target.requestFocusWhenReady()
        }
    }

    private fun alignRow(index: Int) {
        val target = rowOffsets[index]
        if (scrollState.value == target) return
        scope.launch { glideTo(target) }
    }

    private suspend fun glideTo(offset: Int) {
        val clamped = offset.coerceIn(0, scrollState.maxValue)
        if (scrollState.value != clamped) scrollState.animateScrollTo(clamped, glideTween)
    }

    private val glideTween = tween<Float>(durationMillis = TvScrollMotion.HORIZONTAL_MS, easing = FastOutSlowInEasing)
}
