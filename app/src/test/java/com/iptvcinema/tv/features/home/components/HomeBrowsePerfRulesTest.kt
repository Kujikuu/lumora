package com.iptvcinema.tv.features.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBrowsePerfRulesTest {
    @Test
    fun isRailEntry_onlyTheEntryCardHoldsTheRequester() {
        assertTrue(isRailEntry(index = 3, entryIndex = 3, lastIndex = 9))
        assertFalse(isRailEntry(index = 2, entryIndex = 3, lastIndex = 9))
        assertFalse(isRailEntry(index = 4, entryIndex = 3, lastIndex = 9))
    }

    @Test
    fun isRailEntry_entryPastTheEndFallsBackToLastCard() {
        // A rail that shrank after a sync: the saved entry no longer exists.
        assertTrue(isRailEntry(index = 4, entryIndex = 7, lastIndex = 4))
        assertFalse(isRailEntry(index = 3, entryIndex = 7, lastIndex = 4))
    }

    @Test
    fun spotlightSettle_cardWaitsForFocusToRest() {
        assertEquals(SPOTLIGHT_SETTLE_MS, spotlightSettleDelayMs(hasShown = true, targetIsCard = true))
    }

    @Test
    fun spotlightSettle_heroAndFirstSpotlightShowAtOnce() {
        assertEquals(0L, spotlightSettleDelayMs(hasShown = true, targetIsCard = false))
        assertEquals(0L, spotlightSettleDelayMs(hasShown = false, targetIsCard = true))
        assertEquals(0L, spotlightSettleDelayMs(hasShown = false, targetIsCard = false))
    }
}
