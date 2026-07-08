package com.iptvcinema.tv.core.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.iptvcinema.tv.R

val GraphikArabicFontFamily: FontFamily = FontFamily(
    Font(R.font.graphik_arabic_light, FontWeight.Light),
    Font(R.font.graphik_arabic_regular, FontWeight.Normal),
    Font(R.font.graphik_arabic_medium, FontWeight.Medium),
    Font(R.font.graphik_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.graphik_arabic_bold, FontWeight.Bold),
)

@Composable
fun rememberAppFontFamily(): FontFamily = remember { GraphikArabicFontFamily }
