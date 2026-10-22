package com.iptvcinema.tv.core.design.theme

import androidx.compose.ui.graphics.Color

object CinemaColors {
    // Near-black with a blue tint, so artwork and white text stand out without a flat look.
    val Background = Color(0xFF05070C)
    val BackgroundSoft = Color(0xFF0A0D14)
    val Surface = Color(0xFF121620)
    val SurfaceSoft = Color(0xFF0D1018)
    /** Selected-but-not-focused fills (current menu item, selected chip). */
    val SurfaceRaised = Color(0xFF1B202B)
    val SurfaceGlass = Color(0xD9121620)
    /** Translucent fill for secondary buttons and badges over artwork. */
    val OverlayFill = Color(0x29FFFFFF)

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
    val TextSecondary = Color(0xFFC3C7CF)
    val TextMuted = Color(0xFF8A909C)

    val Border = Color(0xFF2A2F3A)
    val FocusBorder = Color(0xFFFFFFFF)

    val LiveRed = Color(0xFFE02424)
    val Success = Color(0xFF46D369)
    val Warning = Color(0xFFF5C518)
    val Danger = Color(0xFFE02424)

    // Lumora logo bands, sampled from the launcher mark.
    val LumoraYellow = Color(0xFFEDD900)
    val LumoraRed = Color(0xFFE70302)
    val LumoraBlue = Color(0xFF0200EC)
    val LumoraCyan = Color(0xFF00EBE8)
}
