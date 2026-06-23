package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.catalog.CatalogEpisode

object EpisodeSequenceHelper {
    fun nextEpisode(episodes: List<CatalogEpisode>, currentEpisodeId: String): CatalogEpisode? {
        val sorted = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        val index = sorted.indexOfFirst { it.id == currentEpisodeId }
        if (index < 0 || index >= sorted.lastIndex) return null
        return sorted[index + 1]
    }

    fun previousEpisode(episodes: List<CatalogEpisode>, currentEpisodeId: String): CatalogEpisode? {
        val sorted = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        val index = sorted.indexOfFirst { it.id == currentEpisodeId }
        if (index <= 0) return null
        return sorted[index - 1]
    }

    fun upNextEpisodes(
        episodes: List<CatalogEpisode>,
        currentEpisodeId: String,
        limit: Int = 3,
    ): List<CatalogEpisode> {
        val sorted = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        val index = sorted.indexOfFirst { it.id == currentEpisodeId }
        if (index < 0) return emptyList()
        return sorted.drop(index + 1).take(limit)
    }
}
