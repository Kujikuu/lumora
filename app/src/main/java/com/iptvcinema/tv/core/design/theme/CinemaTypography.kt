package com.iptvcinema.tv.core.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import com.iptvcinema.tv.R

@Composable
fun rememberCinemaTypography(): Typography {
    val configuration = LocalConfiguration.current
    val isArabic = configuration.locales[0].language == "ar"
    return remember(isArabic) {
        val bodyFont = if (isArabic) {
            FontFamily(Font(R.font.noto_sans_arabic))
        } else {
            FontFamily.SansSerif
        }
        val displayFont = if (isArabic) bodyFont else FontFamily.Serif

        Typography(
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
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        color = CinemaColors.TextPrimary,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        color = CinemaColors.TextPrimary,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        color = CinemaColors.TextPrimary,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        color = CinemaColors.TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        color = CinemaColors.TextPrimary,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        color = CinemaColors.TextPrimary,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        color = CinemaColors.TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = CinemaColors.TextPrimary,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = CinemaColors.TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        color = CinemaColors.TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = CinemaColors.TextSecondary,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = CinemaColors.TextSecondary,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = CinemaColors.TextPrimary,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = CinemaColors.TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = CinemaColors.TextMuted,
    ),
)
