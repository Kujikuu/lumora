package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

/**
 * Tracks when horizontal content rails are scrolled so the shell nav can hide
 * and cards can extend under the former rail area (Yango home-scrolled behavior).
 */
@Stable
class ShellImmersionState {
    var hideNavRail by mutableStateOf(false)
        private set

    fun showNavRail() {
        hideNavRail = false
    }

    fun updateRailImmersion(
        hasRailFocus: Boolean,
        focusedItemIndex: Int,
        listState: LazyListState,
    ) {
        if (!hasRailFocus) return
        hideNavRail = shouldImmerse(focusedItemIndex, listState)
    }

    fun updateRailScroll(listState: LazyListState, focusedItemIndex: Int) {
        hideNavRail = shouldImmerse(focusedItemIndex, listState)
    }

    private fun shouldImmerse(focusedItemIndex: Int, listState: LazyListState): Boolean =
        focusedItemIndex > 0 ||
            listState.firstVisibleItemIndex > 0 ||
            listState.firstVisibleItemScrollOffset > 24
}

val LocalShellImmersion = compositionLocalOf<ShellImmersionState?> { null }

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
        animationSpec = tween(durationMillis = 200),
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
        animationSpec = tween(durationMillis = 200),
        label = "shellHeroContentStart",
    )
    return animated
}
