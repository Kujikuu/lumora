package com.iptvcinema.launcher.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentRowsCodecTest {
    private val snapshot = ContentSnapshot(
        generatedAtMs = 123,
        rows = listOf(
            ContentRow(
                "continue",
                listOf(ContentCard("movie:1", ContentKind.Movie, "Film", null, "https://x/p.jpg", true, "lumora://play?type=movie&id=1", progress = 0.4f)),
            ),
            ContentRow(
                "live",
                listOf(ContentCard("live:9", ContentKind.Channel, "قناة", "News", null, true, "lumora://play?type=live&id=9", validUntilMs = 999)),
            ),
            ContentRow("new", emptyList()),
        ),
    )

    @Test
    fun roundTrip_keepsEveryField() {
        assertEquals(snapshot, ContentRowsCodec.decode(ContentRowsCodec.encode(snapshot)))
    }

    @Test
    fun decode_rejectsGarbage() {
        assertNull(ContentRowsCodec.decode("{"))
        assertNull(ContentRowsCodec.decode("""{"version":7,"generatedAt":1,"rows":[]}"""))
    }
}
