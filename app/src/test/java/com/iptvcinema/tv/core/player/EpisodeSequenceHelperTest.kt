package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeSequenceHelperTest {
    private val episodes = listOf(
        episode("e1", season = 1, number = 1),
        episode("e2", season = 1, number = 2),
        episode("e3", season = 2, number = 1),
    )

    @Test
    fun nextEpisode_returnsNextInSeason() {
        assertEquals("e2", EpisodeSequenceHelper.nextEpisode(episodes, "e1")?.id)
    }

    @Test
    fun nextEpisode_crossesSeasonBoundary() {
        assertEquals("e3", EpisodeSequenceHelper.nextEpisode(episodes, "e2")?.id)
    }

    @Test
    fun nextEpisode_returnsNull_forLastEpisode() {
        assertNull(EpisodeSequenceHelper.nextEpisode(episodes, "e3"))
    }

    @Test
    fun upNextEpisodes_returnsFollowingEpisodes() {
        val upNext = EpisodeSequenceHelper.upNextEpisodes(episodes, "e1", limit = 2)
        assertEquals(listOf("e2", "e3"), upNext.map { it.id })
    }

    @Test
    fun previousEpisode_returnsPreviousInSeason() {
        assertEquals("e1", EpisodeSequenceHelper.previousEpisode(episodes, "e2")?.id)
    }

    @Test
    fun previousEpisode_crossesSeasonBoundary() {
        assertEquals("e2", EpisodeSequenceHelper.previousEpisode(episodes, "e3")?.id)
    }

    @Test
    fun previousEpisode_returnsNull_forFirstEpisode() {
        assertNull(EpisodeSequenceHelper.previousEpisode(episodes, "e1"))
    }

    private fun episode(id: String, season: Int, number: Int): CatalogEpisode = CatalogEpisode(
        id = id,
        sourceId = "source",
        seriesId = "series",
        seasonNumber = season,
        episodeNumber = number,
        title = "Episode $number",
        streamUrl = "https://example.com/$id.m3u8",
        durationMinutes = 45,
        plot = null,
        thumbnailUrl = null,
    )
}
