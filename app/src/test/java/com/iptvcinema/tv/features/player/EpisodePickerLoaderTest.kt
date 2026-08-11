package com.iptvcinema.tv.features.player

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodePickerLoaderTest {
    @Test
    fun loadEpisodePickerSeasons_combinesCatalogWithHistory() = runBlocking {
        val result = loadEpisodePickerSeasons(
            profileId = "profile-a",
            sourceId = "source-a",
            seriesId = "series-a",
            loadEpisodes = { _, _ -> listOf(episode("e1")) },
            loadHistory = { _, _, _ -> listOf(history("e1", 50_000L, 100_000L)) },
        )

        assertEquals(0.5f, result.single().episodes.single().progress!!, 0.001f)
    }

    @Test
    fun loadEpisodePickerSeasons_keepsCatalog_whenHistoryFails() = runBlocking {
        val result = loadEpisodePickerSeasons(
            profileId = "profile-a",
            sourceId = "source-a",
            seriesId = "series-a",
            loadEpisodes = { _, _ -> listOf(episode("e1")) },
            loadHistory = { _, _, _ -> error("offline") },
        )

        assertEquals("e1", result.single().episodes.single().id)
        assertNull(result.single().episodes.single().progress)
    }

    @Test
    fun loadEpisodePickerSeasons_returnsEmpty_whenCatalogFails() = runBlocking {
        val result = loadEpisodePickerSeasons(
            profileId = "profile-a",
            sourceId = "source-a",
            seriesId = "series-a",
            loadEpisodes = { _, _ -> error("catalog unavailable") },
            loadHistory = { _, _, _ -> emptyList() },
        )

        assertTrue(result.isEmpty())
    }

    private fun episode(id: String) = CatalogEpisode(
        id = id,
        sourceId = "source-a",
        seriesId = "series-a",
        seasonNumber = 1,
        episodeNumber = 1,
        title = id,
        streamUrl = "https://example.com/$id.m3u8",
        durationMinutes = 45,
        plot = null,
        thumbnailUrl = null,
    )

    private fun history(contentId: String, positionMs: Long, durationMs: Long?) = WatchHistoryItem(
        id = contentId,
        profileId = "profile-a",
        sourceId = "source-a",
        contentId = contentId,
        contentType = WatchHistoryContentType.EPISODE,
        seriesId = "series-a",
        title = contentId,
        posterUrl = null,
        positionMs = positionMs,
        durationMs = durationMs,
        lastWatchedAt = Instant.parse("2026-08-11T10:00:00Z"),
    )
}
