package com.iptvcinema.tv.features.livetv

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelNumberEntryTest {
    @Test
    fun append_collectsDigitsUpToTheLimit() {
        val entry = ChannelNumberEntry().append('1').append('2').append('3').append('4').append('5')
        assertEquals("1234", entry.digits)
        assertTrue(entry.isComplete)
        assertEquals(1234, entry.number)
    }

    @Test
    fun append_ignoresNonDigits() {
        val entry = ChannelNumberEntry().append('a').append('7')
        assertEquals("7", entry.digits)
        assertFalse(entry.isComplete)
    }

    @Test
    fun digitFor_mapsRemoteAndNumPadKeys() {
        assertEquals('0', ChannelNumberEntry.digitFor(Key.Zero))
        assertEquals('9', ChannelNumberEntry.digitFor(Key.NumPad9))
        assertNull(ChannelNumberEntry.digitFor(Key.DirectionUp))
    }

    @Test
    fun resolve_prefersPlaylistChannelNumbers() {
        val channels = listOf("a" to 101, "b" to 5, "c" to null)
        val match = ChannelNumberResolver.resolve(channels, 5) { it.second }
        assertEquals("b", match?.first)
        // A numbered playlist never falls back to list position.
        assertNull(ChannelNumberResolver.resolve(channels, 2) { it.second })
    }

    @Test
    fun resolve_fallsBackToPositionInTheList() {
        val channels = listOf("a" to null, "b" to null, "c" to null)
        assertEquals("c", ChannelNumberResolver.resolve(channels, 3) { it.second }?.first)
        assertNull(ChannelNumberResolver.resolve(channels, 4) { it.second })
        assertNull(ChannelNumberResolver.resolve(channels, 0) { it.second })
    }
}
