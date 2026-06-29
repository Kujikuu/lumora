package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem

object WatchHistoryResumePolicy {
    const val RESUME_MIN_MS = 5_000L
    const val RESUME_MAX_RATIO = 0.95
    const val AUTOPLAY_REMAINING_MS = 30_000L
    const val AUTOPLAY_RELEASE_BUFFER_MS = 5_000L
    /** @deprecated Use [shouldShowAutoplay] for fixed remaining-time UX. */
    const val AUTOPLAY_THRESHOLD_RATIO = 0.95

    fun resumePositionMs(
        positionMs: Long,
        durationMs: Long,
        minMs: Long = RESUME_MIN_MS,
        maxRatio: Double = RESUME_MAX_RATIO,
    ): Long {
        if (durationMs <= 0L) return 0L
        if (positionMs < minMs) return 0L
        if (positionMs.toDouble() / durationMs.toDouble() > maxRatio) return 0L
        return positionMs
    }

    fun shouldShowAutoplay(
        positionMs: Long,
        durationMs: Long?,
        remainingThresholdMs: Long = AUTOPLAY_REMAINING_MS,
    ): Boolean {
        if (durationMs == null || durationMs <= 0L) return false
        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
        if (durationMs < 60_000L) {
            return positionMs.toDouble() / durationMs.toDouble() >= AUTOPLAY_THRESHOLD_RATIO
        }
        return remainingMs in 0..remainingThresholdMs
    }

    fun isBeforeAutoplayWindow(
        positionMs: Long,
        durationMs: Long?,
        remainingThresholdMs: Long = AUTOPLAY_REMAINING_MS,
    ): Boolean {
        if (durationMs == null || durationMs <= 0L) return true
        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
        return remainingMs > remainingThresholdMs + AUTOPLAY_RELEASE_BUFFER_MS
    }

    fun isNearEnd(positionMs: Long, durationMs: Long?, thresholdRatio: Double = AUTOPLAY_THRESHOLD_RATIO): Boolean {
        if (durationMs == null || durationMs <= 0L) return false
        return positionMs.toDouble() / durationMs.toDouble() >= thresholdRatio
    }

    fun isContinueWatching(positionMs: Long, durationMs: Long?): Boolean {
        if (durationMs == null || durationMs <= 0L) return false
        if (positionMs < RESUME_MIN_MS) return false
        return positionMs.toDouble() / durationMs.toDouble() < RESUME_MAX_RATIO
    }

    /**
     * Builds the Continue Watching list from history ordered by [WatchHistoryItem.lastWatchedAt] descending.
     * Keeps at most one in-progress episode per series (the most recently watched).
     */
    fun selectContinueWatching(items: List<WatchHistoryItem>, limit: Int): List<WatchHistoryItem> {
        if (limit <= 0) return emptyList()
        val inProgress = items.filter { item ->
            item.contentType in CONTINUE_WATCHING_TYPES &&
                isContinueWatching(item.positionMs, item.durationMs)
        }
        val result = mutableListOf<WatchHistoryItem>()
        val seenSeriesIds = mutableSetOf<String>()
        for (item in inProgress) {
            if (item.contentType == WatchHistoryContentType.EPISODE) {
                val seriesKey = item.seriesId?.takeIf { it.isNotBlank() }
                    ?: "episode:${item.contentId}"
                if (!seenSeriesIds.add(seriesKey)) continue
            }
            result.add(item)
            if (result.size >= limit) break
        }
        return result
    }

    private val CONTINUE_WATCHING_TYPES = setOf(
        WatchHistoryContentType.MOVIE,
        WatchHistoryContentType.EPISODE,
    )
}
