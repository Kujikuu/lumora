package com.iptvcinema.launcher.content

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderRowsParserTest {
    private fun row(rowKey: String, key: String, kind: String = "movie", wide: Long = 1, progress: Double? = null, validUntil: Long? = null) =
        mapOf(
            LauncherRowsContract.COLUMN_ROW_KEY to rowKey,
            LauncherRowsContract.COLUMN_KEY to key,
            LauncherRowsContract.COLUMN_KIND to kind,
            LauncherRowsContract.COLUMN_TITLE to "T $key",
            LauncherRowsContract.COLUMN_SUBTITLE to null,
            LauncherRowsContract.COLUMN_IMAGE_URL to "https://x/$key",
            LauncherRowsContract.COLUMN_WIDE to wide,
            LauncherRowsContract.COLUMN_PROGRESS to progress,
            LauncherRowsContract.COLUMN_VALID_UNTIL to validUntil,
            LauncherRowsContract.COLUMN_INTENT_URI to "lumora://play?type=$kind&id=$key",
            LauncherRowsContract.COLUMN_GENERATED_AT to 55L,
        )

    @Test
    fun parse_groupsCardsIntoTheFourRowsInContractOrder() {
        val snapshot = ProviderRowsParser.parse(
            listOf(row("new", "m2"), row("continue", "m1", progress = 0.5), row("live", "c1", kind = "channel", validUntil = 9)),
        )
        assertEquals(55L, snapshot.generatedAtMs)
        assertEquals(LauncherRowsContract.ROW_ORDER, snapshot.rows.map { it.key })
        assertEquals(listOf("m1"), snapshot.row("continue")!!.cards.map { it.key })
        assertEquals(0.5f, snapshot.row("continue")!!.cards[0].progress)
        assertEquals(emptyList<ContentCard>(), snapshot.row("favorites")!!.cards)
        assertEquals(ContentKind.Channel, snapshot.row("live")!!.cards[0].kind)
        assertEquals(9L, snapshot.row("live")!!.cards[0].validUntilMs)
    }

    @Test
    fun parse_skipsRowsMissingRequiredColumns() {
        val broken = row("new", "m2") - LauncherRowsContract.COLUMN_INTENT_URI
        val snapshot = ProviderRowsParser.parse(listOf(broken, row("new", "m3")))
        assertEquals(listOf("m3"), snapshot.row("new")!!.cards.map { it.key })
    }

    @Test
    fun parse_ofNothingGivesFourEmptyRows() {
        val snapshot = ProviderRowsParser.parse(emptyList())
        assertEquals(0L, snapshot.generatedAtMs)
        assertEquals(4, snapshot.rows.size)
    }
}
