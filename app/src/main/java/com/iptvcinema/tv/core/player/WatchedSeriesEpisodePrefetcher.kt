package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WatchedSeriesEpisodePrefetcher @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    private val episodeCatalogRepository: EpisodeCatalogRepository,
    private val appSessionRepository: AppSessionRepository,
) {
    suspend fun prefetchForCurrentSession(): Int {
        val session = appSessionRepository.sessionState.first()
        val profileId = session.currentProfileId ?: return 0
        val sourceId = session.currentSourceId ?: return 0
        return prefetchForProfile(profileId, sourceId)
    }

    suspend fun prefetchForProfile(profileId: String, sourceId: String): Int {
        val seriesIds = watchHistoryRepository.getDistinctSeriesIds(profileId)
        var prefetched = 0
        for (seriesId in seriesIds) {
            runCatching {
                episodeCatalogRepository.getEpisodesForSeries(sourceId, seriesId)
            }.onSuccess { episodes ->
                if (episodes.isNotEmpty()) prefetched++
            }
        }
        return prefetched
    }
}
