package com.iptvcinema.tv.core.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uLiveTvPipelineTest {
    @Test
    fun parseAndNormalize_producesBrowseReadyCatalog() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="bbc.one" tvg-logo="https://logo.example/bbc.png" group-title="UK",BBC One
            https://stream.example.com/bbc-one.m3u8
            #EXTINF:-1 tvg-id="cnn.int" group-title="News",CNN International
            https://stream.example.com/cnn.m3u8
            #EXTINF:-1 group-title="News",Sky News
            https://stream.example.com/sky-news.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(playlist)
        val (categories, channels) = M3uNormalizer.normalizeLiveCatalog("m3u-source-1", entries)

        assertEquals(2, categories.map { it.name }.toSet().size)
        assertTrue(categories.any { it.name == "UK" })
        assertTrue(categories.any { it.name == "News" })
        assertEquals(3, channels.size)
        assertEquals("BBC One", channels[0].name)
        assertEquals("https://stream.example.com/bbc-one.m3u8", channels[0].streamUrl)
        assertEquals("bbc.one", channels[0].tvgId)
        assertEquals("UK", channels[0].categoryName)
        assertEquals(2, channels.count { it.categoryName == "News" })
    }
}
