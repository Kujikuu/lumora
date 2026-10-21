package com.iptvcinema.tv.features.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBrowsePerfRulesTest {
    @Test
    fun railEntry_staysOnTheCardFocusedLast() {
        assertEquals(3, resolveRailEntry(entryIndex = 3, lastIndex = 9, onScreen = listOf(2, 3, 4, 5)))
    }

    @Test
    fun railEntry_pastTheEndFallsBackToLastCard() {
        // A rail that shrank after a sync: the saved entry no longer exists.
        assertEquals(4, resolveRailEntry(entryIndex = 7, lastIndex = 4, onScreen = listOf(1, 2, 3, 4)))
    }

    @Test
    fun railEntry_offScreenMovesToFirstCardOnScreen() {
        // The list reordered under the remembered card: a requester on it would be detached.
        assertEquals(2, resolveRailEntry(entryIndex = 7, lastIndex = 9, onScreen = listOf(2, 3, 4, 1)))
    }

    @Test
    fun railEntry_keepsSavedCardBeforeTheRowHasLaidOut() {
        assertEquals(7, resolveRailEntry(entryIndex = 7, lastIndex = 9, onScreen = emptyList()))
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
