package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.epg.EpgRetention
import kotlin.math.ceil

/** Which programmes can be replayed from a channel's archive, and how long a request spans. */
object CatchupPolicy {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Earliest start time that can be replayed, bounded by both the archive and kept guide data. */
    fun earliestStartMs(archiveDays: Int, nowMs: Long): Long =
        maxOf(nowMs - archiveDays * DAY_MS, nowMs - EpgRetention.PAST_MS)

    /** A programme can be replayed once it has started, if it lies inside the archive window. */
    fun isAvailable(archiveDays: Int, startMs: Long, endMs: Long, nowMs: Long): Boolean =
        archiveDays > 0 && endMs > startMs && startMs < nowMs && startMs >= earliestStartMs(archiveDays, nowMs)

    fun durationMinutes(startMs: Long, endMs: Long): Int =
        ceil((endMs - startMs) / 60_000.0).toInt().coerceAtLeast(1)
}
