package com.iptvcinema.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.OneShotPreDrawListener
import com.iptvcinema.launcher.home.HomeScreen
import com.iptvcinema.tv.core.design.theme.CinemaTheme

/**
 * The home screen. Declared singleTask with every configuration change handled in place, so
 * Home presses, standby and language switches reuse this instance instead of recreating it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CinemaTheme {
                HomeScreen()
            }
        }
        // Data sources start once the first frame is up, so they never delay it.
        val decorView = window.decorView
        OneShotPreDrawListener.add(decorView) { decorView.post { LauncherGraph.startAfterFirstFrame() } }
    }

    override fun onStart() {
        super.onStart()
        LauncherGraph.clock.start()
        LauncherGraph.network.start()
    }

    override fun onStop() {
        LauncherGraph.clock.stop()
        LauncherGraph.network.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LauncherGraph.homePresses.tryEmit(Unit)
    }
}
