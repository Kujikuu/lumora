package com.iptvcinema.tv.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.core.design.theme.CinemaTheme
import com.iptvcinema.tv.core.navigation.AppNavGraph
import com.iptvcinema.tv.core.platform.AppLocaleHelper
import com.iptvcinema.tv.core.player.PlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var playerManager: PlayerManager

    // Applies the language chosen in Settings; the system per-app language service is
    // missing on many TVs, so it cannot be relied on.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Released on the first frame; the animated intro lives in the Compose SplashScreen.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            IptvCinemaAppContent()
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
private fun IptvCinemaAppContent() {
    CinemaTheme {
        AppNavGraph()
    }
}
