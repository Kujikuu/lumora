package com.iptvcinema.launcher

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.iptvcinema.launcher.apps.AppsRepository
import com.iptvcinema.launcher.content.ContentRowsRepository
import com.iptvcinema.launcher.inputs.InputsRepository
import com.iptvcinema.launcher.system.ClockTicker
import com.iptvcinema.launcher.system.NetworkStateMonitor
import com.iptvcinema.tv.core.di.ImageLoaderTuning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The launcher's handful of singletons, built on first use. A DI framework would cost start-up
 * time on a weak box for six objects; this is the whole graph.
 */
object LauncherGraph {
    private lateinit var app: Application

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val contentRows: ContentRowsRepository by lazy { ContentRowsRepository(app, scope) }
    val apps: AppsRepository by lazy { AppsRepository(app, scope) }
    val inputs: InputsRepository by lazy { InputsRepository(app, scope) }
    val clock: ClockTicker by lazy { ClockTicker(app) }
    val network: NetworkStateMonitor by lazy { NetworkStateMonitor(app) }
    val imageLoader: ImageLoader by lazy { buildImageLoader(app) }

    /** Fired when Home is pressed while the launcher is already showing. */
    val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Volatile
    private var started = false

    fun init(application: Application) {
        app = application
    }

    /** Starts the data sources; called once the first frame is on screen. */
    fun startAfterFirstFrame() {
        if (started) return
        started = true
        contentRows.start()
        apps.start()
        inputs.start()
    }

    fun dropImageMemory() {
        imageLoader.memoryCache?.clear()
    }

    private fun buildImageLoader(context: Context): ImageLoader {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryBytes = ImageLoaderTuning.memoryCacheBytes(activityManager.memoryClass).coerceAtMost(MAX_MEMORY_CACHE_BYTES)
        return ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder(context).maxSizeBytes(memoryBytes).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(false)
            .allowHardware(true)
            .allowRgb565(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
    }

    // The launcher shows about 60 small cards; a bigger cache only costs the box memory.
    private const val MAX_MEMORY_CACHE_BYTES = 12 * 1024 * 1024
    private const val DISK_CACHE_BYTES = 64L * 1024L * 1024L
}
