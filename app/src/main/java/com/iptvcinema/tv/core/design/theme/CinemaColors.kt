package com.iptvcinema.tv.core.design.theme

import androidx.compose.ui.graphics.Color

object CinemaColors {
    val Background = Color(0xFF08090D)
    val BackgroundSoft = Color(0xFF0C0E14)
    val Surface = Color(0xFF11131A)
    val SurfaceSoft = Color(0xFF181B24)
    val SurfaceGlass = Color(0xD111131A)

    val Accent = Color(0xFFD6243A)
    val AccentSoft = Color(0xFFFF4D63)
    val AccentDeep = Color(0xFFA81830)
    val AccentGlow = Color(0xFFD6243A)

    val Secondary = Color(0xFF8B5CF6)
    val SecondarySoft = Color(0xFFA78BFA)
    val SecondaryDeep = Color(0xFF6D28D9)

    // Backwards-compatible aliases — older components still call these names.
    val Gold = Accent
    val GoldSoft = AccentSoft
    val GoldDeep = AccentDeep

    val AmberWarm = Color(0xFFFFC95C)

    val White = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB8BCC8)
    val TextMuted = Color(0xFF707684)

    val Border = Color(0xFF2A2D36)
    val FocusBorder = Color(0xFFFF4D63)

    val LiveRed = Color(0xFFE02424)
    val Success = Color(0xFF46D369)
    val Warning = Color(0xFFF5C518)
    val Danger = Color(0xFFE02424)
}
