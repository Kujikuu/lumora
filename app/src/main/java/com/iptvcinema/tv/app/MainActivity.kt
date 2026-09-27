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
import com.iptvcinema.tv.core.player.PlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var playerManager: PlayerManager

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

    override fun onDestroy() {
        // Screens only stop the shared player; decoders are freed once the UI is gone.
        playerManager.release()
        super.onDestroy()
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
