package com.iptvcinema.tv.core.design.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

// The nav rail stays visible in its collapsed form, like YouTube on TV, so content
// insets are fixed. The old hide-on-scroll animated every rail's padding each frame,
// which relayouted the whole screen during vertical D-pad moves.

/** Left inset for shell content rails, clear of the collapsed nav rail. */
fun shellContentStart(): Dp = CinemaSpacing.NavRailWidth + 16.dp

/** Left inset for hero text and buttons on shell screens. */
fun shellHeroContentStart(): Dp = CinemaSpacing.ContentStart

/** Fixed left inset for catalog browse screens. */
fun catalogContentStart(): Dp = CinemaSpacing.ContentStart
