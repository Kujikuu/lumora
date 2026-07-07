package com.iptvcinema.tv.features.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.navigation.BlockBackHandler

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SplashScreen(
    startupDestination: StartupDestination?,
    onNavigate: (StartupDestination) -> Unit,
) {
    BlockBackHandler()

    LaunchedEffect(startupDestination) {
        startupDestination?.let { onNavigate(it) }
    }

    CinemaScreen(showTopNav = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name).uppercase().replace(" ", "\n"),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.Accent,
                    fontSize = 118.sp,
                    lineHeight = 112.sp,
                    textAlign = TextAlign.Center,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}
