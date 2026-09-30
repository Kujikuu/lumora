package com.iptvcinema.tv.features.catalog

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSortOption
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.util.safeSummary
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogGridUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val categories: List<String> = emptyList(),
    val posters: List<PosterCardData> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
    val sortOption: CatalogSortOption = CatalogSortOption.TITLE_AZ,
)

class CatalogGridDependencies @Inject constructor(
    val catalogRepository: CatalogRepository,
    val favoritesRepository: FavoritesRepository,
    val appSessionRepository: AppSessionRepository,
    val parentalControlsRepository: ParentalControlsRepository,
    val parentalGate: ParentalGate,
    val catalogRefreshController: CatalogRefreshController,
    val catalogSyncProgressTracker: CatalogSyncProgressTracker,
)

/**
 * The full catalog grid of one category, with category chips and sort.
 *
 * Each input has its own flow: changing the category re-queries only the catalog, changing the
 * sort only re-sorts, and parental controls and favorites follow the profile alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class CatalogGridViewModel<T : Any>(
    protected val deps: CatalogGridDependencies,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedSort = MutableStateFlow(CatalogSortOption.TITLE_AZ)
    private val _uiState = MutableStateFlow(CatalogGridUiState())
    val uiState: StateFlow<CatalogGridUiState> = _uiState.asStateFlow()

    // The latest catalog items, so a poster's favorite toggle can read its title and artwork.
    @Volatile
    private var currentItems: List<T> = emptyList()
    private val favoriteToggles = mutableSetOf<String>()

    protected abstract val favoriteType: FavoriteContentType
    protected abstract fun observeItems(categoryName: String?): Flow<CatalogBrowseState<T>>
    protected abstract fun T.itemId(): String
    protected abstract fun T.itemTitle(): String
    protected abstract fun T.itemPosterUrl(): String?
    protected abstract fun T.itemCategory(): String?
    protected abstract fun T.itemRating(): String?
    protected abstract fun List<T>.sortedBy(option: CatalogSortOption): List<T>
    protected abstract fun T.toPoster(isFavorite: Boolean): PosterCardData

    protected fun start() {
        CatalogRefreshSupport.observeSyncBanner(viewModelScope, deps.catalogRepository) { banner ->
            _uiState.update { it.copy(syncBannerText = banner) }
        }
        val profileId = deps.appSessionRepository.sessionState.map { it.currentProfileId }.distinctUntilChanged()
        val controlsFlow = profileId.flatMapLatest { id ->
            id?.let { deps.parentalControlsRepository.observeControls(it) } ?: flowOf(null)
        }
        val favoritesFlow = profileId.flatMapLatest { id ->
            id?.let { deps.favoritesRepository.observeFavorites(it) } ?: flowOf(emptyList())
        }
        val itemsFlow = selectedCategory.flatMapLatest { observeItems(it) }
        viewModelScope.launch {
            combine(itemsFlow, selectedSort, controlsFlow, favoritesFlow, selectedCategory) {
                    state, sort, controls, favorites, category ->
                toUiState(state, sort, controls, favorites, category)
            }
                // Mapping, filtering and sorting up to 2,000 titles stays off the main thread.
                .flowOn(Dispatchers.Default)
                .collect { next ->
                _uiState.update { current ->
                    next.copy(syncBannerText = current.syncBannerText, refreshState = current.refreshState)
                }
            }
        }
    }

    private fun toUiState(
        state: CatalogBrowseState<T>,
        sort: CatalogSortOption,
        controls: ParentalControls?,
        favorites: List<FavoriteItem>,
        selectedCategory: String?,
    ): CatalogGridUiState {
        val gate = deps.parentalGate
        val categories = if (controls == null) {
            state.categories
        } else {
            state.categories.filterNot { gate.isCategoryBlocked(it, controls) }
        }
        val items = if (controls == null) {
            state.items
        } else {
            state.items.filterNot { item ->
                gate.isContentBlocked(item.itemCategory() ?: selectedCategory, item.itemRating(), controls)
            }
        }
        currentItems = items
        val favoriteIds = favorites.filter { it.contentType == favoriteType }.mapTo(mutableSetOf()) { it.contentId }
        return CatalogGridUiState(
            loadState = state.loadState,
            categories = categories,
            posters = items.sortedBy(sort).map { it.toPoster(isFavorite = it.itemId() in favoriteIds) },
            message = state.message,
            sourceStatus = state.sourceStatus,
            sourceType = state.sourceType,
            sortOption = sort,
        )
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
            setRefreshState = { refreshState -> _uiState.update { it.copy(refreshState = refreshState) } },
            catalogRefreshController = deps.catalogRefreshController,
            catalogSyncProgressTracker = deps.catalogSyncProgressTracker,
            appSessionRepository = deps.appSessionRepository,
        )
    }

    /** Adds or removes a poster from My List. A second press while the first is saving is ignored. */
    fun togglePosterFavorite(poster: PosterCardData) {
        val id = poster.contentId ?: return
        val item = currentItems.firstOrNull { it.itemId() == id } ?: return
        if (!favoriteToggles.add(id)) return
        viewModelScope.launch {
            try {
                val session = deps.appSessionRepository.sessionState.first()
                val profileId = session.currentProfileId ?: return@launch
                deps.favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = id,
                    contentType = favoriteType,
                    title = item.itemTitle(),
                    posterUrl = item.itemPosterUrl(),
                    sourceId = session.currentSourceId,
                    currentlyFavorite = poster.isFavorite,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Favorite toggle failed: ${error.safeSummary()}")
            } finally {
                favoriteToggles.remove(id)
            }
        }
    }

    private companion object {
        const val TAG = "CatalogGrid"
    }
}
