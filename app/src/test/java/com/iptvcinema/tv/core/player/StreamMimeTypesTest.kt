package com.iptvcinema.tv.core.player

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamMimeTypesTest {
    @Test
    fun `m3u8 url maps to hls`() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamMimeTypes.forUrl("http://host:8080/live/u/p/101.m3u8", isLive = true, isXtreamSource = true),
        )
    }

    @Test
    fun `ts url maps to mpeg ts`() {
        assertEquals(
            MimeTypes.VIDEO_MP2T,
            StreamMimeTypes.forUrl("http://host/live/u/p/101.ts", isLive = true, isXtreamSource = false),
        )
    }

    @Test
    fun `mp4 url maps to mp4`() {
        assertEquals(
            MimeTypes.VIDEO_MP4,
            StreamMimeTypes.forUrl("http://host/movie/u/p/55.mp4", isLive = false, isXtreamSource = true),
        )
    }

    @Test
    fun `mkv url maps to matroska`() {
        assertEquals(
            MimeTypes.VIDEO_MATROSKA,
            StreamMimeTypes.forUrl("http://host/series/u/p/77.MKV", isLive = false, isXtreamSource = true),
        )
    }

    @Test
    fun `query string and fragment are ignored`() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamMimeTypes.forUrl("https://cdn.example.com/a/index.m3u8?token=x.ts#t=10", isLive = true, isXtreamSource = false),
        )
    }

    @Test
    fun `extension only in the query is not trusted`() {
        assertNull(StreamMimeTypes.forUrl("http://host/play.php?file=a.m3u8", isLive = true, isXtreamSource = false))
    }

    @Test
    fun `xtream live without extension is mpeg ts`() {
        assertEquals(
            MimeTypes.VIDEO_MP2T,
            StreamMimeTypes.forUrl("http://host:8080/u/p/101", isLive = true, isXtreamSource = true),
        )
    }

    @Test
    fun `non xtream live without extension stays unknown`() {
        assertNull(StreamMimeTypes.forUrl("http://host:8080/u/p/101", isLive = true, isXtreamSource = false))
    }

    @Test
    fun `xtream vod without extension stays unknown`() {
        assertNull(StreamMimeTypes.forUrl("http://host:8080/u/p/101", isLive = false, isXtreamSource = true))
    }

    @Test
    fun `dot in host is not an extension`() {
        assertNull(StreamMimeTypes.forUrl("http://stream.example.ts", isLive = false, isXtreamSource = false))
    }

    @Test
    fun `unknown extension stays unknown`() {
        assertNull(StreamMimeTypes.forUrl("http://host/stream.php", isLive = true, isXtreamSource = true))
    }

    @Test
    fun `blank url stays unknown`() {
        assertNull(StreamMimeTypes.forUrl("", isLive = true, isXtreamSource = true))
    }
}
