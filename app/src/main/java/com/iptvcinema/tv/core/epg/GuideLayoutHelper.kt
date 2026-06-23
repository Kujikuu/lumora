package com.iptvcinema.tv.core.epg

import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object GuideLayoutHelper {
    const val SLOT_DURATION_MS = 30L * 60 * 1000
    const val DEFAULT_WINDOW_PAST_MS = 30L * 60 * 1000
    const val DEFAULT_WINDOW_FUTURE_MS = (3.5 * 60 * 60 * 1000).toLong()
    const val SHIFT_HOURS = 24

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    fun defaultWindowStart(nowMs: Long): Long = nowMs - DEFAULT_WINDOW_PAST_MS

    fun defaultWindowEnd(nowMs: Long): Long = nowMs + DEFAULT_WINDOW_FUTURE_MS

    fun shiftWindowStart(currentStartMs: Long, hours: Int): Long =
        currentStartMs + hours * 60L * 60 * 1000

    fun windowEndFromStart(windowStartMs: Long): Long =
        windowStartMs + DEFAULT_WINDOW_PAST_MS + DEFAULT_WINDOW_FUTURE_MS

    fun programsInWindow(
        programs: List<EpgProgram>,
        windowStartMs: Long,
        windowEndMs: Long,
    ): List<EpgProgram> = programs.filter { program ->
        program.endEpochMs > windowStartMs && program.startEpochMs < windowEndMs
    }

    fun programsForChannel(
        programs: List<EpgProgram>,
        channelId: String,
        windowStartMs: Long,
        windowEndMs: Long,
    ): List<EpgProgram> = programsInWindow(programs, windowStartMs, windowEndMs)
        .filter { it.channelId == channelId }
        .sortedBy { it.startEpochMs }

    fun timelineSlotStarts(windowStartMs: Long, windowEndMs: Long): List<Long> {
        if (windowEndMs <= windowStartMs) return emptyList()
        val alignedStart = (windowStartMs / SLOT_DURATION_MS) * SLOT_DURATION_MS
        val slots = mutableListOf<Long>()
        var slot = alignedStart
        while (slot < windowEndMs) {
            if (slot >= windowStartMs - SLOT_DURATION_MS) {
                slots += slot
            }
            slot += SLOT_DURATION_MS
        }
        return slots
    }

    fun formatSlotLabel(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(timeFormatter)

    fun formatDateLabel(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(dateFormatter)

    fun formatCurrentTimeLabel(nowMs: Long): String = formatSlotLabel(nowMs)

    fun windowDurationMs(windowStartMs: Long, windowEndMs: Long): Long =
        (windowEndMs - windowStartMs).coerceAtLeast(1)

    fun programStartOffsetFraction(
        program: EpgProgram,
        windowStartMs: Long,
        windowDurationMs: Long,
    ): Float = ((program.startEpochMs - windowStartMs).toFloat() / windowDurationMs).coerceIn(0f, 1f)

    fun programWidthFraction(
        program: EpgProgram,
        windowDurationMs: Long,
    ): Float = ((program.endEpochMs - program.startEpochMs).toFloat() / windowDurationMs)
        .coerceIn(0.05f, 1f)

    fun currentTimeFraction(
        nowMs: Long,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Float? {
        if (nowMs < windowStartMs || nowMs > windowEndMs) return null
        val duration = windowDurationMs(windowStartMs, windowEndMs)
        return ((nowMs - windowStartMs).toFloat() / duration).coerceIn(0f, 1f)
    }

    fun placeholderProgramsForChannel(
        channel: ChannelItem,
        windowStartMs: Long,
        windowEndMs: Long,
        fallbackTitle: String = "No program info",
    ): List<EpgProgram> {
        val title = channel.currentProgram.takeIf { it.isNotBlank() } ?: fallbackTitle
        val durationMinutes = ((windowEndMs - windowStartMs) / 60_000L).toInt().coerceAtLeast(1)
        val start = Instant.ofEpochMilli(windowStartMs).atZone(ZoneId.systemDefault())
        return listOf(
            EpgProgram(
                id = "placeholder-${channel.id}",
                channelId = channel.id,
                title = title,
                startHour = start.hour,
                startMinute = start.minute,
                durationMinutes = durationMinutes,
                startEpochMs = windowStartMs,
                endEpochMs = windowEndMs,
                description = channel.programDescription,
            ),
        )
    }

    fun programsForSelectedChannel(
        programs: List<EpgProgram>,
        channel: ChannelItem,
        windowStartMs: Long,
        windowEndMs: Long,
        fallbackTitle: String = "No program info",
    ): List<EpgProgram> {
        val real = programsForChannel(programs, channel.id, windowStartMs, windowEndMs)
        return real.ifEmpty {
            placeholderProgramsForChannel(channel, windowStartMs, windowEndMs, fallbackTitle)
        }
    }

    fun channelItemWithProgram(
        channel: ChannelItem,
        program: EpgProgram,
        nowMs: Long,
    ): ChannelItem {
        val durationMs = (program.endEpochMs - program.startEpochMs).coerceAtLeast(1)
        val elapsedMs = (nowMs - program.startEpochMs).coerceIn(0, durationMs)
        return channel.copy(
            currentProgram = program.title,
            programDescription = program.description,
            programStart = formatTime(program.startEpochMs),
            programEnd = formatTime(program.endEpochMs),
            programProgress = elapsedMs.toFloat() / durationMs.toFloat(),
        )
    }

    private fun formatTime(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
}
