package com.iptvcinema.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/**
 * The current minute, updated from the system's minute tick only while the home screen is in
 * front. A Compose state read by the header alone, so a tick recomposes the clock and nothing else.
 */
class ClockTicker(private val context: Context) {
    var nowMs by mutableLongStateOf(System.currentTimeMillis())
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            nowMs = System.currentTimeMillis()
        }
    }
    private var registered = false

    fun start() {
        nowMs = System.currentTimeMillis()
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        registered = true
    }

    fun stop() {
        if (!registered) return
        context.unregisterReceiver(receiver)
        registered = false
    }
}
