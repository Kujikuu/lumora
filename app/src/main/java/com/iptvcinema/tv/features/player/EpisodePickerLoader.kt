package com.iptvcinema.tv.features.player

import com.iptvcinema.tv.core.catalog.SeasonGrouping
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode

internal suspend fun loadEpisodePickerSeasons(
    profileId: String?,
    sourceId: String,
    seriesId: String,
    loadEpisodes: suspend (String, String) -> List<CatalogEpisode>,
    loadHistory: suspend (String, String, String) -> List<WatchHistoryItem>,
): List<SeasonItem> {
    val episodes = runCatching { loadEpisodes(sourceId, seriesId) }.getOrDefault(emptyList())
    val seasons = SeasonGrouping.toSeasonItems(episodes, seriesId)
    if (profileId.isNullOrBlank() || seasons.isEmpty()) return seasons

    val history = runCatching { loadHistory(profileId, sourceId, seriesId) }.getOrDefault(emptyList())
    return EpisodePickerWatchStateMapper.apply(seasons, history, sourceId, seriesId)
}
