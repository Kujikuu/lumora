package com.iptvcinema.tv.core.design.components

import androidx.compose.ui.graphics.Color

/** Colours only the settings screens and their dialogs use. */
object SettingsPalette {
    /** Readable red for destructive labels and error lines on dark panels. */
    val DangerText = Color(0xFFFF6B6B)
    /** Red tint behind blocked-category chips and destructive dialog actions. */
    val DangerTint = Color(0x2EE02424)
    val SwitchTrackOff = Color(0xFF3A3F4B)
    val SwitchKnobOff = Color(0xFFC3C7CF)
    /** Secondary text on a focused (white) row. */
    val FocusedSecondaryText = Color(0xFF4A4F5A)
    /** Outline of a PIN dot that has no digit yet. */
    val PinDotEmpty = Color(0xFF5C6270)
}
