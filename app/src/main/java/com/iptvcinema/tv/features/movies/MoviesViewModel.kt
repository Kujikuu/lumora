package com.iptvcinema.tv.features.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogSortOption
import com.iptvcinema.tv.core.catalog.sortedByCatalogOption
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toPosterCardData
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.home.HomeCardAction
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.toFavoriteContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MoviesUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val categories: List<String> = emptyList(),
    val movies: List<MovieItem> = emptyList(),
    val featured: MovieItem? = null,
    val continueWatchingMovies: List<HomeContentCard> = emptyList(),
    val posters: List<PosterCardData> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
    val sortOption: CatalogSortOption = CatalogSortOption.TITLE_AZ,
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val catalogRefreshController: CatalogRefreshController,
    private val catalogSyncProgressTracker: CatalogSyncProgressTracker,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedSort = MutableStateFlow(CatalogSortOption.TITLE_AZ)
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
                selectedSort,
            ) { session, categoryName, sortOption ->
                Triple(session, categoryName, sortOption)
            }.flatMapLatest { (session, categoryName, sortOption) ->
                val profileId = session.currentProfileId
                val continueWatchingEnabled = runCatching {
                    userSettingsRepository.getSettings()?.continueWatchingEnabled
                }.getOrNull() ?: true
                val controlsFlow = profileId?.let { parentalControlsRepository.observeControls(it) }
                    ?: flowOf(null)
                val historyFlow = if (profileId != null && continueWatchingEnabled) {
                    watchHistoryRepository.observeContinueWatching(profileId, limit = 10)
                } else {
                    flowOf(emptyList())
                }
                val favoritesFlow = if (profileId != null) {
                    favoritesRepository.observeFavorites(profileId)
                } else {
                    flowOf(emptyList())
                }
                combine(
                    catalogRepository.observeMovies(categoryName),
                    catalogRepository.observeFeaturedMovie(),
                    controlsFlow,
                    historyFlow,
                    favoritesFlow,
                ) { browseState, featuredMovie, controls, history, favorites ->
                    MoviesFlowBundle(
                        session = session,
                        browseState = browseState,
                        featuredMovie = featuredMovie,
                        controls = controls,
                        history = history,
                        favorites = favorites,
                        continueWatchingEnabled = continueWatchingEnabled,
                        sortOption = sortOption,
                    )
                }
            }.collect { bundle ->
                val state = bundle.browseState
                val controls = bundle.controls
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
                val featured = bundle.featuredMovie?.takeUnless { movie ->
                    controls != null &&
                        parentalGate.isContentBlocked(
                            movie.genres.firstOrNull(),
                            movie.rating,
                            controls,
                        )
                }
                val gridMovies = (if (featured == null) {
                    movies
                } else {
                    movies.filter { it.id != featured.id }
                }                ).sortedByCatalogOption(
                    option = bundle.sortOption,
                    titleSelector = { it.title },
                    yearSelector = { it.year },
                    sortOrderSelector = { it.sortOrder },
                    addedAtSelector = { it.addedAt },
                )
                val continueWatchingMovies = if (bundle.continueWatchingEnabled) {
                    bundle.history.mapNotNull { item ->
                        if (item.contentType != WatchHistoryContentType.MOVIE) return@mapNotNull null
                        val progress = item.durationMs?.takeIf { it > 0 }?.let { duration ->
                            (item.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        }
                        val display = catalogRepository.resolveWatchHistoryCardDisplay(
                            sourceId = bundle.session.currentSourceId,
                            item = item,
                            isDemoMode = bundle.session.isDemoMode,
                        )
                        HomeContentCard(
                            contentId = item.contentId,
                            contentType = "movie",
                            title = display.title,
                            subtitle = display.subtitle,
                            imageUrl = display.posterUrl,
                            progress = progress,
                            isFavorite = bundle.favorites.isFavorite(item.contentId, FavoriteContentType.MOVIE),
                            primaryAction = HomeCardAction.ContinueWatching,
                        )
                    }
                } else {
                    emptyList()
                }
                val current = _uiState.value
                _uiState.value = MoviesUiState(
                    loadState = state.loadState,
                    categories = filteredCategories,
                    movies = movies,
                    featured = featured,
                    continueWatchingMovies = continueWatchingMovies,
                    posters = gridMovies.map { it.toPosterCardData() },
                    message = state.message,
                    sourceStatus = state.sourceStatus,
                    sourceType = state.sourceType,
                    syncBannerText = current.syncBannerText,
                    refreshState = current.refreshState,
                    sortOption = bundle.sortOption,
                )
            }
        }
    }

    fun selectSort(option: CatalogSortOption) {
        selectedSort.value = option
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
            catalogSyncProgressTracker = catalogSyncProgressTracker,
            appSessionRepository = appSessionRepository,
        )
    }

    fun togglePosterFavorite(movieId: String) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            val session = appSessionRepository.sessionState.first()
            val movie = _uiState.value.movies.firstOrNull { it.id == movieId } ?: return@launch
            runCatching {
                favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = movieId,
                    contentType = FavoriteContentType.MOVIE,
                    title = movie.title,
                    posterUrl = movie.imageUrl,
                    sourceId = session.currentSourceId,
                )
            }
        }
    }

    fun toggleFavorite(card: HomeContentCard) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            val session = appSessionRepository.sessionState.first()
            runCatching {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = card.contentId,
                    contentType = card.toFavoriteContentType(),
                    title = card.title,
                    posterUrl = card.imageUrl,
                    sourceId = session.currentSourceId,
                )
                updateCardFavoriteState(card.contentId, card.toFavoriteContentType(), isFavorite)
            }
        }
    }

    private fun updateCardFavoriteState(
        contentId: String,
        contentType: FavoriteContentType,
        isFavorite: Boolean,
    ) {
        val state = _uiState.value
        _uiState.value = state.copy(
            continueWatchingMovies = state.continueWatchingMovies.map { card ->
                if (card.contentId == contentId && card.toFavoriteContentType() == contentType) {
                    card.copy(isFavorite = isFavorite)
                } else {
                    card
                }
            },
        )
    }

    private fun List<FavoriteItem>.isFavorite(contentId: String, contentType: FavoriteContentType): Boolean =
        any { it.contentId == contentId && it.contentType == contentType }

    private data class MoviesFlowBundle(
        val session: com.iptvcinema.tv.core.datastore.AppSessionState,
        val browseState: com.iptvcinema.tv.core.data.repository.CatalogBrowseState<MovieItem>,
        val featuredMovie: MovieItem?,
        val controls: com.iptvcinema.tv.core.model.ParentalControls?,
        val history: List<com.iptvcinema.tv.core.model.WatchHistoryItem>,
        val favorites: List<FavoriteItem>,
        val continueWatchingEnabled: Boolean,
        val sortOption: CatalogSortOption,
    )
}
