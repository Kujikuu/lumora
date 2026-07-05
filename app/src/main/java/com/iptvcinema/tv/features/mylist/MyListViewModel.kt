package com.iptvcinema.tv.features.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.util.AppStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface MyListUiState {
    data object Loading : MyListUiState
    data class Ready(
        val favorites: List<FavoriteItem>,
        val recentlyWatched: List<WatchHistoryItem>,
        val recentlyWatchedPosters: List<PosterCardData> = emptyList(),
    ) : MyListUiState
    data class Error(val message: String) : MyListUiState
}

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val catalogRepository: CatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MyListUiState>(MyListUiState.Loading)
    val uiState: StateFlow<MyListUiState> = _uiState.asStateFlow()

    init {
        loadMyList()
    }

    fun loadMyList() {
        viewModelScope.launch {
            _uiState.value = MyListUiState.Loading
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId
            if (profileId == null) {
                _uiState.value = MyListUiState.Ready(emptyList(), emptyList(), emptyList())
                return@launch
            }
            runCatching {
                val favorites = favoritesRepository.observeFavorites(profileId).first()
                val history = watchHistoryRepository.observeHistory(profileId, limit = 4).first()
                val recentlyWatchedPosters = history.map { item ->
                    val display = catalogRepository.resolveWatchHistoryCardDisplay(
                        sourceId = session.currentSourceId,
                        item = item,
                        isDemoMode = session.isDemoMode,
                    )
                    val progress = item.durationMs?.takeIf { it > 0 }?.let { duration ->
                        (item.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    }
                    PosterCardData(
                        title = display.title,
                        subtitle = display.subtitle,
                        imageUrl = display.posterUrl,
                        progress = progress,
                        contentId = item.contentId,
                    )
                }
                _uiState.value = MyListUiState.Ready(favorites, history, recentlyWatchedPosters)
            }.onFailure { error ->
                _uiState.value = MyListUiState.Error(error.message ?: appStrings.get(R.string.error_load_list))
            }
        }
    }

    fun removeFavorite(favorite: FavoriteItem) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            runCatching {
                favoritesRepository.removeFavorite(profileId, favorite)
                val current = _uiState.value
                if (current is MyListUiState.Ready) {
                    _uiState.value = current.copy(
                        favorites = current.favorites.filterNot {
                            it.contentId == favorite.contentId && it.contentType == favorite.contentType
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.value = MyListUiState.Error(error.message ?: appStrings.get(R.string.error_load_list))
            }
        }
    }
}

fun FavoriteItem.toPosterCardData(): PosterCardData = PosterCardData(
    title = title,
    year = contentType.name.replace('_', ' '),
    imageUrl = posterUrl,
    isFavorite = true,
    contentId = contentId,
)

fun WatchHistoryItem.toPosterCardData(): PosterCardData {
    val progress = durationMs?.takeIf { it > 0 }?.let { duration ->
        (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
    return PosterCardData(
        title = title,
        year = contentType.name.replace('_', ' '),
        imageUrl = posterUrl,
        progress = progress,
        contentId = contentId,
    )
}
