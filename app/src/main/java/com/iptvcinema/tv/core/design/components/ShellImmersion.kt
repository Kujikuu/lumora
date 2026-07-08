package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

internal object ShellImmersionPolicy {
    const val IMMERSE_ENTER_OFFSET_PX = 48
    const val IMMERSE_RELEASE_OFFSET_PX = 8

    fun nextHideNavRail(
        currentlyHidden: Boolean,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ): Boolean = when {
        firstVisibleItemIndex > 0 -> true
        firstVisibleItemScrollOffset > IMMERSE_ENTER_OFFSET_PX -> true
        firstVisibleItemScrollOffset < IMMERSE_RELEASE_OFFSET_PX && firstVisibleItemIndex == 0 -> false
        else -> currentlyHidden
    }
}

/**
 * Tracks when the main vertical list is scrolled so the shell nav can hide
 * and cards can extend under the former rail area.
 *
 * Must be driven by the screen's vertical [LazyListState] only — never by
 * horizontal rail scroll, or horizontal D-pad moves will animate padding and
 * cause vertical jitter.
 */
@Stable
class ShellImmersionState {
    var hideNavRail by mutableStateOf(false)
        private set

    fun showNavRail() {
        hideNavRail = false
    }

    fun trackVerticalScroll(listState: LazyListState) {
        hideNavRail = ShellImmersionPolicy.nextHideNavRail(
            currentlyHidden = hideNavRail,
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
        )
    }
}

val LocalShellImmersion = compositionLocalOf<ShellImmersionState?> { null }

@Composable
fun TrackShellVerticalScroll(listState: LazyListState) {
    val shellImmersion = LocalShellImmersion.current
    LaunchedEffect(listState, shellImmersion) {
        if (shellImmersion == null) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect {
            shellImmersion.trackVerticalScroll(listState)
        }
    }
}

@Composable
fun shellContentStart(): Dp {
    val immersion = LocalShellImmersion.current
    val target = if (immersion?.hideNavRail == true) {
        CinemaSpacing.ScreenPadding
    } else {
        CinemaSpacing.NavRailWidth + 16.dp
    }
    val animated by animateDpAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = TvScrollMotion.SHELL_MS, easing = FastOutSlowInEasing),
        label = "shellContentStart",
    )
    return animated
}

@Composable
fun shellHeroContentStart(): Dp {
    val immersion = LocalShellImmersion.current
    val target = if (immersion?.hideNavRail == true) {
        CinemaSpacing.ScreenPadding
    } else {
        CinemaSpacing.ContentStart
    }
    val animated by animateDpAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = TvScrollMotion.SHELL_MS, easing = FastOutSlowInEasing),
        label = "shellHeroContentStart",
    )
    return animated
}

/** Fixed left inset for catalog browse screens — avoids animated padding jitter on horizontal focus moves. */
fun catalogContentStart(): Dp = CinemaSpacing.ContentStart
