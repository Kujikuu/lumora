package com.iptvcinema.tv.core.tvhome

/** Pure rules behind [HomeRowsBuilder], kept free of Android types so they are unit tested. */
object HomeRowRules {
    /** Watched fraction in 0..1, or null when the duration is unknown or nothing was watched. */
    fun progressOf(positionMs: Long, durationMs: Long?): Float? {
        if (durationMs == null || durationMs <= 0L || positionMs <= 0L) return null
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /** Favourites first, then recently watched, without repeats, at most [limit]. */
    fun liveNowChannelIds(
        favoriteChannelIds: List<String>,
        recentChannelIds: List<String>,
        limit: Int,
    ): List<String> = (favoriteChannelIds + recentChannelIds)
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)
}
