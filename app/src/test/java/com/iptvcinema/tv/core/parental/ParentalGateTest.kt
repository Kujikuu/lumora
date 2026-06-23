package com.iptvcinema.tv.core.parental

import com.iptvcinema.tv.core.model.ParentalControls
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalGateTest {
    private val pinHasher = PinHasher()
    private val session = ParentalSession()
    private val gate = ParentalGate(pinHasher, session)

    private fun controls(
        hideAdult: Boolean = true,
        blocked: List<String> = listOf("Sports"),
        pinHash: String? = null,
        maxRating: String = "12+",
    ) = ParentalControls(
        id = "1",
        userId = "u1",
        profileId = "p1",
        pinHash = pinHash,
        hideAdultCategories = hideAdult,
        lockPlaylistSettings = false,
        lockLiveCategories = false,
        maxRating = maxRating,
        blockedCategories = blocked,
    )

    @Test
    fun filterCategoryNames_hidesAdultAndBlocked() {
        val filtered = gate.filterCategoryNames(
            listOf("Movies", "Adult", "Sports", "Kids"),
            controls(),
        )
        assertTrue(filtered.contains("Movies"))
        assertTrue(filtered.contains("Kids"))
        assertFalse(filtered.contains("Sports"))
        assertFalse(filtered.any { it.equals("Adult", ignoreCase = true) })
    }

    @Test
    fun verifyPin_marksSessionVerified() {
        val hash = pinHasher.hashPin("1234")
        val ctrl = controls(pinHash = hash)
        assertTrue(gate.verifyPin(ctrl, "1234"))
        assertTrue(gate.isPinVerified("p1"))
        assertFalse(gate.verifyPin(ctrl, "9999"))
    }

    @Test
    fun isContentBlocked_respectsCategoryAndRating() {
        val ctrl = controls(blocked = listOf("Sports"), hideAdult = false)
        assertTrue(gate.isContentBlocked("Sports", "G", ctrl))
        assertFalse(gate.isContentBlocked("Kids", "G", ctrl))
        assertTrue(gate.isContentBlocked("Kids", "18+", controls(maxRating = "PG")))
    }
}
