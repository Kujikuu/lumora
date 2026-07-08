package com.iptvcinema.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingFormatterTest {
    @Test
    fun `formatForDisplay rounds numeric ratings to one decimal`() {
        assertEquals("8.2", RatingFormatter.formatForDisplay("8.234"))
        assertEquals("8.3", RatingFormatter.formatForDisplay("8.25"))
        assertEquals("7.0", RatingFormatter.formatForDisplay("7"))
    }

    @Test
    fun `formatForDisplay preserves content ratings`() {
        assertEquals("PG-13", RatingFormatter.formatForDisplay("PG-13"))
        assertEquals("18+", RatingFormatter.formatForDisplay("18+"))
        assertEquals("TV-MA", RatingFormatter.formatForDisplay("TV-MA"))
    }

    @Test
    fun `formatForDisplay returns null for blank`() {
        assertNull(RatingFormatter.formatForDisplay(null))
        assertNull(RatingFormatter.formatForDisplay(""))
        assertNull(RatingFormatter.formatForDisplay("   "))
    }
}
