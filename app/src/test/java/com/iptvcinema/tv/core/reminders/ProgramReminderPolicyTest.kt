package com.iptvcinema.tv.core.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramReminderPolicyTest {
    private fun reminder(id: String, startMs: Long) = ProgramReminder(
        id = id,
        profileId = "p",
        channelId = "c",
        channelName = "Channel",
        programId = id,
        title = "Show $id",
        startEpochMs = startMs,
        endEpochMs = startMs + 3_600_000L,
    )

    @Test
    fun canRemind_onlyForProgrammesStillAhead() {
        val now = 1_000_000L
        assertTrue(ProgramReminderPolicy.canRemind(now + ProgramReminderPolicy.LEAD_MS + 1L, now))
        assertFalse(ProgramReminderPolicy.canRemind(now + 10_000L, now))
        assertFalse(ProgramReminderPolicy.canRemind(now - 1L, now))
    }

    @Test
    fun due_returnsTheEarliestReminderInsideTheLeadWindow() {
        val now = 10_000_000L
        val soon = reminder("soon", now + 30_000L)
        val later = reminder("later", now + 45_000L)
        val far = reminder("far", now + 3_600_000L)
        assertEquals(soon, ProgramReminderPolicy.due(listOf(far, later, soon), now))
        assertNull(ProgramReminderPolicy.due(listOf(far), now))
    }

    @Test
    fun prune_dropsReminderWhoseProgrammeIsLongStarted() {
        val now = 10_000_000L
        val stale = reminder("stale", now - ProgramReminderPolicy.GRACE_MS - 1L)
        val recent = reminder("recent", now - 1_000L)
        assertEquals(listOf(recent), ProgramReminderPolicy.prune(listOf(stale, recent), now))
    }

    @Test
    fun nextWakeDelay_countsDownToTheLeadMoment() {
        val now = 10_000_000L
        val reminders = listOf(reminder("a", now + 5 * 60_000L))
        assertEquals(4 * 60_000L, ProgramReminderPolicy.nextWakeDelayMs(reminders, now))
        assertNull(ProgramReminderPolicy.nextWakeDelayMs(emptyList(), now))
        assertEquals(0L, ProgramReminderPolicy.nextWakeDelayMs(listOf(reminder("b", now)), now))
    }
}
