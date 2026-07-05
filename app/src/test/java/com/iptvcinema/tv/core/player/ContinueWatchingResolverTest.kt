package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueWatchingResolverTest {
    private val episodes = listOf(
        episode("e1", season = 1, number = 1),
        episode("e2", season = 1, number = 2),
        episode("e3", season = 2, number = 1),
    )

    @Test
    fun resolve_includesNextUpEpisode_whenLatestEpisodeIsCompleted() = runBlocking {
        val resolver = ContinueWatchingResolver(FakeContinueWatchingCatalog(episodes))
        val history = listOf(
            completedEpisodeHistory(
                seriesId = "series-a",
                episodeId = "e1",
                lastWatchedEpochSecond = 100,
            ),
        )

        val result = resolver.resolve(history, sourceId = "source-1", limit = 10)

        assertEquals(1, result.size)
        assertEquals("e2", result.first().contentId)
        assertEquals(0L, result.first().positionMs)
        assertEquals("series-a", result.first().seriesId)
    }

    @Test
    fun resolve_prefersInProgressEpisode_overCompletedSeries() = runBlocking {
        val resolver = ContinueWatchingResolver(FakeContinueWatchingCatalog(episodes))
        val history = listOf(
            completedEpisodeHistory(
                seriesId = "series-a",
                episodeId = "e1",
                lastWatchedEpochSecond = 200,
            ),
            episodeHistory(
                seriesId = "series-a",
                episodeId = "e2",
                lastWatchedEpochSecond = 100,
            ),
        )

        val result = resolver.resolve(history, sourceId = "source-1", limit = 10)

        assertEquals(1, result.size)
        assertEquals("e2", result.first().contentId)
        assertTrue(WatchHistoryResumePolicy.isContinueWatching(result.first().positionMs, result.first().durationMs))
    }

    @Test
    fun resolve_respectsLimit() = runBlocking {
        val seriesBEpisodes = episodes.map { it.copy(id = "b-${it.id}", seriesId = "series-b") }
        val resolver = ContinueWatchingResolver(
            FakeContinueWatchingCatalog(episodes + seriesBEpisodes),
        )
        val history = listOf(
            completedEpisodeHistory(seriesId = "series-a", episodeId = "e1", lastWatchedEpochSecond = 200),
            completedEpisodeHistory(seriesId = "series-b", episodeId = "b-e1", lastWatchedEpochSecond = 100),
        )

        val result = resolver.resolve(history, sourceId = "source-1", limit = 1)

        assertEquals(1, result.size)
    }

    @Test
    fun resolve_returnsInProgressOnly_whenSourceIdMissing() = runBlocking {
        val resolver = ContinueWatchingResolver(FakeContinueWatchingCatalog(episodes))
        val history = listOf(
            episodeHistory(seriesId = "series-a", episodeId = "e1", lastWatchedEpochSecond = 100),
            completedEpisodeHistory(seriesId = "series-b", episodeId = "e1", lastWatchedEpochSecond = 90),
        )

        val result = resolver.resolve(history, sourceId = null, limit = 10)

        assertEquals(1, result.size)
        assertEquals("e1", result.first().contentId)
    }

    private fun episodeHistory(
        seriesId: String,
        episodeId: String,
        lastWatchedEpochSecond: Long,
    ) = WatchHistoryItem(
        id = "$seriesId-$episodeId-progress",
        profileId = "profile-1",
        sourceId = "source-1",
        contentId = episodeId,
        contentType = WatchHistoryContentType.EPISODE,
        seriesId = seriesId,
        title = "Episode",
        posterUrl = null,
        positionMs = 30_000L,
        durationMs = 100_000L,
        lastWatchedAt = Instant.ofEpochSecond(lastWatchedEpochSecond),
    )

    private fun completedEpisodeHistory(
        seriesId: String,
        episodeId: String,
        lastWatchedEpochSecond: Long,
    ) = WatchHistoryItem(
        id = "$seriesId-$episodeId-complete",
        profileId = "profile-1",
        sourceId = "source-1",
        contentId = episodeId,
        contentType = WatchHistoryContentType.EPISODE,
        seriesId = seriesId,
        title = "Episode",
        posterUrl = null,
        positionMs = 100_000L,
        durationMs = 100_000L,
        lastWatchedAt = Instant.ofEpochSecond(lastWatchedEpochSecond),
    )

    private fun episode(id: String, season: Int, number: Int): CatalogEpisode = CatalogEpisode(
        id = id,
        sourceId = "source-1",
        seriesId = "series-a",
        seasonNumber = season,
        episodeNumber = number,
        title = "Episode $number",
        streamUrl = "https://example.com/$id.m3u8",
        durationMinutes = 45,
        plot = null,
        thumbnailUrl = null,
    )

    private class FakeContinueWatchingCatalog(
        private val episodes: List<CatalogEpisode>,
    ) : ContinueWatchingCatalog {
        override suspend fun getEpisode(sourceId: String, episodeId: String): CatalogEpisode? =
            episodes.find { it.id == episodeId }

        override suspend fun getEpisode(
            sourceId: String,
            episodeId: String,
            seriesId: String,
        ): CatalogEpisode? = episodes.find { it.id == episodeId && it.seriesId == seriesId }

        override suspend fun getSeries(sourceId: String, seriesId: String): CatalogSeries? =
            CatalogSeries(
                id = seriesId,
                sourceId = sourceId,
                title = "Series Title",
                posterUrl = "https://example.com/poster.jpg",
                backdropUrl = null,
                categoryId = null,
                categoryName = null,
                plot = null,
                rating = null,
                year = null,
                cast = null,
                sortOrder = 0,
            )

        override suspend fun nextEpisode(sourceId: String, current: CatalogEpisode): CatalogEpisode? =
            EpisodeSequenceHelper.nextEpisode(
                episodes.filter { it.seriesId == current.seriesId },
                current.id,
            )
    }
}
