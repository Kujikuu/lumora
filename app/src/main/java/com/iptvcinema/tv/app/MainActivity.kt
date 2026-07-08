package com.iptvcinema.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.core.design.theme.CinemaTheme
import com.iptvcinema.tv.core.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var isStartupReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isStartupReady }
        super.onCreate(savedInstanceState)
        setContent {
            IptvCinemaAppContent(
                onStartupReady = { isStartupReady = true },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IptvCinemaAppContent(
    onStartupReady: () -> Unit = {},
) {
    CinemaTheme {
        AppNavGraph(onStartupReady = onStartupReady)
    }
}
