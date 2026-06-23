package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextEpisodeResolver @Inject constructor(
    private val episodeCatalogRepository: EpisodeCatalogRepository,
) {
    suspend fun nextEpisode(sourceId: String, current: CatalogEpisode): CatalogEpisode? {
        val episodes = episodeCatalogRepository.getEpisodesForSeries(sourceId, current.seriesId)
        return EpisodeSequenceHelper.nextEpisode(episodes, current.id)
    }

    suspend fun previousEpisode(sourceId: String, current: CatalogEpisode): CatalogEpisode? {
        val episodes = episodeCatalogRepository.getEpisodesForSeries(sourceId, current.seriesId)
        return EpisodeSequenceHelper.previousEpisode(episodes, current.id)
    }

    suspend fun upNextEpisodes(
        sourceId: String,
        current: CatalogEpisode,
        limit: Int = 3,
    ): List<CatalogEpisode> {
        val episodes = episodeCatalogRepository.getEpisodesForSeries(sourceId, current.seriesId)
        return EpisodeSequenceHelper.upNextEpisodes(episodes, current.id, limit)
    }
}
