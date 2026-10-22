package com.iptvcinema.launcher.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Sizes for a 960x540 dp TV; every rail has a fixed height so vertical moves never relayout. */
object HomeDimens {
    val ScreenPadding = 36.dp
    val HeaderHeight = 64.dp
    val RowGap = 14.dp
    val RailTitleGap = 6.dp
    val CardGap = 8.dp
    val FocusOverflow = 12.dp
    val CardTitleHeight = 20.dp

    val LandscapeWidth = 168.dp
    val LandscapeHeight = 94.dp
    val PosterWidth = 92.dp
    val PosterHeight = 138.dp
    val InputWidth = 128.dp

    fun RowStyle.cardWidth(): Dp = when (this) {
        RowStyle.Landscape, RowStyle.Channel, RowStyle.App -> LandscapeWidth
        RowStyle.Poster -> PosterWidth
        RowStyle.Input -> InputWidth
    }

    fun RowStyle.cardHeight(): Dp = when (this) {
        RowStyle.Poster -> PosterHeight
        else -> LandscapeHeight
    }

    /** Whether the title sits under the artwork (tiles draw their own text). */
    fun RowStyle.hasTitleBelow(): Boolean = this == RowStyle.Landscape || this == RowStyle.Poster || this == RowStyle.App

    fun RowStyle.railHeight(): Dp =
        cardHeight() + FocusOverflow + (if (hasTitleBelow()) CardTitleHeight else 0.dp)
}
