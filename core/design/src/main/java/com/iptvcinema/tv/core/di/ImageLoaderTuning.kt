package com.iptvcinema.tv.core.di

/** Image loading limits, kept apart from Android types so they can be unit tested. */
object ImageLoaderTuning {
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
