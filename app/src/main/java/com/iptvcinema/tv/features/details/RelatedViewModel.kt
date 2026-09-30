package com.iptvcinema.tv.features.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.BrowseContentRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.RelatedSnapshot
import com.iptvcinema.tv.core.model.home.toFavoriteContentType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.withFavorite
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RelatedUiState(
    val loadState: DetailsLoadState = DetailsLoadState.Loading,
    val anchorTitle: String = "",
    val sections: List<HomeSection> = emptyList(),
    val message: String? = null,
)

/** A "More like this" page for one movie or series. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RelatedViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val browseContentRepository: BrowseContentRepository,
    private val favoritesRepository: FavoritesRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val request = MutableStateFlow<Pair<RelatedKind, String>?>(null)
    private val _uiState = MutableStateFlow(RelatedUiState())
    val uiState: StateFlow<RelatedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            request.filterNotNull().flatMapLatest { (kind, id) ->
                flow {
                    val session = appSessionRepository.sessionState.first()
                    val snapshot = loadSnapshot(kind, id, session.currentSourceId, session.isDemoMode)
                    if (snapshot == null) {
                        emit(RelatedUiState(DetailsLoadState.Error, message = appStrings.get(notFoundMessage(kind))))
                        return@flow
                    }
                    val profileId = session.currentProfileId
                    val favoritesFlow = profileId?.let { favoritesRepository.observeFavorites(it) } ?: flowOf(emptyList())
                    val controlsFlow = profileId?.let { parentalControlsRepository.observeControls(it) } ?: flowOf(null)
                    emitAll(
                        combine(favoritesFlow, controlsFlow) { favorites, controls ->
                            val sections = RelatedSectionsBuilder.build(
                                kind = kind,
                                snapshot = snapshot,
                                isBlocked = { category, rating ->
                                    controls != null && parentalGate.isContentBlocked(category, rating, controls)
                                },
                                isFavorite = { contentId, type ->
                                    favorites.any { it.contentId == contentId && it.contentType == type }
                                },
                            )
                            RelatedUiState(
                                loadState = if (sections.isEmpty()) DetailsLoadState.Error else DetailsLoadState.Ready,
                                anchorTitle = snapshot.anchorTitle,
                                sections = sections,
                                message = appStrings.get(R.string.related_empty).takeIf { sections.isEmpty() },
                            )
                        },
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun load(kind: RelatedKind, id: String) {
        request.value = kind to id
    }

    fun toggleFavorite(card: HomeContentCard) {
        viewModelScope.launch {
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return@launch
            val type = card.toFavoriteContentType()
            try {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = card.contentId,
                    contentType = type,
                    title = card.title,
                    posterUrl = card.imageUrl,
                    sourceId = session.currentSourceId,
                    currentlyFavorite = card.isFavorite,
                )
                _uiState.update { it.copy(sections = it.sections.withFavorite(card.contentId, type, isFavorite)) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Favorite toggle failed: ${error.safeSummary()}")
            }
        }
    }

    private suspend fun loadSnapshot(kind: RelatedKind, id: String, sourceId: String?, isDemoMode: Boolean): RelatedSnapshot? =
        try {
            when (kind) {
                RelatedKind.Movie -> browseContentRepository.loadMovieRelated(sourceId, id, isDemoMode)
                RelatedKind.Series -> browseContentRepository.loadSeriesRelated(sourceId, id, isDemoMode)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Related titles failed to load: ${error.safeSummary()}")
            null
        }

    private fun notFoundMessage(kind: RelatedKind): Int = when (kind) {
        RelatedKind.Movie -> R.string.error_movie_not_found
        RelatedKind.Series -> R.string.error_series_not_found
    }

    private companion object {
        const val TAG = "RelatedViewModel"
    }
}
