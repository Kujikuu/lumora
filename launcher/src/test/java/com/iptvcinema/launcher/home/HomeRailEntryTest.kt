package com.iptvcinema.launcher.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRailEntryTest {
    @Test
    fun entry_staysOnTheLastFocusedCardWhenItIsOnScreen() {
        assertEquals(3, resolveRailEntry(entryIndex = 3, lastIndex = 9, onScreen = listOf(2, 3, 4)))
    }

    @Test
    fun entry_clampsWhenTheRailShrank() {
        assertEquals(1, resolveRailEntry(entryIndex = 7, lastIndex = 1, onScreen = listOf(0, 1)))
    }

    @Test
    fun entry_movesToTheFirstCardOnScreenWhenTheOldOneScrolledAway() {
        assertEquals(5, resolveRailEntry(entryIndex = 0, lastIndex = 9, onScreen = listOf(5, 6, 7)))
    }

    @Test
    fun entry_keepsTheIndexBeforeTheFirstLayout() {
        assertEquals(2, resolveRailEntry(entryIndex = 2, lastIndex = 9, onScreen = emptyList()))
    }
}
