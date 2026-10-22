package com.iptvcinema.launcher.home

import com.iptvcinema.launcher.apps.AppCard
import com.iptvcinema.launcher.apps.AppEntry
import com.iptvcinema.launcher.content.ContentCard
import com.iptvcinema.launcher.content.ContentKind
import com.iptvcinema.launcher.content.ContentRow
import com.iptvcinema.launcher.content.ContentSnapshot
import com.iptvcinema.launcher.content.ContentState
import com.iptvcinema.launcher.inputs.InputDescriptor
import com.iptvcinema.launcher.inputs.InputState
import com.iptvcinema.launcher.inputs.InputType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRowsAssemblerTest {
    private val labels = HomeLabels(
        openApp = "Open",
        installApp = "Install",
        emptyRow = "Empty",
        noInputs = "No inputs",
        inputTypeNames = mapOf(InputType.Hdmi to "HDMI"),
    )
    private val now = 1_000_000L

    private fun channel(key: String, subtitle: String?, validUntil: Long?) = ContentCard(
        key = key, kind = ContentKind.Channel, title = key, subtitle = subtitle, imageUrl = null,
        isWide = true, intentUri = "lumora://play?type=live&id=$key", validUntilMs = validUntil,
    )

    @Test
    fun rows_alwaysComeInTheSameOrderAndAreNeverEmpty() {
        val rows = HomeRowsAssembler.assemble(ContentState(), apps = null, inputs = null, nowMs = now, labels = labels)
        assertEquals(RowKey.entries, rows.map { it.key })
        assertTrue(rows.all { it.cards.isNotEmpty() })
        assertTrue(rows.all { row -> row.cards.all { it.isPlaceholder } })
    }

    @Test
    fun missingApp_offersInstallOnEveryContentRow() {
        val rows = HomeRowsAssembler.assemble(ContentState(appInstalled = false), null, null, now, labels)
        rows.take(4).forEach { row ->
            assertEquals("Install", row.cards.single().title)
            assertEquals(CardAction.InstallMainApp, row.cards.single().action)
        }
    }

    @Test
    fun noSnapshotYet_offersToOpenTheApp() {
        val rows = HomeRowsAssembler.assemble(ContentState(appInstalled = true, snapshot = null), null, null, now, labels)
        assertEquals(CardAction.OpenMainApp, rows[0].cards.single().action)
        assertEquals("Open", rows[0].cards.single().title)
    }

    @Test
    fun emptyRowInSnapshot_showsTheEmptyPlaceholder() {
        val snapshot = ContentSnapshot(now, listOf(ContentRow("continue", emptyList())))
        val rows = HomeRowsAssembler.assemble(ContentState(snapshot = snapshot), null, null, now, labels)
        assertEquals("Empty", rows[0].cards.single().title)
        assertTrue(rows[0].cards.single().isPlaceholder)
    }

    @Test
    fun liveNow_dropsAProgrammeThatHasEnded() {
        val snapshot = ContentSnapshot(
            now,
            listOf(ContentRow("live", listOf(channel("a", "Ended", now - 1), channel("b", "On now", now + 1), channel("c", "No EPG", null)))),
        )
        val live = HomeRowsAssembler.assemble(ContentState(snapshot = snapshot), null, null, now, labels)[2]
        assertNull(live.cards[0].subtitle)
        assertEquals("On now", live.cards[1].subtitle)
        assertEquals("No EPG", live.cards[2].subtitle)
        assertEquals(CardAction.OpenLink("lumora://play?type=live&id=a"), live.cards[0].action)
    }

    @Test
    fun apps_becomeCardsWithFileArtworkAndSystemFlag() {
        val apps = listOf(AppCard(AppEntry("com.a", "A", true, isSystem = true, 1), File("/x/a.png")), AppCard(AppEntry("com.b", "B", false, false, 2), null))
        val row = HomeRowsAssembler.assemble(ContentState(), apps, null, now, labels)[4]
        assertEquals(listOf("com.a", "com.b"), row.cards.map { it.key })
        assertEquals(File("/x/a.png").toURI().toString(), row.cards[0].image)
        assertNull(row.cards[1].image)
        assertEquals(CardAction.OpenApp("com.a", isSystem = true), row.cards[0].action)
    }

    @Test
    fun inputs_carryTypeMarkAndConnectionBadge() {
        val inputs = listOf(
            InputDescriptor("hdmi1", InputType.Hdmi, "HDMI 1", InputState.Connected, true),
            InputDescriptor("hdmi2", InputType.Hdmi, "HDMI 2", InputState.Disconnected, true),
            InputDescriptor("tuner", InputType.Tuner, "TV", InputState.Unknown, false),
        )
        val row = HomeRowsAssembler.assemble(ContentState(), null, inputs, now, labels)[5]
        assertEquals(listOf("HDMI", "HDMI", "Tuner"), row.cards.map { it.mark })
        assertEquals(listOf(CardBadge.Connected, CardBadge.Disconnected, null), row.cards.map { it.badge })
        assertEquals(CardAction.OpenInput(inputs[0]), row.cards[0].action)
    }

    @Test
    fun noInputs_offersTheSystemPicker() {
        val row = HomeRowsAssembler.assemble(ContentState(), null, emptyList(), now, labels)[5]
        assertEquals(CardAction.OpenInputPicker, row.cards.single().action)
        assertEquals("No inputs", row.cards.single().title)
    }
}
