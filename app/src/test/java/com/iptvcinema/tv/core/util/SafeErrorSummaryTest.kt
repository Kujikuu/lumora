package com.iptvcinema.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SafeErrorSummaryTest {
    @Test
    fun keepsOnlyTheReason_neverHeaders() {
        val error = IllegalStateException(
            "failed to parse filter\nURL: https://x.supabase.co/rest/v1/t\nHeaders: [Authorization=[Bearer secret-token]]",
        )

        val summary = error.safeSummary()

        assertEquals("IllegalStateException: failed to parse filter", summary)
        assertFalse(summary.contains("secret-token"))
    }
}
