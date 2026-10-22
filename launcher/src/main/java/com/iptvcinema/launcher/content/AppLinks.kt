package com.iptvcinema.launcher.content

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/** Opens Lumora Play: a card's play link, the app itself, or its store page when it is missing. */
object AppLinks {
    fun openPlayLink(context: Context, intentUri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUri))
            .setPackage(LauncherRowsContract.APP_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, intent)) openApp(context)
    }

    fun openApp(context: Context) {
        val packageManager = context.packageManager
        val launch = packageManager.getLeanbackLaunchIntentForPackage(LauncherRowsContract.APP_PACKAGE)
            ?: packageManager.getLaunchIntentForPackage(LauncherRowsContract.APP_PACKAGE)
        if (launch == null || !start(context, launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) openStorePage(context)
    }

    fun openStorePage(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${LauncherRowsContract.APP_PACKAGE}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, market)) {
            start(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${LauncherRowsContract.APP_PACKAGE}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (error: ActivityNotFoundException) {
        Log.w(TAG, "No activity for ${intent.action} ${intent.dataString}")
        false
    } catch (error: SecurityException) {
        Log.w(TAG, "Not allowed to start ${intent.action}: ${error.message}")
        false
    }

    private const val TAG = "AppLinks"
}
