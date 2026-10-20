package com.iptvcinema.tv.core.di

import android.app.ActivityManager
import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        apiClient: Lazy<OkHttpClient>,
    ): ImageLoader {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryCacheSizeBytes = ImageLoaderTuning.memoryCacheBytes(activityManager.memoryClass)

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(memoryCacheSizeBytes)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            // Built on first image load, off the startup path.
            .okHttpClient { imageClient(apiClient.get()) }
            // A crossfade costs extra frames per card while a rail scrolls; posters just appear.
            .crossfade(false)
            .allowHardware(true)
            // Opaque JPEG posters decode at half the memory whenever a software bitmap is used.
            .allowRgb565(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
    }

    /**
     * Images get their own dispatcher and pool. Shared with the API client, OkHttp's default of
     * 5 requests per host queued posters behind each other and behind sync calls. Interceptors
     * (User-Agent) carry over. Shorter timeouts let dead IPTV image links fail fast and free
     * their slot instead of holding it for 30 s.
     */
    private fun imageClient(apiClient: OkHttpClient): OkHttpClient =
        apiClient.newBuilder()
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = ImageLoaderTuning.MAX_REQUESTS
                    maxRequestsPerHost = ImageLoaderTuning.MAX_REQUESTS_PER_HOST
                },
            )
            .connectionPool(
                ConnectionPool(ImageLoaderTuning.MAX_IDLE_CONNECTIONS, KEEP_ALIVE_MINUTES, TimeUnit.MINUTES),
            )
            .connectTimeout(IMAGE_CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(IMAGE_READ_TIMEOUT_S, TimeUnit.SECONDS)
            .build()

    // Posters for a full IPTV catalog add up fast; a bigger disk cache means fewer
    // pop-ins when scrolling back through rails already seen.
    private const val DISK_CACHE_BYTES = 250L * 1024L * 1024L
    private const val KEEP_ALIVE_MINUTES = 5L
    private const val IMAGE_CONNECT_TIMEOUT_S = 10L
    private const val IMAGE_READ_TIMEOUT_S = 20L
}

/** Image loading limits, kept apart from Android types so they can be unit tested. */
internal object ImageLoaderTuning {
    const val MAX_REQUESTS = 32
    const val MAX_REQUESTS_PER_HOST = 16
    // Matches the per-host limit, so a burst of posters does not reopen TLS connections.
    const val MAX_IDLE_CONNECTIONS = MAX_REQUESTS_PER_HOST

    // Shares the heap with the video buffer; kept modest for 128 MB TVs.
    const val MEMORY_CACHE_FRACTION = 0.18
    const val MIN_MEMORY_CACHE_BYTES = 16 * 1024 * 1024

    fun memoryCacheBytes(memoryClassMb: Int): Int =
        (memoryClassMb * 1024L * 1024L * MEMORY_CACHE_FRACTION).toInt()
            .coerceAtLeast(MIN_MEMORY_CACHE_BYTES)
}
