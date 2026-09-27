package com.iptvcinema.tv.core.di

import android.app.ActivityManager
import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClassMb = activityManager.memoryClass
        val memoryCacheSizeBytes = (memoryClassMb * 1024L * 1024L * MEMORY_CACHE_FRACTION).toInt()

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(memoryCacheSizeBytes.coerceAtLeast(16 * 1024 * 1024))
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .okHttpClient(okHttpClient)
            .crossfade(CROSSFADE_MS)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
    }

    // Posters for a full IPTV catalog add up fast; a bigger disk cache means fewer
    // pop-ins when scrolling back through rails already seen.
    private const val DISK_CACHE_BYTES = 250L * 1024L * 1024L
    private const val MEMORY_CACHE_FRACTION = 0.25
    private const val CROSSFADE_MS = 180
}
