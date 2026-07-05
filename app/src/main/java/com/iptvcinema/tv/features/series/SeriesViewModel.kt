package com.iptvcinema.tv.features.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
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

data class SeriesUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val categories: List<String> = emptyList(),
    val series: List<SeriesItem> = emptyList(),
    val featured: SeriesItem? = null,
    val continueSeries: List<HomeContentCard> = emptyList(),
    val posters: List<PosterCardData> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
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
    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

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
                    catalogRepository.observeSeries(categoryName),
                    controlsFlow,
                    historyFlow,
                    favoritesFlow,
                ) { browseState, controls, history, favorites ->
                    SeriesFlowBundle(
                        session = session,
                        browseState = browseState,
                        controls = controls,
                        history = history,
                        favorites = favorites,
                        continueWatchingEnabled = continueWatchingEnabled,
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
                val seriesItems = if (controls != null) {
                    state.items.filter { series ->
                        !parentalGate.isContentBlocked(
                            series.genres.firstOrNull(),
                            series.rating,
                            controls,
                        )
                    }
                } else {
                    state.items
                }
                val featured = seriesItems.firstOrNull()
                val gridSeries = if (featured == null) {
                    seriesItems
                } else {
                    seriesItems.filter { it.id != featured.id }
                }
                val continueSeries = if (bundle.continueWatchingEnabled) {
                    bundle.history.mapNotNull { item ->
                        if (item.contentType != WatchHistoryContentType.EPISODE) return@mapNotNull null
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
                            contentType = "episode",
                            seriesId = item.seriesId,
                            title = display.title,
                            subtitle = display.subtitle,
                            imageUrl = display.posterUrl,
                            progress = progress,
                            isFavorite = bundle.favorites.isFavorite(item.contentId, FavoriteContentType.EPISODE),
                            primaryAction = HomeCardAction.ContinueWatching,
                        )
                    }
                } else {
                    emptyList()
                }
                val current = _uiState.value
                _uiState.value = SeriesUiState(
                    loadState = state.loadState,
                    categories = filteredCategories,
                    series = seriesItems,
                    featured = featured,
                    continueSeries = continueSeries,
                    posters = gridSeries.map { series ->
                        CatalogSeries(
                            id = series.id,
                            sourceId = "",
                            title = series.title,
                            posterUrl = series.imageUrl,
                            backdropUrl = series.backdropUrl,
                            categoryId = null,
                            categoryName = series.genres.firstOrNull(),
                            plot = series.plot,
                            rating = series.rating,
                            year = series.year,
                        ).toPosterCardData()
                    },
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
            catalogSyncProgressTracker = catalogSyncProgressTracker,
            appSessionRepository = appSessionRepository,
        )
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
            continueSeries = state.continueSeries.map { card ->
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

    private data class SeriesFlowBundle(
        val session: com.iptvcinema.tv.core.datastore.AppSessionState,
        val browseState: com.iptvcinema.tv.core.data.repository.CatalogBrowseState<SeriesItem>,
        val controls: com.iptvcinema.tv.core.model.ParentalControls?,
        val history: List<com.iptvcinema.tv.core.model.WatchHistoryItem>,
        val favorites: List<FavoriteItem>,
        val continueWatchingEnabled: Boolean,
    )
}
