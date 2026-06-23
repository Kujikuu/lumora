package com.iptvcinema.tv.core.epg

import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {
    private val sourceId = "source-1"

    @Test
    fun buildChannelLookup_matchesTvgIdAndNormalizedName() {
        val channels = listOf(
            LocalChannelEntity(
                id = "101",
                sourceId = sourceId,
                name = "News HD",
                streamUrl = "http://example.com/stream",
                logoUrl = null,
                categoryId = "cat1",
                categoryName = "News",
                tvgId = "news.hd",
                channelNumber = 1,
            ),
        )

        val lookup = XmltvParser.buildChannelLookup(channels)

        assertEquals("101", lookup["news.hd"])
        assertEquals("101", lookup["101"])
        assertEquals("101", lookup["newshd"])
    }

    @Test
    fun isProgramInIngestWindow_filtersOutsidePastAndFutureBounds() {
        val nowMs = 1_000_000L
        val pastCutoff = nowMs - 24L * 60 * 60 * 1000
        val futureCutoff = nowMs + 48L * 60 * 60 * 1000

        assertTrue(
            XmltvParser.isProgramInIngestWindow(
                startMs = nowMs - 60_000,
                endMs = nowMs + 60_000,
                nowMs = nowMs,
            ),
        )
        assertFalse(
            XmltvParser.isProgramInIngestWindow(
                startMs = pastCutoff - 60_000,
                endMs = pastCutoff - 1_000,
                nowMs = nowMs,
            ),
        )
        assertFalse(
            XmltvParser.isProgramInIngestWindow(
                startMs = futureCutoff + 1_000,
                endMs = futureCutoff + 60_000,
                nowMs = nowMs,
            ),
        )
    }
}
