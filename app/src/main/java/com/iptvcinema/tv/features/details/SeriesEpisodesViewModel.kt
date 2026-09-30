package com.iptvcinema.tv.features.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.features.home.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SeriesEpisodesUiState(
    val loadState: DetailsLoadState = DetailsLoadState.Loading,
    val seriesTitle: String = "",
    val sections: List<HomeSection> = emptyList(),
    val resumeFocus: Pair<String, Int>? = null,
    val playbackBlocked: Boolean = false,
    val message: String? = null,
)

/**
 * Episodes of one series, one rail per season, with watched state. Follows watch history, so
 * progress updates when the viewer comes back from the player.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesEpisodesViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val catalogRepository: CatalogRepository,
    private val episodeCatalogRepository: EpisodeCatalogRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val seriesId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(SeriesEpisodesUiState())
    val uiState: StateFlow<SeriesEpisodesUiState> = _uiState.asStateFlow()

    private val labels by lazy {
        EpisodeLabels(
            seasonTitle = { season, count -> appStrings.get(R.string.details_season_episodes_count, season, count) },
            episodeTitle = { number -> appStrings.get(R.string.details_episode_label, number) },
            watched = appStrings.get(R.string.player_episode_watched),
        )
    }

    init {
        viewModelScope.launch {
            seriesId.filterNotNull().flatMapLatest { id ->
                flow {
                    val loaded = loadSeries(id)
                    if (loaded == null) {
                        emit(null)
                        return@flow
                    }
                    val historyFlow = loaded.profileId
                        ?.let { watchHistoryRepository.observeHistory(it, HISTORY_LIMIT) }
                        ?: flowOf(emptyList())
                    historyFlow.map { history ->
                        val content = SeriesEpisodesBuilder.build(loaded.series, loaded.episodes, history, labels)
                        SeriesEpisodesUiState(
                            loadState = if (content.sections.isEmpty()) DetailsLoadState.Error else DetailsLoadState.Ready,
                            seriesTitle = loaded.series.title,
                            sections = content.sections,
                            resumeFocus = content.resumeFocus,
                            playbackBlocked = loaded.playbackBlocked,
                            message = appStrings.get(R.string.details_no_episodes).takeIf { content.sections.isEmpty() },
                        )
                    }.collect { emit(it) }
                }
            }.collect { state ->
                _uiState.value = state ?: SeriesEpisodesUiState(
                    loadState = DetailsLoadState.Error,
                    message = appStrings.get(R.string.error_series_not_found),
                )
            }
        }
    }

    fun load(id: String) {
        seriesId.value = id
    }

    fun refreshWatchState() {
        watchHistoryRepository.invalidate()
    }

    private suspend fun loadSeries(id: String): LoadedSeries? = try {
        val session = appSessionRepository.sessionState.first()
        if (session.isDemoMode) {
            demoSeries(id)
        } else {
            val sourceId = session.currentSourceId ?: return null
            val series = catalogRepository.getSeries(sourceId, id) ?: return null
            // Details has just refreshed the episodes; the cached list is current.
            val episodes = episodeCatalogRepository.getSeriesEpisodeCatalog(sourceId, id, forceRefresh = false).episodes
            val controls = session.currentProfileId?.let { parentalControlsRepository.getControls(it) }
            LoadedSeries(
                series = series,
                episodes = episodes,
                profileId = session.currentProfileId,
                playbackBlocked = controls != null &&
                    parentalGate.isContentBlocked(series.categoryName, series.rating, controls),
            )
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Log.w(TAG, "Episodes failed to load: ${error.safeSummary()}")
        null
    }

    private suspend fun demoSeries(id: String): LoadedSeries? {
        val series = FakeDataProvider.seriesById(id) ?: return null
        val episodes = series.seasons.flatMap { season ->
            season.episodes.map { episode ->
                CatalogEpisode(
                    id = episode.id,
                    sourceId = DEMO_SOURCE_ID,
                    seriesId = series.id,
                    seasonNumber = season.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    title = episode.title,
                    streamUrl = "",
                    durationMinutes = episode.durationMinutes,
                    plot = null,
                    thumbnailUrl = episode.thumbnailUrl,
                )
            }
        }
        val catalogSeries = CatalogSeries(
            id = series.id,
            sourceId = DEMO_SOURCE_ID,
            title = series.title,
            posterUrl = series.imageUrl,
            backdropUrl = series.backdropUrl,
            categoryId = null,
            categoryName = series.genres.firstOrNull(),
            plot = series.plot,
            rating = series.rating,
            year = series.year,
        )
        return LoadedSeries(catalogSeries, episodes, appSessionRepository.sessionState.first().currentProfileId, false)
    }

    private data class LoadedSeries(
        val series: CatalogSeries,
        val episodes: List<CatalogEpisode>,
        val profileId: String?,
        val playbackBlocked: Boolean,
    )

    private companion object {
        const val TAG = "SeriesEpisodes"
        const val DEMO_SOURCE_ID = "demo"
        // Long series need more than the usual 50 rows to show every watched episode.
        const val HISTORY_LIMIT = 500
    }
}
