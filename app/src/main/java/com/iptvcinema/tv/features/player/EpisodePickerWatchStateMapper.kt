package com.iptvcinema.tv.features.player

import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.player.EpisodeWatchProgress

internal object EpisodePickerWatchStateMapper {
    fun apply(
        seasons: List<SeasonItem>,
        history: List<WatchHistoryItem>,
        sourceId: String,
        seriesId: String,
    ): List<SeasonItem> {
        val catalogIds = seasons.flatMap { it.episodes }.mapTo(mutableSetOf()) { it.id }
        val latestByEpisodeId = history.asSequence()
            .filter { it.contentType == WatchHistoryContentType.EPISODE }
            .filter { it.sourceId == sourceId && it.seriesId == seriesId }
            .filter { it.contentId in catalogIds }
            .groupBy { it.contentId }
            .mapValues { (_, rows) -> rows.maxBy { it.lastWatchedAt } }
        val progressByEpisodeId = latestByEpisodeId.mapNotNull { (episodeId, item) ->
            EpisodeWatchProgress.normalized(item.positionMs, item.durationMs)
                ?.let { progress -> episodeId to progress }
        }.toMap()

        return seasons.map { season ->
            season.copy(episodes = season.episodes.map { episode ->
                episode.copy(progress = progressByEpisodeId[episode.id])
            })
        }
    }
}
