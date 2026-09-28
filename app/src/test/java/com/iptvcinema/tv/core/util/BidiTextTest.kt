package com.iptvcinema.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BidiTextTest {
    @Test
    fun wrapsTextInAFirstStrongIsolate() {
        assertEquals("⁨justice.⁩", "justice.".isolateDirection())
    }

    @Test
    fun emptyAndAlreadyIsolatedTextIsUnchanged() {
        assertEquals("", "".isolateDirection())
        val once = "S1E2".isolateDirection()
        assertEquals(once, once.isolateDirection())
    }
}
