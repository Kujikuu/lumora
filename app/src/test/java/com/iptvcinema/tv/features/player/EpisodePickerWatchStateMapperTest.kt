package com.iptvcinema.tv.features.player

import com.iptvcinema.tv.core.model.EpisodeItem
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodePickerWatchStateMapperTest {
    @Test
    fun apply_mapsPartialAndCompletedProgress_withoutChangingUnwatchedEpisodes() {
        val seasons = seasons("partial", "watched", "unwatched")
        val history = listOf(
            history("partial", "source-a", "series-a", 25_000L, 100_000L),
            history("watched", "source-a", "series-a", 95_000L, 100_000L),
        )

        val episodes = EpisodePickerWatchStateMapper
            .apply(seasons, history, "source-a", "series-a")
            .flatMap { it.episodes }
            .associateBy { it.id }

        assertEquals(0.25f, episodes.getValue("partial").progress!!, 0.001f)
        assertEquals(0.95f, episodes.getValue("watched").progress!!, 0.001f)
        assertNull(episodes.getValue("unwatched").progress)
    }

    @Test
    fun apply_ignoresInvalidForeignAndMissingCatalogRows() {
        val seasons = seasons("target")
        val history = listOf(
            history("target", "source-b", "series-a", 50_000L, 100_000L),
            history("target", "source-a", "series-b", 50_000L, 100_000L),
            history("target", "source-a", "series-a", 50_000L, null),
            history("missing", "source-a", "series-a", 50_000L, 100_000L),
        )

        val episode = EpisodePickerWatchStateMapper
            .apply(seasons, history, "source-a", "series-a")
            .single()
            .episodes
            .single()

        assertNull(episode.progress)
    }

    @Test
    fun apply_prefersNewerDuplicateHistoryRow() {
        val episode = EpisodePickerWatchStateMapper.apply(
            seasons = seasons("target"),
            history = listOf(
                history(
                    contentId = "target",
                    sourceId = "source-a",
                    seriesId = "series-a",
                    positionMs = 25_000L,
                    durationMs = 100_000L,
                    lastWatchedAt = Instant.parse("2026-08-11T09:00:00Z"),
                ),
                history(
                    contentId = "target",
                    sourceId = "source-a",
                    seriesId = "series-a",
                    positionMs = 75_000L,
                    durationMs = 100_000L,
                    lastWatchedAt = Instant.parse("2026-08-11T10:00:00Z"),
                ),
            ),
            sourceId = "source-a",
            seriesId = "series-a",
        ).single().episodes.single()

        assertEquals(0.75f, episode.progress!!, 0.001f)
    }

    @Test
    fun apply_usesFirstDuplicateWhenTimestampsAreEqual() {
        val timestamp = Instant.parse("2026-08-11T10:00:00Z")
        val episode = EpisodePickerWatchStateMapper.apply(
            seasons = seasons("target"),
            history = listOf(
                history("target", "source-a", "series-a", 20_000L, 100_000L, timestamp),
                history("target", "source-a", "series-a", 80_000L, 100_000L, timestamp),
            ),
            sourceId = "source-a",
            seriesId = "series-a",
        ).single().episodes.single()

        assertEquals(0.2f, episode.progress!!, 0.001f)
    }

    private fun seasons(vararg episodeIds: String) = listOf(
        SeasonItem(
            id = "series-a-s1",
            seasonNumber = 1,
            episodes = episodeIds.mapIndexed { index, id ->
                EpisodeItem(id, index + 1, id, durationMinutes = 45)
            },
        ),
    )

    private fun history(
        contentId: String,
        sourceId: String,
        seriesId: String,
        positionMs: Long,
        durationMs: Long?,
        lastWatchedAt: Instant = Instant.parse("2026-08-11T10:00:00Z"),
    ) = WatchHistoryItem(
        id = contentId,
        profileId = "profile-a",
        sourceId = sourceId,
        contentId = contentId,
        contentType = WatchHistoryContentType.EPISODE,
        seriesId = seriesId,
        title = contentId,
        posterUrl = null,
        positionMs = positionMs,
        durationMs = durationMs,
        lastWatchedAt = lastWatchedAt,
    )
}
