package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import javax.inject.Inject
import javax.inject.Singleton

interface ContinueWatchingCatalog {
    suspend fun getEpisode(sourceId: String, episodeId: String): CatalogEpisode?
    suspend fun getEpisode(sourceId: String, episodeId: String, seriesId: String): CatalogEpisode?
    suspend fun getSeries(sourceId: String, seriesId: String): CatalogSeries?
    suspend fun nextEpisode(sourceId: String, current: CatalogEpisode): CatalogEpisode?
}

@Singleton
class DefaultContinueWatchingCatalog @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val episodeCatalogRepository: EpisodeCatalogRepository,
    private val nextEpisodeResolver: NextEpisodeResolver,
) : ContinueWatchingCatalog {
    override suspend fun getEpisode(sourceId: String, episodeId: String): CatalogEpisode? =
        catalogRepository.getEpisode(sourceId, episodeId)

    override suspend fun getEpisode(sourceId: String, episodeId: String, seriesId: String): CatalogEpisode? =
        episodeCatalogRepository.getEpisode(sourceId, episodeId, seriesId)

    override suspend fun getSeries(sourceId: String, seriesId: String): CatalogSeries? =
        catalogRepository.getSeries(sourceId, seriesId)

    override suspend fun nextEpisode(sourceId: String, current: CatalogEpisode): CatalogEpisode? =
        nextEpisodeResolver.nextEpisode(sourceId, current)
}

@Singleton
class ContinueWatchingResolver @Inject constructor(
    private val catalog: ContinueWatchingCatalog,
) {
    suspend fun resolve(
        history: List<WatchHistoryItem>,
        sourceId: String?,
        limit: Int,
    ): List<WatchHistoryItem> {
        val inProgress = WatchHistoryResumePolicy.selectContinueWatching(history, limit)
        if (inProgress.size >= limit || sourceId.isNullOrBlank()) return inProgress

        val seenSeriesIds = inProgress
            .filter { it.contentType == WatchHistoryContentType.EPISODE }
            .mapNotNull { item -> item.seriesId?.takeIf { it.isNotBlank() } }
            .toMutableSet()

        val nextUp = mutableListOf<WatchHistoryItem>()
        val episodeHistoryBySeries = history
            .filter { item ->
                item.contentType == WatchHistoryContentType.EPISODE &&
                    !item.seriesId.isNullOrBlank()
            }
            .groupBy { item -> item.seriesId!! }

        val sortedSeriesEntries = episodeHistoryBySeries.entries
            .sortedByDescending { entry -> entry.value.maxOf { it.lastWatchedAt } }

        for ((seriesId, items) in sortedSeriesEntries) {
            if (inProgress.size + nextUp.size >= limit) break
            if (!seenSeriesIds.add(seriesId)) continue

            val latest = items.maxByOrNull { it.lastWatchedAt } ?: continue
            if (WatchHistoryResumePolicy.isContinueWatching(latest.positionMs, latest.durationMs)) continue
            if (!WatchHistoryResumePolicy.isNearEnd(latest.positionMs, latest.durationMs)) continue

            val currentEpisode = catalog.getEpisode(sourceId, latest.contentId)
                ?: catalog.getEpisode(sourceId, latest.contentId, seriesId)
                ?: continue

            val nextEpisode = catalog.nextEpisode(sourceId, currentEpisode) ?: continue
            val series = catalog.getSeries(sourceId, seriesId)

            nextUp.add(
                WatchHistoryItem(
                    id = "next-up:$seriesId:${nextEpisode.id}",
                    profileId = latest.profileId,
                    sourceId = sourceId,
                    contentId = nextEpisode.id,
                    contentType = WatchHistoryContentType.EPISODE,
                    seriesId = seriesId,
                    title = nextEpisode.title,
                    posterUrl = series?.posterUrl ?: nextEpisode.thumbnailUrl,
                    positionMs = 0L,
                    durationMs = nextEpisode.durationMinutes?.times(60_000L)?.toLong(),
                    lastWatchedAt = latest.lastWatchedAt,
                ),
            )
        }

        return (inProgress + nextUp).take(limit)
    }
}
