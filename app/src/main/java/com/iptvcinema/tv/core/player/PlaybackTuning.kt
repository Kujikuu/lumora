package com.iptvcinema.tv.core.player

/**
 * Buffering, timeout and recovery constants for playback. Kept in one place so
 * start-up speed and rebuffer behavior can be tuned without touching player code.
 */
object PlaybackTuning {
    const val MIN_BUFFER_MS = 20_000
    const val MAX_BUFFER_MS = 50_000
    const val BUFFER_FOR_PLAYBACK_MS = 1_000
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_500

    const val CONNECT_TIMEOUT_MS = 8_000L
    const val READ_TIMEOUT_MS = 20_000L

    /** Segment/playlist load retries inside ExoPlayer before an error reaches the app. */
    const val LOADABLE_RETRY_COUNT = 6
    const val MAX_LOAD_RETRY_DELAY_MS = 2_000L

    /** Buffering this long with no position progress triggers a re-prepare. */
    const val STALL_TIMEOUT_MS = 15_000L

    /** Short buffering blips under this duration never show an indicator. */
    const val BUFFERING_INDICATOR_DELAY_MS = 500L

    /** Rapid channel up/down presses only start the stream once the user settles. */
    const val CHANNEL_ZAP_DEBOUNCE_MS = 250L

    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_BASE_DELAY_MS = 1_000L
}
