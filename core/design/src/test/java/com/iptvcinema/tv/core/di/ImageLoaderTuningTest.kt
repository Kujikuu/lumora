package com.iptvcinema.tv.core.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageLoaderTuningTest {
    @Test
    fun memoryCache_isAFractionOfTheHeap() {
        val bytes = ImageLoaderTuning.memoryCacheBytes(memoryClassMb = 128)
        val heap = 128L * 1024 * 1024
        assertEquals((heap * ImageLoaderTuning.MEMORY_CACHE_FRACTION).toInt(), bytes)
        assertTrue("cache must stay within 15-20% of a 128 MB heap", bytes.toLong() in heap * 15 / 100..heap * 20 / 100)
    }

    @Test
    fun memoryCache_neverDropsBelowTheFloor() {
        assertEquals(ImageLoaderTuning.MIN_MEMORY_CACHE_BYTES, ImageLoaderTuning.memoryCacheBytes(memoryClassMb = 32))
    }

    @Test
    fun dispatcher_allowsParallelPostersPerHost() {
        // OkHttp's default of 5 per host queued posters one behind another.
        assertTrue(ImageLoaderTuning.MAX_REQUESTS_PER_HOST > 5)
        assertTrue(ImageLoaderTuning.MAX_REQUESTS >= ImageLoaderTuning.MAX_REQUESTS_PER_HOST)
        assertTrue(ImageLoaderTuning.MAX_IDLE_CONNECTIONS >= ImageLoaderTuning.MAX_REQUESTS_PER_HOST)
    }
}
