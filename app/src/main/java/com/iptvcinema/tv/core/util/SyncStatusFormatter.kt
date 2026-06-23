package com.iptvcinema.tv.core.util

import java.util.concurrent.TimeUnit

object SyncStatusFormatter {
    fun formatBanner(statusSyncing: Boolean, lastSyncedAtEpochMs: Long?): String? {
        if (statusSyncing) return "Syncing catalog…"
        val lastSynced = lastSyncedAtEpochMs ?: return null
        val elapsedMs = (System.currentTimeMillis() - lastSynced).coerceAtLeast(0L)
        return "Last synced ${formatRelativeElapsed(elapsedMs)}"
    }

    fun formatRelativeElapsed(elapsedMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 24 * 60 -> "${minutes / 60}h ago"
            else -> "${minutes / (24 * 60)}d ago"
        }
    }
}
