package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.model.EpisodeItem
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode

object SeasonGrouping {
    fun toSeasonItems(
        episodes: List<CatalogEpisode>,
        seriesId: String,
        additionalSeasonNumbers: List<Int> = emptyList(),
    ): List<SeasonItem> {
        val grouped = episodes.groupBy { it.seasonNumber }
        val allSeasonNumbers = (grouped.keys + additionalSeasonNumbers).distinct().sorted()
        return allSeasonNumbers.map { seasonNumber ->
            SeasonItem(
                id = "$seriesId-s$seasonNumber",
                seasonNumber = seasonNumber,
                episodes = grouped[seasonNumber].orEmpty()
                    .sortedBy { it.episodeNumber }
                    .map { episode ->
                        EpisodeItem(
                            id = episode.id,
                            episodeNumber = episode.episodeNumber,
                            title = episode.title,
                            durationMinutes = episode.durationMinutes ?: 0,
                            thumbnailUrl = episode.thumbnailUrl,
                        )
                    },
            )
        }
    }
}
