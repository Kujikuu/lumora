package com.iptvcinema.tv.core.parental

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingPolicyTest {
    @Test
    fun isAllowed_respectsMaxRatingLadder() {
        assertTrue(RatingPolicy.isAllowed("G", "12+"))
        assertTrue(RatingPolicy.isAllowed("PG", "12+"))
        assertTrue(RatingPolicy.isAllowed("PG-13", "12+"))
        assertFalse(RatingPolicy.isAllowed("18+", "12+"))
        assertFalse(RatingPolicy.isAllowed("R", "PG"))
    }

    @Test
    fun isAllowed_normalizesAliases() {
        assertTrue(RatingPolicy.isAllowed("TV-MA", "18+"))
        assertFalse(RatingPolicy.isAllowed("TV-MA", "PG-13"))
    }

    @Test
    fun isAllowed_allowsUnratedContent() {
        assertTrue(RatingPolicy.isAllowed(null, "PG"))
        assertTrue(RatingPolicy.isAllowed("", "PG"))
    }
}
