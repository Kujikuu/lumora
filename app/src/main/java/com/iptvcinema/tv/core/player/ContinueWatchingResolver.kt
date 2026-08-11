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
    suspend fun getEpisodesForSeries(sourceId: String, seriesId: String): List<CatalogEpisode>
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

    override suspend fun getEpisodesForSeries(sourceId: String, seriesId: String): List<CatalogEpisode> =
        catalogRepository.getEpisodesForSeries(sourceId, seriesId)

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
        val episodeHistoryBySeries = history
            .filter { item ->
                item.contentType == WatchHistoryContentType.EPISODE &&
                    !item.seriesId.isNullOrBlank()
            }
            .groupBy { item -> item.seriesId!! }
        val frontierEpisodeBySeries = episodeHistoryBySeries.mapValues { (seriesId, items) ->
            selectSeriesFrontier(sourceId, seriesId, items)
        }
        val frontierHistory = history.filter { item ->
            item.contentType != WatchHistoryContentType.EPISODE ||
                item.seriesId.isNullOrBlank() ||
                frontierEpisodeBySeries[item.seriesId]?.id == item.id
        }
        val inProgress = WatchHistoryResumePolicy.selectContinueWatching(frontierHistory, history.size)
        val restartAtBeginning = frontierEpisodeBySeries.values
            .filter(::shouldRestartAtBeginning)
            .map { item -> item.copy(positionMs = 0L) }
        val resumable = (inProgress + restartAtBeginning)
            .sortedByDescending { item -> item.lastWatchedAt }
            .take(limit)
        if (resumable.size >= limit || sourceId.isNullOrBlank()) return resumable

        val seenSeriesIds = resumable
            .filter { it.contentType == WatchHistoryContentType.EPISODE }
            .mapNotNull { item -> item.seriesId?.takeIf { it.isNotBlank() } }
            .toMutableSet()

        val nextUp = mutableListOf<WatchHistoryItem>()

        val sortedSeriesEntries = episodeHistoryBySeries.entries
            .sortedByDescending { entry -> entry.value.maxOf { it.lastWatchedAt } }

        for ((seriesId, items) in sortedSeriesEntries) {
            if (resumable.size + nextUp.size >= limit) break
            if (!seenSeriesIds.add(seriesId)) continue

            val frontier = frontierEpisodeBySeries[seriesId] ?: continue
            if (WatchHistoryResumePolicy.isContinueWatching(frontier.positionMs, frontier.durationMs)) continue
            if (!WatchHistoryResumePolicy.isNearEnd(frontier.positionMs, frontier.durationMs)) continue

            val currentEpisode = catalog.getEpisode(sourceId, frontier.contentId)
                ?: catalog.getEpisode(sourceId, frontier.contentId, seriesId)
                ?: continue

            val nextEpisode = catalog.nextEpisode(sourceId, currentEpisode) ?: continue
            val series = catalog.getSeries(sourceId, seriesId)
            val savedNextEpisode = items
                .filter { item -> item.contentId == nextEpisode.id }
                .maxByOrNull { item -> item.lastWatchedAt }
                ?.takeIf { item ->
                    WatchHistoryResumePolicy.isContinueWatching(item.positionMs, item.durationMs)
                }

            nextUp.add(
                savedNextEpisode?.copy(lastWatchedAt = frontier.lastWatchedAt) ?: WatchHistoryItem(
                    id = "next-up:$seriesId:${nextEpisode.id}",
                    profileId = frontier.profileId,
                    sourceId = sourceId,
                    contentId = nextEpisode.id,
                    contentType = WatchHistoryContentType.EPISODE,
                    seriesId = seriesId,
                    title = nextEpisode.title,
                    posterUrl = series?.posterUrl ?: nextEpisode.thumbnailUrl,
                    positionMs = 0L,
                    durationMs = nextEpisode.durationMinutes?.times(60_000L)?.toLong(),
                    lastWatchedAt = frontier.lastWatchedAt,
                ),
            )
        }

        return (resumable + nextUp).take(limit)
    }

    private fun shouldRestartAtBeginning(item: WatchHistoryItem): Boolean =
        item.durationMs?.let { it > 0L } == true &&
            item.positionMs in 0 until WatchHistoryResumePolicy.RESUME_MIN_MS

    private suspend fun selectSeriesFrontier(
        sourceId: String?,
        seriesId: String,
        history: List<WatchHistoryItem>,
    ): WatchHistoryItem {
        val mostRecent = history.maxBy { item -> item.lastWatchedAt }
        if (sourceId.isNullOrBlank()) return mostRecent

        val episodeById = runCatching {
            catalog.getEpisodesForSeries(sourceId, seriesId).associateBy { episode -> episode.id }
        }.getOrDefault(emptyMap())
        return history
            .mapNotNull { item -> episodeById[item.contentId]?.let { episode -> item to episode } }
            .maxWithOrNull(
                compareBy<Pair<WatchHistoryItem, CatalogEpisode>>(
                    { (_, episode) -> episode.seasonNumber },
                    { (_, episode) -> episode.episodeNumber },
                    { (item, _) -> item.lastWatchedAt },
                ),
            )
            ?.first
            ?: mostRecent
    }
}
