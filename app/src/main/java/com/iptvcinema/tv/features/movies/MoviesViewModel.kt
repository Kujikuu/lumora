package com.iptvcinema.tv.features.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toPosterCardData
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class MoviesUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val categories: List<String> = emptyList(),
    val movies: List<MovieItem> = emptyList(),
    val featured: MovieItem? = null,
    val posters: List<PosterCardData> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val catalogRefreshController: CatalogRefreshController,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        CatalogRefreshSupport.observeSyncBanner(viewModelScope, catalogRepository) { banner ->
            _uiState.value = _uiState.value.copy(syncBannerText = banner)
        }
        viewModelScope.launch {
            combine(
                appSessionRepository.sessionState,
                selectedCategory,
            ) { session, categoryName ->
                session to categoryName
            }.flatMapLatest { (session, categoryName) ->
                val controlsFlow = session.currentProfileId?.let { profileId ->
                    parentalControlsRepository.observeControls(profileId)
                } ?: flowOf(null)
                combine(
                    catalogRepository.observeMovies(categoryName),
                    catalogRepository.observeFeaturedMovie(),
                    controlsFlow,
                ) { browseState, featuredMovie, controls ->
                    Triple(browseState, featuredMovie, controls)
                }
            }.collect { (state, featuredMovie, controls) ->
                val filteredCategories = if (controls != null) {
                    parentalGate.filterCategoryNames(state.categories, controls)
                } else {
                    state.categories
                }
                val movies = if (controls != null) {
                    state.items.filter { movie ->
                        !parentalGate.isContentBlocked(
                            movie.genres.firstOrNull(),
                            movie.rating,
                            controls,
                        )
                    }
                } else {
                    state.items
                }
                val featured = featuredMovie?.takeUnless { movie ->
                    controls != null &&
                        parentalGate.isContentBlocked(
                            movie.genres.firstOrNull(),
                            movie.rating,
                            controls,
                        )
                }
                val gridMovies = if (featured == null) {
                    movies
                } else {
                    movies.filter { it.id != featured.id }
                }
                val current = _uiState.value
                _uiState.value = MoviesUiState(
                    loadState = state.loadState,
                    categories = filteredCategories,
                    movies = movies,
                    featured = featured,
                    posters = gridMovies.map { movie -> movie.toPosterCardData() },
                    message = state.message,
                    sourceStatus = state.sourceStatus,
                    sourceType = state.sourceType,
                    syncBannerText = current.syncBannerText,
                    refreshState = current.refreshState,
                )
            }
        }
    }

    fun selectCategory(categoryName: String?) {
        selectedCategory.value = categoryName
    }

    fun refreshCurrentSource() {
        CatalogRefreshSupport.runCatalogRefresh(
            scope = viewModelScope,
            getRefreshState = { _uiState.value.refreshState },
            setRefreshState = { refreshState ->
                _uiState.value = _uiState.value.copy(refreshState = refreshState)
            },
            catalogRefreshController = catalogRefreshController,
        )
    }

    private fun MovieItem.toPosterCardData(): PosterCardData =
        CatalogMovie(
            id = id,
            sourceId = "",
            title = title,
            streamUrl = "",
            posterUrl = imageUrl,
            backdropUrl = backdropUrl,
            categoryId = null,
            categoryName = genres.firstOrNull(),
            year = year,
            durationMinutes = runtimeMinutes,
            rating = rating.toString(),
            plot = plot,
            genres = genres,
        ).toPosterCardData()
}
