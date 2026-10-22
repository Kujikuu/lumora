package com.iptvcinema.tv.core.tvhome

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeChannelRulesTest {
    @Test
    fun interleave_alternatesAndKeepsTheRemainder() {
        assertEquals(
            listOf("m1", "s1", "m2", "s2", "m3", "m4"),
            HomeChannelRules.interleave(listOf("m1", "m2", "m3", "m4"), listOf("s1", "s2")),
        )
        assertEquals(listOf("s1"), HomeChannelRules.interleave(emptyList(), listOf("s1")))
    }
}
