package com.iptvcinema.tv.features.player

/**
 * Seek steps for Left/Right presses while the overlay is hidden. Pressing again within
 * [RESET_AFTER_MS] keeps the run going and the step grows, so a long film is quick to cross
 * while a single tap still moves ten seconds.
 */
data class SeekRamp(
    val presses: Int = 0,
    val lastPressMs: Long = 0L,
    val direction: Int = 0,
) {
    /** The ramp after one more press at [nowMs] in [direction] (+1 forward, -1 back). */
    fun press(nowMs: Long, direction: Int): SeekRamp {
        val continues = direction == this.direction && nowMs - lastPressMs <= RESET_AFTER_MS
        return SeekRamp(
            presses = if (continues) presses + 1 else 1,
            lastPressMs = nowMs,
            direction = direction,
        )
    }

    /** Signed seek delta for the current press. */
    val deltaMs: Long
        get() = direction * stepMs(presses)

    companion object {
        const val RESET_AFTER_MS = 800L
        private const val STEP_SMALL_MS = 10_000L
        private const val STEP_MEDIUM_MS = 30_000L
        private const val STEP_LARGE_MS = 60_000L

        fun stepMs(presses: Int): Long = when {
            presses <= 3 -> STEP_SMALL_MS
            presses <= 8 -> STEP_MEDIUM_MS
            else -> STEP_LARGE_MS
        }

        /** "+30s" or "-1m", for the banner shown while seeking. */
        fun label(deltaMs: Long): String {
            val sign = if (deltaMs < 0) "-" else "+"
            val seconds = kotlin.math.abs(deltaMs) / 1_000L
            return if (seconds % 60L == 0L && seconds >= 60L) "$sign${seconds / 60L}m" else "$sign${seconds}s"
        }
    }
}
