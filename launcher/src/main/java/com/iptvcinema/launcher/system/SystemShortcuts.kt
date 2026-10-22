package com.iptvcinema.launcher.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.iptvcinema.launcher.content.AppLinks

/** Header shortcuts into the system settings; each falls back to the main Settings screen. */
object SystemShortcuts {
    fun openSettings(context: Context) = open(context, Settings.ACTION_SETTINGS)

    fun openNetwork(context: Context) = open(context, Settings.ACTION_WIFI_SETTINGS)

    fun openManageApps(context: Context) = open(context, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)

    private fun open(context: Context, action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null || !AppLinks.start(context, intent)) {
            if (action != Settings.ACTION_SETTINGS) open(context, Settings.ACTION_SETTINGS)
        }
    }
}
