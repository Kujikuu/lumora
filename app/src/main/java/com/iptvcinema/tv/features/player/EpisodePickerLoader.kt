package com.iptvcinema.tv.features.player

import com.iptvcinema.tv.core.catalog.SeasonGrouping
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class EpisodePickerLoadCoordinator(
    private val scope: CoroutineScope,
) {
    private var loadJob: Job? = null
    private var generation: Long = 0L

    fun launch(
        onStart: () -> Unit = {},
        load: suspend () -> List<SeasonItem>,
        publish: (List<SeasonItem>) -> Unit,
    ): Job {
        val requestGeneration = ++generation
        loadJob?.cancel()
        onStart()
        return scope.launch {
            val seasons = load()
            if (isActive && generation == requestGeneration) {
                publish(seasons)
            }
        }.also { loadJob = it }
    }

    fun cancel() {
        generation++
        loadJob?.cancel()
        loadJob = null
    }
}

internal suspend fun loadEpisodePickerSeasons(
    profileId: String?,
    sourceId: String,
    seriesId: String,
    loadEpisodes: suspend (String, String) -> List<CatalogEpisode>,
    loadHistory: suspend (String, String, String) -> List<WatchHistoryItem>,
): List<SeasonItem> {
    val episodes = loadOrDefault(emptyList()) { loadEpisodes(sourceId, seriesId) }
    val seasons = SeasonGrouping.toSeasonItems(episodes, seriesId)
    if (profileId.isNullOrBlank() || seasons.isEmpty()) return seasons

    val history = loadOrDefault(emptyList()) { loadHistory(profileId, sourceId, seriesId) }
    return EpisodePickerWatchStateMapper.apply(seasons, history, sourceId, seriesId)
}

private suspend fun <T> loadOrDefault(
    defaultValue: T,
    load: suspend () -> T,
): T = try {
    load().also { currentCoroutineContext().ensureActive() }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (_: Exception) {
    defaultValue
}
