package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchHistorySeriesFilterTest {
    @Test
    fun filterEpisodeHistoryForSeries_keepsOnlyMatchingEpisodesNewestFirst() {
        val result = filterEpisodeHistoryForSeries(
            items = listOf(
                history("old", "source-a", "series-a", WatchHistoryContentType.EPISODE, watchedAt = 100),
                history("wrong-source", "source-b", "series-a", WatchHistoryContentType.EPISODE, watchedAt = 400),
                history("wrong-series", "source-a", "series-b", WatchHistoryContentType.EPISODE, watchedAt = 300),
                history("movie", "source-a", "series-a", WatchHistoryContentType.MOVIE, watchedAt = 500),
                history("new", "source-a", "series-a", WatchHistoryContentType.EPISODE, watchedAt = 200),
            ),
            sourceId = "source-a",
            seriesId = "series-a",
        )

        assertEquals(listOf("new", "old"), result.map { it.contentId })
    }

    private fun history(
        contentId: String,
        sourceId: String,
        seriesId: String,
        contentType: WatchHistoryContentType,
        watchedAt: Long,
    ) = WatchHistoryItem(
        id = contentId,
        profileId = "profile-a",
        sourceId = sourceId,
        contentId = contentId,
        contentType = contentType,
        seriesId = seriesId,
        title = contentId,
        posterUrl = null,
        positionMs = 30_000L,
        durationMs = 60_000L,
        lastWatchedAt = Instant.ofEpochSecond(watchedAt),
    )
}
