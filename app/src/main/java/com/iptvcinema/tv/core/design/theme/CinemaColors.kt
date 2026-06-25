package com.iptvcinema.tv.core.design.theme

import androidx.compose.ui.graphics.Color

object CinemaColors {
    val Background = Color(0xFF141414)
    val BackgroundSoft = Color(0xFF1A1A1A)
    val Surface = Color(0xFF2A2A2A)
    val SurfaceSoft = Color(0xFF333333)
    val SurfaceGlass = Color(0xB3181818)

    val Accent = Color(0xFFE50914)
    val AccentSoft = Color(0xFFFF3B46)
    val AccentDeep = Color(0xFFB20710)
    val AccentGlow = Color(0xFFE50914)

    // Backwards-compatible aliases — now map to the red brand accent.
    val Gold = Accent
    val GoldSoft = AccentSoft
    val GoldDeep = AccentDeep

    val AmberWarm = Color(0xFFFFC95C)

    val White = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFE5E5E5)
    val TextSecondary = Color(0xFF999999)
    val TextMuted = Color(0xFF666666)

    val Border = Color(0xFF404040)
    val FocusBorder = Color(0xFFE5E5E5)

    val LiveRed = Color(0xFFE50914)
    val Success = Color(0xFF46D369)
    val Warning = Color(0xFFF5C518)
    val Danger = Color(0xFFE50914)
}
