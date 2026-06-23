package com.iptvcinema.tv.core.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class M3uNormalizerTest {
    @Test
    fun normalizeLiveCatalog_createsCategoriesFromGroupTitle() {
        val entries = listOf(
            M3uEntry(
                name = "News One",
                url = "https://example.com/news.m3u8",
                tvgId = "news1",
                tvgName = null,
                logo = null,
                group = "News",
            ),
            M3uEntry(
                name = "Sports One",
                url = "https://example.com/sports.m3u8",
                tvgId = "sport1",
                tvgName = null,
                logo = null,
                group = "Sports",
            ),
        )

        val (categories, channels) = M3uNormalizer.normalizeLiveCatalog("source-1", entries)

        assertEquals(2, categories.size)
        assertEquals(listOf("News", "Sports"), categories.map { it.name })
        assertEquals(2, channels.size)
        assertEquals("News", channels[0].categoryName)
        assertEquals("Sports", channels[1].categoryName)
    }

    @Test
    fun channelId_isStableForSameUrl() {
        val entry = M3uEntry(
            name = "Channel",
            url = "https://example.com/live.m3u8",
            tvgId = "abc",
            tvgName = null,
            logo = null,
            group = "General",
        )

        assertEquals(M3uNormalizer.channelId(entry), M3uNormalizer.channelId(entry))
    }

    @Test
    fun channelId_differsForDifferentUrls() {
        val first = M3uEntry("A", "https://example.com/a.m3u8", null, null, null, "General")
        val second = M3uEntry("B", "https://example.com/b.m3u8", null, null, null, "General")

        assertNotEquals(M3uNormalizer.channelId(first), M3uNormalizer.channelId(second))
    }
}
