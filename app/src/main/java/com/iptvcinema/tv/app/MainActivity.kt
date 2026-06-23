package com.iptvcinema.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.core.design.theme.CinemaTheme
import com.iptvcinema.tv.core.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            IptvCinemaAppContent()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IptvCinemaAppContent() {
    CinemaTheme {
        AppNavGraph()
    }
}
