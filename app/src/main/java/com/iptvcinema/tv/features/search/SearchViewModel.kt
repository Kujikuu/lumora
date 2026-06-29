package com.iptvcinema.tv.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelTileData
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.CatalogSearchFilter
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.RecentSearchRepository
import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.util.AppStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedFilterIndex: Int = 0,
    val loadState: CatalogLoadState = CatalogLoadState.Ready,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val movieResults: List<PosterCardData> = emptyList(),
    val seriesResults: List<PosterCardData> = emptyList(),
    val channelResults: List<ChannelTileData> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val message: String? = null,
) {
    val hasResults: Boolean =
        movieResults.isNotEmpty() || seriesResults.isNotEmpty() || channelResults.isNotEmpty()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val recentSearchRepository: RecentSearchRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            catalogRepository.observeSourceMeta().collect { (status, type) ->
                _uiState.value = _uiState.value.copy(sourceStatus = status, sourceType = type)
            }
        }
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId
            val recent = if (profileId != null) {
                recentSearchRepository.getRecentSearches(profileId)
            } else {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(recentSearches = recent)
        }
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        scheduleSearch()
    }

    fun selectFilter(index: Int) {
        _uiState.value = _uiState.value.copy(selectedFilterIndex = index.coerceIn(0, SEARCH_FILTERS.lastIndex))
        scheduleSearch()
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query = "",
            loadState = CatalogLoadState.Ready,
            movieResults = emptyList(),
            seriesResults = emptyList(),
            channelResults = emptyList(),
            message = null,
        )
    }

    fun applyRecentSearch(term: String) {
        updateQuery(term)
    }

    fun retry() = scheduleSearch(immediate = true)

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val query = _uiState.value.query.trim()
            if (query.length < 2) {
                val session = appSessionRepository.sessionState.first()
                val noSource = !session.isDemoMode && session.currentSourceId == null
                _uiState.value = _uiState.value.copy(
                    loadState = if (noSource) CatalogLoadState.Empty else CatalogLoadState.Ready,
                    movieResults = emptyList(),
                    seriesResults = emptyList(),
                    channelResults = emptyList(),
                    message = if (noSource) appStrings.get(R.string.msg_no_source_connected) else null,
                )
                return@launch
            }
            val session = appSessionRepository.sessionState.first()
            if (!session.isDemoMode && session.currentSourceId == null) {
                _uiState.value = _uiState.value.copy(
                    loadState = CatalogLoadState.Empty,
                    message = appStrings.get(R.string.msg_no_source_connected),
                    movieResults = emptyList(),
                    seriesResults = emptyList(),
                    channelResults = emptyList(),
                )
                return@launch
            }
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            _uiState.value = _uiState.value.copy(loadState = CatalogLoadState.Loading, message = null)
            runCatching {
                val filter = filterAt(_uiState.value.selectedFilterIndex)
                val results = catalogRepository.searchCatalog(query, filter)
                val session = appSessionRepository.sessionState.first()
                val profileId = session.currentProfileId
                val controls = profileId?.let { parentalControlsRepository.getControls(it) }
                val filteredMovies = results.movies.filter { movie ->
                    controls == null || !parentalGate.isContentBlocked(
                        movie.genres.firstOrNull(),
                        movie.rating,
                        controls,
                    )
                }
                val filteredSeries = results.series.filter { series ->
                    controls == null || !parentalGate.isContentBlocked(
                        series.genres.firstOrNull(),
                        series.rating,
                        controls,
                    )
                }
                val filteredChannels = results.channels.filter { channel ->
                    controls == null || !parentalGate.isCategoryBlocked(channel.category, controls)
                }
                if (profileId != null) {
                    recentSearchRepository.addRecentSearch(profileId, query)
                    val recent = recentSearchRepository.getRecentSearches(profileId)
                    _uiState.value = _uiState.value.copy(recentSearches = recent)
                }
                _uiState.value = _uiState.value.copy(
                    loadState = CatalogLoadState.Ready,
                    movieResults = filteredMovies.map { movie ->
                        PosterCardData(
                            title = movie.title,
                            year = movie.year.takeIf { it > 0 }?.toString(),
                            runtime = movie.runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
                            imageUrl = movie.imageUrl,
                            contentId = movie.id,
                        )
                    },
                    seriesResults = filteredSeries.map { series ->
                        PosterCardData(
                            title = series.title,
                            year = series.year.takeIf { it > 0 }?.toString(),
                            imageUrl = series.imageUrl,
                            contentId = series.id,
                        )
                    },
                    channelResults = filteredChannels.map { channel -> channel.toChannelTileData() },
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    loadState = CatalogLoadState.Error,
                    message = error.message ?: appStrings.get(R.string.error_search_failed),
                )
            }
        }
    }

    private fun filterAt(index: Int): CatalogSearchFilter =
        SEARCH_FILTERS[index.coerceIn(0, SEARCH_FILTERS.lastIndex)]

    companion object {
        private val SEARCH_FILTERS = listOf(
            CatalogSearchFilter.All,
            CatalogSearchFilter.Movies,
            CatalogSearchFilter.Series,
            CatalogSearchFilter.Live,
        )
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
