package com.iptvcinema.tv.core.player

object EpisodeWatchProgress {
    const val WATCHED_THRESHOLD = 0.95f

    fun normalized(positionMs: Long, durationMs: Long?): Float? {
        val validDuration = durationMs?.takeIf { it > 0L } ?: return null
        if (positionMs <= 0L) return null
        return (positionMs.toFloat() / validDuration.toFloat()).coerceIn(0f, 1f)
    }
}
