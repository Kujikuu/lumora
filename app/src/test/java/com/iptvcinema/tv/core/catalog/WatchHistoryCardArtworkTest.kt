package com.iptvcinema.tv.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchHistoryCardArtworkTest {
    @Test
    fun resolveEpisodeContinueWatchingPosterUrl_prefersSeriesPosterOverStoredEpisodeThumbnail() {
        val result = resolveEpisodeContinueWatchingPosterUrl(
            seriesPosterUrl = "https://example.com/series.jpg",
            episodeThumbnailUrl = "https://example.com/broken-episode.jpg",
            storedPosterUrl = "https://example.com/broken-episode.jpg",
        )

        assertEquals("https://example.com/series.jpg", result)
    }

    @Test
    fun resolveEpisodeContinueWatchingPosterUrl_fallsBackToEpisodeThumbnailWhenSeriesMissing() {
        val result = resolveEpisodeContinueWatchingPosterUrl(
            seriesPosterUrl = null,
            episodeThumbnailUrl = "https://example.com/episode.jpg",
            storedPosterUrl = "https://example.com/stored.jpg",
        )

        assertEquals("https://example.com/episode.jpg", result)
    }

    @Test
    fun resolveEpisodeContinueWatchingPosterUrl_usesStoredPosterAsLastResort() {
        val result = resolveEpisodeContinueWatchingPosterUrl(
            seriesPosterUrl = null,
            episodeThumbnailUrl = null,
            storedPosterUrl = "https://example.com/stored.jpg",
        )

        assertEquals("https://example.com/stored.jpg", result)
    }
}
