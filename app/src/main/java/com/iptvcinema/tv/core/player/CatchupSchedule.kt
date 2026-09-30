package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.EpgProgram
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CatchupDay(
    val date: LocalDate,
    val programs: List<EpgProgram>,
)

object CatchupSchedule {
    /**
     * Past programmes that can be replayed, grouped by local day. The most recent day comes first,
     * and within a day the most recent programme comes first, so the rail opens closest to now.
     */
    fun days(
        programs: List<EpgProgram>,
        archiveDays: Int,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<CatchupDay> = programs
        .filter { CatchupPolicy.isAvailable(archiveDays, it.startEpochMs, it.endEpochMs, nowMs) }
        .filter { it.endEpochMs <= nowMs }
        .distinctBy { it.startEpochMs }
        .sortedByDescending { it.startEpochMs }
        .groupBy { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }
        .map { (date, dayPrograms) -> CatchupDay(date, dayPrograms) }
}
