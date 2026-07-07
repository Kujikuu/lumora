package com.iptvcinema.tv.core.design.theme

import androidx.compose.ui.unit.dp

object CinemaSpacing {
    val ScreenPadding = 30.dp
    val ScreenPaddingVertical = 12.dp
    val NavBottomPadding = 4.dp
    val SectionGap = 18.dp
    val CardGap = 6.dp
    val RailGap = 5.dp
    val ButtonGap = 7.dp

    val HeroMinHeight = 430.dp
    val HeroFeaturedMinHeight = 300.dp
    val HeroMaxHeight = 440.dp

    val NavRailWidth = 92.dp

    /** Standard portrait poster width in horizontal rails (2:3 aspect). */
    val PosterCardWidth = 90.dp

    /** Left padding for non-hero content that must clear the nav rail. */
    val ContentStart = NavRailWidth + 8.dp

    val ExpandedPosterCardWidth = 260.dp
    val ExpandedPosterCardHeight = 180.dp
    val ExpandedPosterPanelMinHeight = 74.dp
    val CompactPosterCardHeight = 123.dp
    val CompactPortraitCardTotalHeight = 123.dp
    val CompactLandscapePosterHeight = 84.dp
    val CompactLandscapeCardTotalHeight = 62.dp
    val ExpandedLandscapeCardHeight = 184.dp
    val MoodTileSize = 60.dp
    val HeroThumbSize = 28.dp

    /** Vertical padding so focus-scaled cards do not clip inside fixed-height rails. */
    val FocusScaleOverflow = 8.dp
}
