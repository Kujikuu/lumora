package com.iptvcinema.tv.core.util

import com.iptvcinema.tv.core.model.WatchHistoryItem

/**
 * Formats remaining watch time in Yango-style labels: "39 min" or "2 h 14 min".
 */
object RemainingWatchTimeFormatter {
    data class Parts(
        val hours: Int,
        val minutes: Int,
    )

    fun partsFromRemainingMs(remainingMs: Long): Parts? {
        if (remainingMs <= 0L) return null
        val totalMinutes = ((remainingMs + 59_999) / 60_000).toInt().coerceAtLeast(1)
        return Parts(
            hours = totalMinutes / 60,
            minutes = totalMinutes % 60,
        )
    }

    fun format(parts: Parts, appStrings: AppStrings): String = when {
        parts.hours > 0 && parts.minutes > 0 ->
            appStrings.get(
                com.iptvcinema.tv.R.string.home_remaining_hours_minutes,
                parts.hours,
                parts.minutes,
            )
        parts.hours > 0 ->
            appStrings.get(
                com.iptvcinema.tv.R.string.home_remaining_hours_only,
                parts.hours,
            )
        else ->
            appStrings.get(
                com.iptvcinema.tv.R.string.home_remaining_minutes,
                parts.minutes.coerceAtLeast(1),
            )
    }

    fun formatRemainingMs(remainingMs: Long, appStrings: AppStrings): String? =
        partsFromRemainingMs(remainingMs)?.let { format(it, appStrings) }

    fun formatFromWatchHistory(item: WatchHistoryItem, appStrings: AppStrings): String? =
        item.durationMs?.takeIf { it > 0 }?.let { duration ->
            formatRemainingMs((duration - item.positionMs).coerceAtLeast(0L), appStrings)
        }
}
