package com.iptvcinema.tv.core.player

import androidx.media3.common.PlaybackException

enum class PlaybackRecoveryAction {
    SEEK_TO_LIVE_EDGE,
    RETRY,
    FAIL,
}

/** Decides how the player reacts to errors and stalls. Pure so it can be unit tested. */
object PlaybackRecoveryPolicy {
    private val transientNetworkCodes = setOf(
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    )

    // Live IPTV streams often hiccup mid-stream with a broken segment or playlist.
    // Re-opening the stream usually fixes it, so these are retried for live only.
    private val liveGlitchCodes = setOf(
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
    )

    fun forError(
        errorCode: Int,
        isLive: Boolean,
        attempt: Int,
        maxAttempts: Int = PlaybackTuning.MAX_RETRY_ATTEMPTS,
    ): PlaybackRecoveryAction {
        if (attempt >= maxAttempts) return PlaybackRecoveryAction.FAIL
        return when {
            errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> PlaybackRecoveryAction.SEEK_TO_LIVE_EDGE
            errorCode in transientNetworkCodes -> PlaybackRecoveryAction.RETRY
            isLive && errorCode in liveGlitchCodes -> PlaybackRecoveryAction.RETRY
            else -> PlaybackRecoveryAction.FAIL
        }
    }

    fun retryDelayMs(attempt: Int, baseDelayMs: Long = PlaybackTuning.RETRY_BASE_DELAY_MS): Long {
        val exponent = (attempt - 1).coerceIn(0, 10)
        return baseDelayMs * (1L shl exponent)
    }

    fun isStalled(stillBuffering: Boolean, positionAtStartMs: Long, currentPositionMs: Long): Boolean =
        stillBuffering && currentPositionMs == positionAtStartMs
}
