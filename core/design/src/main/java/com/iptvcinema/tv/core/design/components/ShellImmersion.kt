package com.iptvcinema.tv.core.design.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

// The nav rail stays visible in its collapsed form, like YouTube on TV, so content
// insets are fixed. The old hide-on-scroll animated every rail's padding each frame,
// which relayouted the whole screen during vertical D-pad moves.

/** Left inset for shell content rails, clear of the collapsed nav rail. */
fun shellContentStart(): Dp = CinemaSpacing.ContentStart

/** Left inset for hero text and buttons on shell screens. */
fun shellHeroContentStart(): Dp = CinemaSpacing.ContentStart

/** Fixed left inset for catalog browse screens. */
fun catalogContentStart(): Dp = CinemaSpacing.ContentStart

/**
 * Hides a horizontal rail's content under the collapsed nav rail. Cards that glide past the
 * start would otherwise draw behind the menu icons; clipping in draw keeps the full-bleed
 * backdrop behind the menu and costs no layout.
 */
fun Modifier.clipUnderNavRail(): Modifier = drawWithContent {
    val inset = CinemaSpacing.NavRailWidth.toPx()
    if (layoutDirection == LayoutDirection.Rtl) {
        clipRect(right = size.width - inset) { this@drawWithContent.drawContent() }
    } else {
        clipRect(left = inset) { this@drawWithContent.drawContent() }
    }
}
