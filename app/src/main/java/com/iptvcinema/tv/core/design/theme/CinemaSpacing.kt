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

    /** Space between poster artwork and the title row below a card. */
    val CardTitleTopGap = 10.dp
    /** Space between a card title and its subtitle (e.g. episode name). */
    val CardTitleSubtitleGap = 6.dp

    val HeroMinHeight = 430.dp
    val HeroFeaturedMinHeight = 300.dp
    val HeroMaxHeight = 440.dp

    val NavRailWidth = 92.dp
    val NavRailExpandedWidth = 340.dp
    /** Icon column width inside the rail (rail width minus horizontal padding). */
    val NavRailIconSlotWidth = 80.dp
    /** Nav icons at ~70% of the previous 28dp size; same in collapsed and expanded. */
    val NavRailIconSize = 20.dp
    val NavRailActiveIndicatorWidth = 24.dp
    val NavRailProfileAvatarSize = 24.dp
    val NavRailItemMinHeight = 48.dp

    /** Standard portrait poster width in horizontal rails (2:3 aspect). */
    val PosterCardWidth = 90.dp

    /** Left padding for non-hero content that must clear the nav rail. */
    val ContentStart = NavRailWidth + 8.dp

    val ExpandedPosterCardWidth = 260.dp
    val ExpandedPosterCardHeight = 186.dp
    val ExpandedPosterPanelMinHeight = 78.dp
    val CompactPosterCardHeight = 123.dp
    val CompactPortraitCardTotalHeight = 123.dp
    val CompactLandscapePosterHeight = 84.dp
    val CompactLandscapeCardTotalHeight = 62.dp
    val ExpandedLandscapeCardHeight = 194.dp
    /** 16:9 poster-only landscape cards (Recommended Series, New Releases). */
    val ExpandedLandscapePosterOnlyHeight = 146.dp
    val ExpandedLandscapePosterTitleHeight = 26.dp
    val ExpandedLandscapePosterTotalHeight =
        ExpandedLandscapePosterOnlyHeight + ExpandedLandscapePosterTitleHeight + CardTitleTopGap
    val MoodTileSize = 60.dp
    val HeroThumbSize = 28.dp

    /** Vertical padding so focus-scaled cards do not clip inside fixed-height rails. */
    val FocusScaleOverflow = 8.dp
}
