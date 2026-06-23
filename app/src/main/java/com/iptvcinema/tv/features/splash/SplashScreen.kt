package com.iptvcinema.tv.features.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.design.components.CinemaBrandMark
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.LoadingIndicator
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

    CinemaScreen(showTopNav = false, showRemoteHints = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(
                            CinemaColors.GoldDeep.copy(alpha = 0.22f),
                            CinemaColors.BackgroundSoft,
                            CinemaColors.Background,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(360.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(CinemaColors.Gold.copy(alpha = 0.28f), CinemaColors.Background.copy(alpha = 0f)),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(360.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(CinemaColors.Gold.copy(alpha = 0.24f), CinemaColors.Background.copy(alpha = 0f)),
                        ),
                    ),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CinemaBrandMark(size = 112.dp)
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CinemaColors.Gold,
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.splash_tagline),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = CinemaColors.TextSecondary,
                        fontWeight = FontWeight.Light,
                    ),
                    textAlign = TextAlign.Center,
                )
                LoadingIndicator(text = stringResource(R.string.splash_loading))
            }
        }
    }
}
