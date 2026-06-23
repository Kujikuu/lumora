package com.iptvcinema.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncStatusFormatterTest {
    @Test
    fun formatBanner_returnsSyncingWhenActive() {
        assertEquals("Syncing catalog…", SyncStatusFormatter.formatBanner(statusSyncing = true, lastSyncedAtEpochMs = null))
    }

    @Test
    fun formatBanner_returnsNullWhenNoSyncData() {
        assertNull(SyncStatusFormatter.formatBanner(statusSyncing = false, lastSyncedAtEpochMs = null))
    }

    @Test
    fun formatRelativeElapsed_formatsMinutesAndHours() {
        assertEquals("just now", SyncStatusFormatter.formatRelativeElapsed(30_000L))
        assertEquals("5m ago", SyncStatusFormatter.formatRelativeElapsed(5 * 60_000L))
        assertEquals("2h ago", SyncStatusFormatter.formatRelativeElapsed(2 * 60 * 60_000L))
    }
}
