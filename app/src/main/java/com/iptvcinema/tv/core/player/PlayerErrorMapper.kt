package com.iptvcinema.tv.core.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource

object PlayerErrorMapper {
    fun mapPlaybackError(error: PlaybackException, isXtreamSource: Boolean = false): Pair<String, String> {
        val cause = error.cause
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> "Connection timed out" to "STREAM_TIMEOUT"
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                val code = (cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode
                when (code) {
                    403 -> {
                        val message = if (isXtreamSource) {
                            "Your IPTV account may be expired or blocked. Check your subscription or reconnect in Settings."
                        } else {
                            "Stream blocked or expired"
                        }
                        message to "HTTP_403"
                    }
                    404 -> "Stream not found" to "HTTP_404"
                    else -> "Stream unavailable" to "HTTP_${code ?: "ERROR"}"
                }
            }
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            -> "Unsupported stream format" to "UNSUPPORTED_FORMAT"
            else -> "Stream unavailable" to "PLAYBACK_ERROR"
        }
    }
}
