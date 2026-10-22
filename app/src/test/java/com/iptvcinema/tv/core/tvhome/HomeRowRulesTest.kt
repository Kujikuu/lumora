package com.iptvcinema.tv.core.tvhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRowRulesTest {
    @Test
    fun progress_isTheWatchedFraction() {
        assertEquals(0.25f, HomeRowRules.progressOf(positionMs = 15_000, durationMs = 60_000))
    }

    @Test
    fun progress_isNullWithoutADurationOrPosition() {
        assertNull(HomeRowRules.progressOf(positionMs = 15_000, durationMs = null))
        assertNull(HomeRowRules.progressOf(positionMs = 15_000, durationMs = 0))
        assertNull(HomeRowRules.progressOf(positionMs = 0, durationMs = 60_000))
    }

    @Test
    fun progress_neverExceedsOne() {
        assertEquals(1f, HomeRowRules.progressOf(positionMs = 90_000, durationMs = 60_000))
    }

    @Test
    fun liveNow_putsFavouritesFirstThenRecentWithoutRepeats() {
        assertEquals(
            listOf("f1", "f2", "r1"),
            HomeRowRules.liveNowChannelIds(listOf("f1", "f2"), listOf("f2", "r1", "f1"), limit = 12),
        )
    }

    @Test
    fun liveNow_respectsTheLimitAndDropsBlankIds() {
        assertEquals(
            listOf("f1", "r1"),
            HomeRowRules.liveNowChannelIds(listOf("f1", ""), listOf("r1", "r2"), limit = 2),
        )
    }
}
