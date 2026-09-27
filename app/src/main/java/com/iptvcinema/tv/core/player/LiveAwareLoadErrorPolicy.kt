package com.iptvcinema.tv.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Retries failed segment and playlist loads more times, with shorter waits, than the
 * default policy. Most IPTV glitches are single failed requests, so retrying them
 * inside ExoPlayer avoids a full stream restart and the black screen it causes.
 */
@OptIn(UnstableApi::class)
class LiveAwareLoadErrorPolicy : DefaultLoadErrorHandlingPolicy(PlaybackTuning.LOADABLE_RETRY_COUNT) {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val defaultDelay = super.getRetryDelayMsFor(loadErrorInfo)
        if (defaultDelay == C.TIME_UNSET) return C.TIME_UNSET
        return defaultDelay.coerceAtMost(PlaybackTuning.MAX_LOAD_RETRY_DELAY_MS)
    }
}
