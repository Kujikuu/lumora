package com.iptvcinema.tv.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.OneShotPreDrawListener
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.core.design.theme.CinemaTheme
import com.iptvcinema.tv.core.navigation.AppNavGraph
import com.iptvcinema.tv.core.platform.AppLocaleHelper
import com.iptvcinema.tv.core.player.PlayerCommand
import com.iptvcinema.tv.core.player.PlayerManager
import com.iptvcinema.tv.core.tvhome.PendingDeepLinkStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var playerManager: PlayerManager

    @Inject
    lateinit var pendingDeepLinkStore: PendingDeepLinkStore

    @Inject
    lateinit var firstFrameSignal: FirstFrameSignal

    // Many TVs (the Haier test set among them) go to standby without stopping the activity, so
    // the screen-off broadcast is the only signal that nobody is watching any more.
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) pausePlayback()
        }
    }

    // Applies the language chosen in Settings; the system per-app language service is
    // missing on many TVs, so it cannot be relied on.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Released on the first frame; the animated intro lives in the Compose SplashScreen.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // A recreated activity already handled the link that launched it.
        if (savedInstanceState == null) pendingDeepLinkStore.offer(intent?.dataString)
        setContent {
            IptvCinemaAppContent()
        }
        // Pre-draw fires just before the first frame; the post runs once it is on screen.
        val decorView = window.decorView
        OneShotPreDrawListener.add(decorView) { decorView.post(firstFrameSignal::markDrawn) }
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        // Home button, another app, or standby on TVs that do stop the activity.
        pausePlayback()
        super.onStop()
    }

    /** Pauses rather than stops, so the viewer can resume where they were when they come back. */
    private fun pausePlayback() {
        playerManager.handleCommand(PlayerCommand.Pause)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkStore.offer(intent.dataString)
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
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
