package com.iptvcinema.tv.core.design.theme

import androidx.compose.ui.graphics.Color

object CinemaColors {
    val Background = Color(0xFF000000)
    val BackgroundSoft = Color(0xFF070707)
    val Surface = Color(0xFF1F1F1F)
    val SurfaceSoft = Color(0xFF151515)
    val SurfaceGlass = Color(0xD9151515)

    val Accent = Color(0xFFE70302)
    val AccentSoft = Color(0xFFFF5A59)
    val AccentDeep = Color(0xFFB80201)
    val AccentGlow = Color(0xFFE70302)

    val Secondary = Color(0xFFFF9900)
    val SecondarySoft = Color(0xFFFFB13D)
    val SecondaryDeep = Color(0xFFD37A00)

    // Backwards-compatible aliases — older components still call these names.
    val Gold = Accent
    val GoldSoft = AccentSoft
    val GoldDeep = AccentDeep

    val AmberWarm = Color(0xFFFFB02E)

    val White = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFC8C8C8)
    val TextMuted = Color(0xFF8D8D8D)

    val Border = Color(0xFF2B2B2B)
    val FocusBorder = Color(0xFFFFFFFF)

    val LiveRed = Color(0xFFE02424)
    val Success = Color(0xFF46D369)
    val Warning = Color(0xFFF5C518)
    val Danger = Color(0xFFE02424)
}
