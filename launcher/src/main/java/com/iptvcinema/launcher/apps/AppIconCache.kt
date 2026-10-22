package com.iptvcinema.launcher.apps

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import java.io.File

/**
 * Banners and icons rendered once to PNG files, so a later Home press loads a small file through
 * Coil's cache instead of inflating every app's drawable again. The file name carries the
 * package's last update time, so an updated app gets a fresh icon and the old file is swept.
 */
class AppIconCache(context: Context) {
    private val directory = File(context.filesDir, DIRECTORY)
    private val packageManager = context.packageManager

    fun fileFor(entry: AppEntry): File = File(directory, AppIconCacheKeys.fileName(entry.packageName, entry.lastUpdateTimeMs))

    /** Renders the file for [entry] if it is missing; returns it, or null when nothing could be drawn. */
    fun ensure(entry: AppEntry, resolveInfo: ResolveInfo): File? {
        val file = fileFor(entry)
        if (file.isFile) return file
        directory.mkdirs()
        val drawable = loadArtwork(resolveInfo) ?: return null
        return runCatching {
            val bitmap = Bitmap.createBitmap(WIDTH_PX, HEIGHT_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(BACKGROUND)
            drawable.draw(canvas, bannerLike = resolveInfo.isBanner)
            val temp = File(directory, "${file.name}.tmp")
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            if (temp.renameTo(file)) file else temp
        }.onFailure { Log.w(TAG, "Icon render failed for ${entry.packageName}: ${it.message}") }.getOrNull()
    }

    /** Deletes files that belong to no current app or to an older version of one. */
    fun sweep(current: List<AppEntry>) {
        val wanted = current.map { fileFor(it).name }.toSet()
        AppIconCacheKeys.staleFiles(directory.list().orEmpty().toList(), wanted)
            .forEach { File(directory, it).delete() }
    }

    private val ResolveInfo.isBanner: Boolean
        get() = activityInfo?.banner != 0 || activityInfo?.applicationInfo?.banner != 0

    private fun loadArtwork(info: ResolveInfo): Drawable? = runCatching {
        val activity = info.activityInfo ?: return null
        activity.loadBanner(packageManager) ?: activity.loadIcon(packageManager)
    }.getOrNull()

    /** A banner fills the frame; an icon sits centred on the dark background. */
    private fun Drawable.draw(canvas: Canvas, bannerLike: Boolean) {
        if (bannerLike) {
            setBounds(0, 0, WIDTH_PX, HEIGHT_PX)
        } else {
            val left = (WIDTH_PX - ICON_PX) / 2
            val top = (HEIGHT_PX - ICON_PX) / 2
            setBounds(left, top, left + ICON_PX, top + ICON_PX)
        }
        draw(canvas)
    }

    private companion object {
        const val TAG = "AppIconCache"
        const val DIRECTORY = "app_icons"
        // 2x the 160x90 dp card on this density; enough for a crisp banner, small to decode.
        const val WIDTH_PX = 320
        const val HEIGHT_PX = 180
        const val ICON_PX = 108
        val BACKGROUND = Color.parseColor("#121620")
    }
}

/** File naming for the icon cache, pure so it is unit tested. */
object AppIconCacheKeys {
    fun fileName(packageName: String, lastUpdateTimeMs: Long): String = "$packageName-$lastUpdateTimeMs.png"

    fun staleFiles(existing: List<String>, wanted: Set<String>): List<String> =
        existing.filter { it.endsWith(".png") && it !in wanted }
}
