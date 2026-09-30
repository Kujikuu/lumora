package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.player.ChannelDirection
import com.iptvcinema.tv.features.home.channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdjacentChannelTest {
    private val channels = listOf(
        channel("1", category = "News"),
        channel("2", category = "Adult"),
        channel("3", category = "Adult"),
        channel("4", category = "Sports"),
    )
    private val notAdult: (com.iptvcinema.tv.core.model.catalog.CatalogChannel) -> Boolean = { it.categoryName != "Adult" }

    @Test
    fun `zapping skips blocked channels in both directions`() {
        assertEquals("4", adjacentChannel(channels, "1", ChannelDirection.NEXT, notAdult)?.id)
        assertEquals("1", adjacentChannel(channels, "4", ChannelDirection.PREVIOUS, notAdult)?.id)
    }

    @Test
    fun `without a filter it moves one channel`() {
        assertEquals("2", adjacentChannel(channels, "1", ChannelDirection.NEXT) { true }?.id)
    }

    @Test
    fun `the ends of the list stop, including when only blocked channels remain`() {
        assertNull(adjacentChannel(channels, "4", ChannelDirection.NEXT) { true })
        assertNull(adjacentChannel(channels.take(3), "1", ChannelDirection.NEXT, notAdult))
    }

    @Test
    fun `a missing current channel starts from the first allowed one`() {
        assertEquals("1", adjacentChannel(channels, "gone", ChannelDirection.NEXT, notAdult)?.id)
    }
}
