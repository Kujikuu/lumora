package com.iptvcinema.launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIconCacheKeysTest {
    @Test
    fun fileName_changesWithTheUpdateTime() {
        assertEquals("com.a-10.png", AppIconCacheKeys.fileName("com.a", 10))
        assertEquals("com.a-11.png", AppIconCacheKeys.fileName("com.a", 11))
    }

    @Test
    fun staleFiles_areOldVersionsAndRemovedApps() {
        val stale = AppIconCacheKeys.staleFiles(
            existing = listOf("com.a-10.png", "com.a-11.png", "com.gone-1.png", "notes.txt"),
            wanted = setOf("com.a-11.png"),
        )
        assertEquals(listOf("com.a-10.png", "com.gone-1.png"), stale)
    }
}
