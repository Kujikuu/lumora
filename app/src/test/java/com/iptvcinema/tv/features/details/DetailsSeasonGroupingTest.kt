package com.iptvcinema.tv.features.details

import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailsSeasonGroupingTest {
    @Test
    fun toSeasonItems_sortsEpisodesWithinSeasonAndIncludesEmptySeasons() {
        val episodes = listOf(
            episode(id = "e3", season = 2, number = 3, title = "Late"),
            episode(id = "e1", season = 1, number = 2, title = "Second"),
            episode(id = "e2", season = 1, number = 1, title = "First"),
        )

        val seasons = DetailsSeasonGrouping.toSeasonItems(
            episodes = episodes,
            seriesId = "series-1",
            additionalSeasonNumbers = listOf(3),
        )

        assertEquals(listOf(1, 2, 3), seasons.map { it.seasonNumber })
        assertEquals(listOf("First", "Second"), seasons[0].episodes.map { it.title })
        assertEquals(listOf("Late"), seasons[1].episodes.map { it.title })
        assertEquals(emptyList<String>(), seasons[2].episodes.map { it.title })
    }

    private fun episode(
        id: String,
        season: Int,
        number: Int,
        title: String,
    ) = CatalogEpisode(
        id = id,
        sourceId = "src",
        seriesId = "series-1",
        seasonNumber = season,
        episodeNumber = number,
        title = title,
        streamUrl = "http://example.com/$id",
        durationMinutes = 45,
        plot = null,
        thumbnailUrl = null,
    )
}
