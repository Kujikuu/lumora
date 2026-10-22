package com.iptvcinema.launcher.inputs

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.iptvcinema.launcher.LauncherGraph
import com.iptvcinema.launcher.R

/**
 * Full-screen view of a passthrough input (HDMI, VGA, YPbPr, AV) through the TV Input Framework,
 * for boxes whose system has no viewer of its own. If the input cannot be shown within a few
 * seconds, the system's input picker takes over. Plain views: no Compose runtime to start here.
 */
class InputViewerActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvView: TvView
    private lateinit var status: TextView
    private val giveUp = Runnable { fallBack() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val inputId = intent.getStringExtra(EXTRA_INPUT_ID) ?: run { finish(); return }
        tvView = TvView(this)
        status = TextView(this).apply {
            text = getString(R.string.input_connecting)
            setTextColor(0xFFC3C7CF.toInt())
            textSize = 20f
        }
        setContentView(
            FrameLayout(this).apply {
                addView(tvView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                addView(status, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
            },
        )
        tvView.setCallback(
            object : TvView.TvInputCallback() {
                override fun onConnectionFailed(inputId: String) = fallBack()
                override fun onDisconnected(inputId: String) = fallBack()
                override fun onVideoAvailable(inputId: String) {
                    handler.removeCallbacks(giveUp)
                    status.visibility = android.view.View.GONE
                }
                override fun onVideoUnavailable(inputId: String, reason: Int) {
                    // Tuning and buffering resolve on their own; anything else is a real failure.
                    if (reason != TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING &&
                        reason != TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING
                    ) {
                        status.text = getString(R.string.input_unavailable)
                        handler.removeCallbacks(giveUp)
                        handler.postDelayed(giveUp, FAILURE_GRACE_MS)
                    }
                }
            },
        )
        tvView.setStreamVolume(1f)
        handler.postDelayed(giveUp, CONNECT_TIMEOUT_MS)
        runCatching { tvView.tune(inputId, TvContract.buildChannelUriForPassthroughInput(inputId)) }
            .onFailure {
                Log.w(TAG, "tune failed: ${it.message}")
                fallBack()
            }
    }

    private fun fallBack() {
        if (isFinishing) return
        handler.removeCallbacks(giveUp)
        Log.i(TAG, "Own viewer could not show the input; opening the system picker")
        LauncherGraph.inputs.openSystemPicker()
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(giveUp)
        if (::tvView.isInitialized) tvView.reset()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "InputViewer"
        private const val EXTRA_INPUT_ID = "input_id"
        private const val EXTRA_LABEL = "label"
        private const val CONNECT_TIMEOUT_MS = 6_000L
        private const val FAILURE_GRACE_MS = 3_000L

        fun intent(context: Context, inputId: String, label: String): Intent =
            Intent(context, InputViewerActivity::class.java)
                .putExtra(EXTRA_INPUT_ID, inputId)
                .putExtra(EXTRA_LABEL, label)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
