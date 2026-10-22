package com.iptvcinema.tv.core.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

@Composable
fun rememberCinemaTypography(): Typography {
    val bodyFont = rememberAppFontFamily()
    val displayFont = bodyFont

    return Typography(
        displayLarge = textStyle(displayFont, FontWeight.Bold, 72, 80),
        displayMedium = textStyle(displayFont, FontWeight.Bold, 56, 64),
        displaySmall = textStyle(displayFont, FontWeight.Bold, 48, 56),
        headlineLarge = textStyle(bodyFont, FontWeight.SemiBold, 56, 64),
        headlineMedium = textStyle(bodyFont, FontWeight.SemiBold, 42, 50),
        headlineSmall = textStyle(bodyFont, FontWeight.SemiBold, 36, 44),
        titleLarge = textStyle(bodyFont, FontWeight.Medium, 30, 38),
        titleMedium = textStyle(bodyFont, FontWeight.Medium, 24, 32),
        titleSmall = textStyle(bodyFont, FontWeight.Medium, 20, 28),
        bodyLarge = textStyle(bodyFont, FontWeight.Normal, 22, 30, CinemaColors.TextPrimary),
        bodyMedium = textStyle(bodyFont, FontWeight.Normal, 18, 26, CinemaColors.TextSecondary),
        bodySmall = textStyle(bodyFont, FontWeight.Normal, 16, 24, CinemaColors.TextSecondary),
        labelLarge = textStyle(bodyFont, FontWeight.Medium, 18, 24),
        labelMedium = textStyle(bodyFont, FontWeight.Medium, 16, 22, CinemaColors.TextSecondary),
        labelSmall = textStyle(bodyFont, FontWeight.Medium, 14, 20, CinemaColors.TextMuted),
    )
}

private fun textStyle(
    fontFamily: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    color: androidx.compose.ui.graphics.Color = CinemaColors.TextPrimary,
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    color = color,
)

/** Default LTR typography for previews and non-composable contexts. */
val CinemaTypography = Typography(
    displayLarge = textStyle(GraphikArabicFontFamily, FontWeight.Bold, 72, 80),
    displayMedium = textStyle(GraphikArabicFontFamily, FontWeight.Bold, 56, 64),
    displaySmall = textStyle(GraphikArabicFontFamily, FontWeight.Bold, 48, 56),
    headlineLarge = textStyle(GraphikArabicFontFamily, FontWeight.SemiBold, 56, 64),
    headlineMedium = textStyle(GraphikArabicFontFamily, FontWeight.SemiBold, 42, 50),
    headlineSmall = textStyle(GraphikArabicFontFamily, FontWeight.SemiBold, 36, 44),
    titleLarge = textStyle(GraphikArabicFontFamily, FontWeight.Medium, 30, 38),
    titleMedium = textStyle(GraphikArabicFontFamily, FontWeight.Medium, 24, 32),
    titleSmall = textStyle(GraphikArabicFontFamily, FontWeight.Medium, 20, 28),
    bodyLarge = textStyle(GraphikArabicFontFamily, FontWeight.Normal, 22, 30, CinemaColors.TextPrimary),
    bodyMedium = textStyle(GraphikArabicFontFamily, FontWeight.Normal, 18, 26, CinemaColors.TextSecondary),
    bodySmall = textStyle(GraphikArabicFontFamily, FontWeight.Normal, 16, 24, CinemaColors.TextSecondary),
    labelLarge = textStyle(GraphikArabicFontFamily, FontWeight.Medium, 18, 24),
    labelMedium = textStyle(GraphikArabicFontFamily, FontWeight.Medium, 16, 22, CinemaColors.TextSecondary),
    labelSmall = textStyle(GraphikArabicFontFamily, FontWeight.Medium, 14, 20, CinemaColors.TextMuted),
)
