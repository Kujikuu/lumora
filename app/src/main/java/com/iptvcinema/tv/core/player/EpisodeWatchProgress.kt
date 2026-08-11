package com.iptvcinema.tv.core.player

enum class EpisodeWatchVisualState { UNWATCHED, PARTIAL, WATCHED }

object EpisodeWatchProgress {
    const val WATCHED_THRESHOLD = 0.95f

    fun visualState(progress: Float?): EpisodeWatchVisualState = when {
        progress == null || !progress.isFinite() || progress <= 0f || progress > 1f ->
            EpisodeWatchVisualState.UNWATCHED
        progress >= WATCHED_THRESHOLD -> EpisodeWatchVisualState.WATCHED
        else -> EpisodeWatchVisualState.PARTIAL
    }

    fun normalized(positionMs: Long, durationMs: Long?): Float? {
        val validDuration = durationMs?.takeIf { it > 0L } ?: return null
        if (positionMs <= 0L) return null
        return (positionMs.toFloat() / validDuration.toFloat()).coerceIn(0f, 1f)
    }
}
