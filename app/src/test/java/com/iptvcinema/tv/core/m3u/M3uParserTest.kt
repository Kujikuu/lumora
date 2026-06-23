package com.iptvcinema.tv.core.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parse_standardExtInf_extractsAttributesAndUrl() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news.us" tvg-name="News Channel" tvg-logo="https://logo.png" group-title="News",News Channel
            https://stream.example.com/live/news.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("News Channel", entries[0].name)
        assertEquals("https://stream.example.com/live/news.m3u8", entries[0].url)
        assertEquals("news.us", entries[0].tvgId)
        assertEquals("News Channel", entries[0].tvgName)
        assertEquals("https://logo.png", entries[0].logo)
        assertEquals("News", entries[0].group)
    }

    @Test
    fun parse_missingLogo_usesDisplayName() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="Sports",ESPN
            http://example.com/espn.ts
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("ESPN", entries[0].name)
        assertEquals(null, entries[0].logo)
        assertEquals("Sports", entries[0].group)
    }

    @Test
    fun parse_unicodeName_parsesCorrectly() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="International",日本チャンネル
            https://example.com/jp.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("日本チャンネル", entries[0].name)
    }

    @Test
    fun parse_badRows_skipsInvalidEntries() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Valid One
            https://example.com/valid.m3u8
            not-a-url
            #EXTINF:-1,No Url Yet
            #EXTINF:-1,Valid Two
            http://example.com/two.ts
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(2, entries.size)
        assertEquals("Valid One", entries[0].name)
        assertEquals("Valid Two", entries[1].name)
    }

    @Test
    fun parse_bomAndExtVlcOpt_toleratesInput() {
        val playlist = """
            ﻿#EXTM3U
            #EXTINF:-1 tvg-id="ch1" group-title="General",Channel One
            #EXTVLCOPT:http-user-agent=CustomAgent/1.0
            https://example.com/one.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("ch1", entries[0].tvgId)
        assertEquals("CustomAgent/1.0", entries[0].attributes["http-user-agent"])
    }

    @Test
    fun parse_emptyPlaylist_returnsEmptyList() {
        assertTrue(M3uParser.parse("#EXTM3U\n").isEmpty())
    }
}
