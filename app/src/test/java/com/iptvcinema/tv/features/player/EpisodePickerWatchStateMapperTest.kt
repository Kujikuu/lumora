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
        lastWatchedAt = Instant.parse("2026-08-11T10:00:00Z"),
    )
}
