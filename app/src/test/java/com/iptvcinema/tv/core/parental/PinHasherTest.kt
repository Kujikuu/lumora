package com.iptvcinema.tv.core.parental

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    private val hasher = PinHasher()

    @Test
    fun hashAndVerify_roundTrip() {
        val hash = hasher.hashPin("4321")
        assertTrue(hasher.verifyPin("4321", hash))
        assertFalse(hasher.verifyPin("1234", hash))
    }
}
