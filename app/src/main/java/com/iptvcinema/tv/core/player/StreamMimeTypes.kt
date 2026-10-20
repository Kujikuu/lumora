package com.iptvcinema.tv.core.player

import androidx.media3.common.MimeTypes
import java.util.Locale

/**
 * Container type of a stream, read from its URL, so ExoPlayer is told the format up front
 * instead of working it out itself. Null when the URL does not say; ExoPlayer then decides.
 */
object StreamMimeTypes {
    fun forUrl(url: String, isLive: Boolean, isXtreamSource: Boolean): String? {
        if (url.isBlank()) return null
        return when (extensionOf(url)) {
            "m3u8" -> MimeTypes.APPLICATION_M3U8
            "ts" -> MimeTypes.VIDEO_MP2T
            "mp4" -> MimeTypes.VIDEO_MP4
            "mkv" -> MimeTypes.VIDEO_MATROSKA
            // Xtream serves live channels as raw MPEG-TS when the URL has no extension.
            null -> MimeTypes.VIDEO_MP2T.takeIf { isLive && isXtreamSource }
            else -> null
        }
    }

    /** Extension of the last path segment only; query strings and host names never count. */
    private fun extensionOf(url: String): String? {
        val withoutQuery = url.substringBefore('#').substringBefore('?')
        val afterScheme = withoutQuery.substringAfter("://", withoutQuery)
        if (!afterScheme.contains('/')) return null
        val lastSegment = afterScheme.substringAfterLast('/')
        val dot = lastSegment.lastIndexOf('.')
        if (dot < 0 || dot == lastSegment.lastIndex) return null
        return lastSegment.substring(dot + 1).lowercase(Locale.ROOT)
    }
}
