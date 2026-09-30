package com.iptvcinema.tv.features.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.BrowseContentRepository
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.datastore.RecentSearchRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.isCategory
import com.iptvcinema.tv.core.model.home.toFavoriteContentType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.withFavorite
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    /** Source problems (none connected, expired) replace the whole screen; see [CatalogLoadState]. */
    val loadState: CatalogLoadState = CatalogLoadState.Ready,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val isSearching: Boolean = false,
    /** Results for the query, or suggestions while it is shorter than [SearchViewModel.MIN_QUERY_LENGTH]. */
    val sections: List<HomeSection> = emptyList(),
    /** Shown under the keyboard when a search found nothing or failed. */
    val emptyMessage: String? = null,
    val message: String? = null,
) {
    val hasQuery: Boolean get() = query.trim().length >= SearchViewModel.MIN_QUERY_LENGTH
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val browseContentRepository: BrowseContentRepository,
    private val recentSearchRepository: RecentSearchRepository,
    private val appSessionRepository: AppSessionRepository,
    private val favoritesRepository: FavoritesRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var suggestions: List<HomeSection> = emptyList()

    init {
        viewModelScope.launch {
            catalogRepository.observeSourceMeta().collect { (status, type) ->
                _uiState.update { it.copy(sourceStatus = status, sourceType = type) }
            }
        }
        viewModelScope.launch {
            val session = appSessionRepository.sessionState.first()
            if (!session.isDemoMode && session.currentSourceId == null) {
                _uiState.update {
                    it.copy(loadState = CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_source_connected))
                }
                return@launch
            }
            loadSuggestions(session)
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                query = query,
                // Back under the minimum length: suggestions again, straight away.
                sections = if (query.trim().length < MIN_QUERY_LENGTH) suggestions else state.sections,
                emptyMessage = if (query.trim().length < MIN_QUERY_LENGTH) null else state.emptyMessage,
            )
        }
        scheduleSearch()
    }

    fun appendToQuery(text: String) = updateQuery(_uiState.value.query + text)

    fun deleteLastCharacter() {
        val query = _uiState.value.query
        if (query.isNotEmpty()) updateQuery(query.dropLast(1))
    }

    fun clearSearch() = updateQuery("")

    fun applyRecentSearch(term: String) = updateQuery(term)

    fun retry() = scheduleSearch(immediate = true)

    fun toggleFavorite(card: HomeContentCard) {
        if (card.isCategory) return
        viewModelScope.launch {
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return@launch
            val type = card.toFavoriteContentType()
            runCatchingNonCancellation("Favorite toggle") {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = card.contentId,
                    contentType = type,
                    title = card.title,
                    posterUrl = card.imageUrl,
                    sourceId = session.currentSourceId,
                    currentlyFavorite = card.isFavorite,
                )
                suggestions = suggestions.withFavorite(card.contentId, type, isFavorite)
                _uiState.update { it.copy(sections = it.sections.withFavorite(card.contentId, type, isFavorite)) }
            }
        }
    }

    private suspend fun loadSuggestions(session: AppSessionState) {
        runCatchingNonCancellation("Suggestions") {
            val (movies, series) = browseContentRepository.loadSearchSuggestions(session.currentSourceId, session.isDemoMode)
            val recent = session.currentProfileId?.let { recentSearchRepository.getRecentSearches(it, RECENT_LIMIT) }.orEmpty()
            val controls = controls(session)
            val favorites = favorites(session)
            suggestions = SearchSectionsBuilder.suggestions(
                recentSearches = recent,
                topMovies = movies,
                topSeries = series,
                isBlocked = blockedBy(controls),
                isFavorite = favoriteLookup(favorites),
            )
            _uiState.update { state -> if (state.hasQuery) state else state.copy(sections = suggestions) }
        }
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        val query = _uiState.value.query.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(isSearching = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            val session = appSessionRepository.sessionState.first()
            try {
                val results = catalogRepository.searchCatalog(query)
                val sections = SearchSectionsBuilder.results(
                    movies = results.movies,
                    series = results.series,
                    channels = results.channels,
                    isBlocked = blockedBy(controls(session)),
                    isFavorite = favoriteLookup(favorites(session)),
                )
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        sections = sections,
                        emptyMessage = appStrings.get(R.string.search_no_results).takeIf { sections.isEmpty() },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Search failed: ${error.safeSummary()}")
                _uiState.update {
                    it.copy(isSearching = false, sections = emptyList(), emptyMessage = appStrings.get(R.string.search_error))
                }
            }
        }
    }

    /**
     * Saves the query once the viewer opens one of its results, so Recent searches holds what
     * led somewhere, not every partial query typed on the way.
     */
    fun onResultOpened() {
        val query = _uiState.value.query.trim()
        if (query.length < MIN_QUERY_LENGTH) return
        viewModelScope.launch {
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return@launch
            runCatchingNonCancellation("Saving the search") {
                recentSearchRepository.addRecentSearch(profileId, query)
                loadSuggestions(session)
            }
        }
    }

    private suspend fun controls(session: AppSessionState): ParentalControls? =
        session.currentProfileId?.let { parentalControlsRepository.getControls(it) }

    private suspend fun favorites(session: AppSessionState): List<FavoriteItem> =
        session.currentProfileId?.let { favoritesRepository.observeFavorites(it).first() }.orEmpty()

    private fun blockedBy(controls: ParentalControls?): (String?, String?) -> Boolean = { category, rating ->
        controls != null && parentalGate.isContentBlocked(category, rating, controls)
    }

    private fun favoriteLookup(favorites: List<FavoriteItem>): (String, FavoriteContentType) -> Boolean = { id, type ->
        favorites.any { it.contentId == id && it.contentType == type }
    }

    private suspend fun runCatchingNonCancellation(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "$what failed: ${error.safeSummary()}")
        }
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        private const val TAG = "SearchViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val RECENT_LIMIT = 8
    }
}
