package com.iptvcinema.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTuningTest {
    private val mb = 1024L * 1024L

    @Test
    fun targetBufferBytes_isFifthOfHeap_onLowMemoryTv() {
        // The Haier test TV: 128 MB heap limit.
        assertEquals((128 * mb / 5).toInt(), PlaybackTuning.targetBufferBytes(128 * mb))
    }

    @Test
    fun targetBufferBytes_neverDropsBelowFloor() {
        assertEquals(PlaybackTuning.MIN_TARGET_BUFFER_BYTES, PlaybackTuning.targetBufferBytes(32 * mb))
    }

    @Test
    fun targetBufferBytes_isCapped_onLargeHeaps() {
        assertEquals(PlaybackTuning.MAX_TARGET_BUFFER_BYTES, PlaybackTuning.targetBufferBytes(1024 * mb))
    }
}
