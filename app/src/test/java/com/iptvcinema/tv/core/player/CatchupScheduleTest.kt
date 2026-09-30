package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.epg.EpgRetention
import com.iptvcinema.tv.core.model.EpgProgram
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatchupScheduleTest {
    private val hour = 60 * 60_000L
    private val now = LocalDateTime.of(2026, 9, 30, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun isAvailable_requiresArchiveStartedProgrammeAndWindow() {
        assertTrue(CatchupPolicy.isAvailable(1, now - 2 * hour, now - hour, now))
        // Programme on air now can be restarted.
        assertTrue(CatchupPolicy.isAvailable(1, now - 10 * 60_000L, now + hour, now))
        assertFalse(CatchupPolicy.isAvailable(0, now - 2 * hour, now - hour, now))
        assertFalse(CatchupPolicy.isAvailable(1, now + hour, now + 2 * hour, now))
        assertFalse(CatchupPolicy.isAvailable(1, now - 25 * hour, now - 24 * hour, now))
    }

    @Test
    fun earliestStart_isCappedByKeptGuideData() {
        assertEquals(now - 24 * hour, CatchupPolicy.earliestStartMs(1, now))
        assertEquals(now - EpgRetention.PAST_MS, CatchupPolicy.earliestStartMs(14, now))
    }

    @Test
    fun durationMinutes_roundsUp() {
        assertEquals(31, CatchupPolicy.durationMinutes(0, 30 * 60_000L + 1))
        assertEquals(1, CatchupPolicy.durationMinutes(0, 1))
    }

    @Test
    fun days_groupsFinishedProgrammesNewestFirst() {
        val programs = listOf(
            program("yesterday-late", now - 14 * hour, now - 13 * hour),
            program("today-early", now - 3 * hour, now - 2 * hour),
            program("today-late", now - 2 * hour, now - hour),
            program("on-air", now - 10 * 60_000L, now + hour),
            program("too-old", now - 30 * hour, now - 29 * hour),
        )

        val days = CatchupSchedule.days(programs, archiveDays = 1, nowMs = now, zone = ZoneOffset.UTC)

        assertEquals(listOf(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 29)), days.map { it.date })
        assertEquals(listOf("today-late", "today-early"), days[0].programs.map { it.id })
        assertEquals(listOf("yesterday-late"), days[1].programs.map { it.id })
    }

    private fun program(id: String, start: Long, end: Long) = EpgProgram(
        id = id,
        channelId = "ch1",
        title = id,
        startHour = 0,
        startMinute = 0,
        durationMinutes = ((end - start) / 60_000L).toInt().coerceAtLeast(1),
        startEpochMs = start,
        endEpochMs = end,
    )
}
