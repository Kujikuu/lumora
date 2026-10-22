package com.iptvcinema.tv.core.tvhome.launcher

import com.iptvcinema.tv.core.tvhome.HomeChannelSpec
import com.iptvcinema.tv.core.tvhome.HomeProgramEntry
import com.iptvcinema.tv.core.tvhome.HomeProgramKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherRowsCodecTest {
    private val snapshot = LauncherRowsSnapshot(
        generatedAtMs = 1_700_000_000_000,
        rows = listOf(
            HomeChannelSpec(
                key = "continue",
                displayName = "Continue watching",
                programs = listOf(
                    HomeProgramEntry(
                        key = "movie:1",
                        kind = HomeProgramKind.Movie,
                        title = "Film \"quoted\"",
                        description = null,
                        posterUrl = "https://x/p.jpg",
                        isWidePoster = true,
                        intentUri = "lumora://play?type=movie&id=1",
                        progress = 0.5f,
                    ),
                ),
            ),
            HomeChannelSpec(
                key = "live",
                displayName = "يُعرض الآن",
                programs = listOf(
                    HomeProgramEntry(
                        key = "live:9",
                        kind = HomeProgramKind.Channel,
                        title = "MBC",
                        description = "News",
                        posterUrl = null,
                        isWidePoster = true,
                        intentUri = "lumora://play?type=live&id=9",
                        validUntilMs = 1_700_000_900_000,
                    ),
                ),
            ),
            HomeChannelSpec(key = "new", displayName = "New this week", programs = emptyList()),
        ),
    )

    @Test
    fun roundTrip_keepsEveryField() {
        assertEquals(snapshot, LauncherRowsCodec.decode(LauncherRowsCodec.encode(snapshot)))
    }

    @Test
    fun decode_rejectsGarbageAndOtherVersions() {
        assertNull(LauncherRowsCodec.decode("not json"))
        assertNull(LauncherRowsCodec.decode("""{"version":99,"generatedAt":1,"rows":[]}"""))
    }
}
