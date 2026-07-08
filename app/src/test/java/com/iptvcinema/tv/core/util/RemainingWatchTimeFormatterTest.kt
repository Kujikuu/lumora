package com.iptvcinema.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemainingWatchTimeFormatterTest {
    @Test
    fun `partsFromRemainingMs rounds up to whole minutes`() {
        val parts = RemainingWatchTimeFormatter.partsFromRemainingMs(39 * 60_000L + 1_000L)
        assertEquals(RemainingWatchTimeFormatter.Parts(hours = 0, minutes = 40), parts)
    }

    @Test
    fun `partsFromRemainingMs formats hours and minutes`() {
        val parts = RemainingWatchTimeFormatter.partsFromRemainingMs((2 * 60 + 14) * 60_000L)
        assertEquals(RemainingWatchTimeFormatter.Parts(hours = 2, minutes = 14), parts)
    }

    @Test
    fun `partsFromRemainingMs returns null for zero or negative`() {
        assertNull(RemainingWatchTimeFormatter.partsFromRemainingMs(0L))
        assertNull(RemainingWatchTimeFormatter.partsFromRemainingMs(-1L))
    }
}
