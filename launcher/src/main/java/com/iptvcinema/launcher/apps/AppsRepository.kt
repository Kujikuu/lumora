package com.iptvcinema.launcher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.iptvcinema.launcher.content.AppLinks
import com.iptvcinema.launcher.content.LauncherRowsContract
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** An app plus the file holding its rendered banner or icon (null while it renders). */
data class AppCard(val entry: AppEntry, val artwork: File?)

/** Installed apps for the Apps row, refreshed when packages come and go. */
class AppsRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val packageManager = context.packageManager
    private val iconCache = AppIconCache(context)
    private val scanLock = Mutex()
    private val _apps = MutableStateFlow<List<AppCard>?>(null)
    /** Null until the first scan finished. */
    val apps: StateFlow<List<AppCard>?> = _apps.asStateFlow()
    private var receiver: BroadcastReceiver? = null

    fun start() {
        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    rescan()
                }
            }.also {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_REPLACED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addDataScheme("package")
                }
                ContextCompat.registerReceiver(context, it, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            }
        }
        rescan()
    }

    fun rescan() {
        scope.launch(Dispatchers.IO) {
            scanLock.withLock {
                runCatching { scan() }
                    .onSuccess { _apps.value = it }
                    .onFailure { Log.w(TAG, "App scan failed: ${it.message}") }
            }
        }
    }

    private fun scan(): List<AppCard> {
        val leanback = resolve(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val touch = resolve(Intent.CATEGORY_LAUNCHER)
        val byPackage = LinkedHashMap<String, Pair<AppEntry, ResolveInfo>>()
        (leanback.map { it to true } + touch.map { it to false }).forEach { (info, isLeanback) ->
            val packageName = info.activityInfo.packageName
            if (packageName in byPackage) return@forEach
            val applicationInfo = info.activityInfo.applicationInfo
            val entry = AppEntry(
                packageName = packageName,
                label = info.loadLabel(packageManager)?.toString().orEmpty().ifBlank { packageName },
                isLeanback = isLeanback,
                isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                lastUpdateTimeMs = lastUpdateTime(packageName),
            )
            byPackage[packageName] = entry to info
        }
        val visible = AppListSorter.hidden(byPackage.values.map { it.first }, context.packageName, homePackages())
        val sorted = AppListSorter.sort(visible, Locale.getDefault(), pinnedFirst = LauncherRowsContract.APP_PACKAGE)
        iconCache.sweep(sorted)
        return sorted.map { entry -> AppCard(entry, iconCache.ensure(entry, byPackage.getValue(entry.packageName).second)) }
    }

    private fun resolve(category: String): List<ResolveInfo> =
        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)

    private fun homePackages(): Set<String> =
        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()

    private fun lastUpdateTime(packageName: String): Long =
        runCatching { packageManager.getPackageInfo(packageName, 0).lastUpdateTime }.getOrDefault(0L)

    fun open(packageName: String) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        AppLinks.start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openInfo(packageName: String) {
        AppLinks.start(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun uninstall(packageName: String) {
        AppLinks.start(
            context,
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private companion object {
        const val TAG = "LauncherApps"
    }
}
