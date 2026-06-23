package com.iptvcinema.tv.core.epg

import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideLayoutHelperTest {
    private val windowStart = 1_000_000L
    private val windowEnd = windowStart + 4 * 60 * 60 * 1000L

    @Test
    fun programsInWindow_keepsOverlappingPrograms() {
        val programs = listOf(
            program(id = "a", start = windowStart - 1_000, end = windowStart + 1_000),
            program(id = "b", start = windowEnd - 1_000, end = windowEnd + 1_000),
            program(id = "c", start = windowEnd + 1_000, end = windowEnd + 5_000),
        )

        val visible = GuideLayoutHelper.programsInWindow(programs, windowStart, windowEnd)

        assertEquals(listOf("a", "b"), visible.map { it.id })
    }

    @Test
    fun programWidthFraction_respectsMinimumWidth() {
        val shortProgram = program(
            id = "short",
            start = windowStart,
            end = windowStart + 60_000,
        )
        val duration = GuideLayoutHelper.windowDurationMs(windowStart, windowEnd)

        val fraction = GuideLayoutHelper.programWidthFraction(shortProgram, duration)

        assertEquals(0.05f, fraction, 0.0001f)
    }

    @Test
    fun currentTimeFraction_isNullOutsideWindow() {
        assertNull(
            GuideLayoutHelper.currentTimeFraction(
                nowMs = windowStart - 1,
                windowStartMs = windowStart,
                windowEndMs = windowEnd,
            ),
        )
    }

    @Test
    fun shiftWindowStart_movesByRequestedHours() {
        val shifted = GuideLayoutHelper.shiftWindowStart(windowStart, 24)
        assertEquals(windowStart + 24 * 60 * 60 * 1000L, shifted)
    }

    @Test
    fun timelineSlotStarts_coversWindow() {
        val slots = GuideLayoutHelper.timelineSlotStarts(windowStart, windowEnd)
        assertTrue(slots.isNotEmpty())
        assertTrue(slots.first() <= windowStart)
        assertTrue(slots.last() < windowEnd)
    }

    @Test
    fun programsForSelectedChannel_returnsPlaceholderWhenEmpty() {
        val channel = ChannelItem(
            id = "ch1",
            name = "News HD",
            category = "News",
            currentProgram = "Morning News",
            programStart = "09:00",
            programEnd = "10:00",
            programProgress = 0.5f,
        )
        val programs = GuideLayoutHelper.programsForSelectedChannel(
            programs = emptyList(),
            channel = channel,
            windowStartMs = windowStart,
            windowEndMs = windowEnd,
        )

        assertEquals(1, programs.size)
        assertEquals("ch1", programs.first().channelId)
        assertEquals("Morning News", programs.first().title)
        assertEquals(windowStart, programs.first().startEpochMs)
        assertEquals(windowEnd, programs.first().endEpochMs)
    }

    @Test
    fun programsForSelectedChannel_prefersRealPrograms() {
        val channel = ChannelItem(
            id = "ch1",
            name = "News HD",
            category = "News",
            currentProgram = "Morning News",
            programStart = "09:00",
            programEnd = "10:00",
            programProgress = 0.5f,
        )
        val real = listOf(
            program(id = "real", start = windowStart + 1_000, end = windowStart + 3_600_000),
        )
        val programs = GuideLayoutHelper.programsForSelectedChannel(
            programs = real,
            channel = channel,
            windowStartMs = windowStart,
            windowEndMs = windowEnd,
        )

        assertEquals(listOf("real"), programs.map { it.id })
    }

    private fun program(id: String, start: Long, end: Long): EpgProgram = EpgProgram(
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
