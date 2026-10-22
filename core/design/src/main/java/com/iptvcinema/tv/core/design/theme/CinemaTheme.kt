package com.iptvcinema.tv.core.design.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val CinemaColorScheme = darkColorScheme(
    primary = CinemaColors.White,
    onPrimary = CinemaColors.Background,
    primaryContainer = CinemaColors.Accent,
    onPrimaryContainer = CinemaColors.White,
    secondary = CinemaColors.TextSecondary,
    onSecondary = CinemaColors.Background,
    background = CinemaColors.Background,
    onBackground = CinemaColors.TextPrimary,
    surface = CinemaColors.Surface,
    onSurface = CinemaColors.TextPrimary,
    surfaceVariant = CinemaColors.SurfaceSoft,
    onSurfaceVariant = CinemaColors.TextSecondary,
    error = CinemaColors.Danger,
    onError = CinemaColors.White,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CinemaColorScheme,
        typography = rememberCinemaTypography(),
        content = {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                content()
            }
        },
    )
}
