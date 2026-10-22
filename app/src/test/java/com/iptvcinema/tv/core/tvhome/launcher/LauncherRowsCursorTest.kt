package com.iptvcinema.tv.core.tvhome.launcher

import com.iptvcinema.tv.core.tvhome.HomeChannelSpec
import com.iptvcinema.tv.core.tvhome.HomeProgramEntry
import com.iptvcinema.tv.core.tvhome.HomeProgramKind
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherRowsCursorTest {
    @Test
    fun rows_followTheColumnOrderOfTheContract() {
        val snapshot = LauncherRowsSnapshot(
            generatedAtMs = 42,
            rows = listOf(
                HomeChannelSpec(
                    key = "favorites",
                    displayName = "Favorite channels",
                    programs = listOf(
                        HomeProgramEntry(
                            key = "live:1",
                            kind = HomeProgramKind.Channel,
                            title = "One",
                            description = "Sports",
                            posterUrl = "https://x/1.png",
                            isWidePoster = true,
                            intentUri = "lumora://play?type=live&id=1",
                        ),
                    ),
                ),
            ),
        )
        val rows = LauncherRowsCursor.rowsOf(snapshot)
        assertEquals(1, rows.size)
        assertEquals(LauncherRowsContract.COLUMNS.size, rows[0].size)
        assertArrayEquals(
            arrayOf<Any?>(
                "favorites", "Favorite channels", "live:1", "channel", "One", "Sports",
                "https://x/1.png", 1, null, null, "lumora://play?type=live&id=1", 42L,
            ),
            rows[0],
        )
    }
}
