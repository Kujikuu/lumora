package com.iptvcinema.tv.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object CinemaShapes {
    /** Rounded corners for all media/content cards (posters, 16:9 cards, tiles). */
    val Card = RoundedCornerShape(10.dp)
    val Small = RoundedCornerShape(4.dp)
    val Medium = RoundedCornerShape(6.dp)
    val Large = RoundedCornerShape(8.dp)
    val XLarge = RoundedCornerShape(12.dp)
    /** List rows and panels (channel rows, settings rows). */
    val Row = RoundedCornerShape(14.dp)
    /** Fully rounded ends for buttons, chips, and pill controls. */
    val Pill = RoundedCornerShape(percent = 50)
}
