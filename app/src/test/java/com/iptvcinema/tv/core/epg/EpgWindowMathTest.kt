package com.iptvcinema.tv.core.epg

import org.junit.Assert.assertEquals
import org.junit.Test

class EpgWindowMathTest {
    @Test
    fun defaultWindowSpansPastAndFuture() {
        val now = 10_000L
        val start = GuideLayoutHelper.defaultWindowStart(now)
        val end = GuideLayoutHelper.defaultWindowEnd(now)

        assertEquals(now - GuideLayoutHelper.DEFAULT_WINDOW_PAST_MS, start)
        assertEquals(now + GuideLayoutHelper.DEFAULT_WINDOW_FUTURE_MS, end)
    }

    @Test
    fun windowEndFromStart_isConsistentWithDefaults() {
        val now = 10_000L
        val start = GuideLayoutHelper.defaultWindowStart(now)
        val end = GuideLayoutHelper.windowEndFromStart(start)

        assertEquals(
            GuideLayoutHelper.DEFAULT_WINDOW_PAST_MS + GuideLayoutHelper.DEFAULT_WINDOW_FUTURE_MS,
            end - start,
        )
    }

    @Test
    fun trimPastConstant_isTwentyFourHours() {
        assertEquals(24L * 60 * 60 * 1000, EpgSyncRepository.TRIM_PAST_MS)
    }
}
