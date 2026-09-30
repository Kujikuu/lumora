package com.iptvcinema.tv.core.tvhome

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchNextDiffTest {
    @Test
    fun plan_updatesKeptRowsInsertsNewAndDeletesStaleOrDuplicate() {
        val plan = WatchNextDiff.plan(
            existing = listOf(
                1L to "movie:a",
                2L to "movie:gone",
                3L to "movie:a",
                4L to null,
            ),
            desired = listOf(entry("movie:a"), entry("series:b")),
        )

        assertEquals(listOf(1L to "movie:a"), plan.updates.map { (id, entry) -> id to entry.key })
        assertEquals(listOf("series:b"), plan.inserts.map { it.key })
        assertEquals(listOf(2L, 3L, 4L), plan.deletes)
    }

    @Test
    fun plan_withNothingDesiredDeletesEverything() {
        val plan = WatchNextDiff.plan(existing = listOf(7L to "movie:a"), desired = emptyList())

        assertEquals(emptyList<WatchNextEntry>(), plan.inserts)
        assertEquals(listOf(7L), plan.deletes)
    }

    private fun entry(key: String) = WatchNextEntry(
        key = key,
        kind = WatchNextKind.Continue,
        programKind = WatchNextProgramKind.Movie,
        title = key,
        episodeTitle = null,
        seasonNumber = null,
        episodeNumber = null,
        posterUrl = null,
        positionMs = 1L,
        durationMs = null,
        lastEngagementMs = 0L,
        intentUri = "lumora://play?type=movie&id=$key",
    )
}
