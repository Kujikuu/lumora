package com.iptvcinema.tv.features.mylist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.continueWatchingKey
import com.iptvcinema.tv.core.util.removeContinueWatching
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.features.browse.BrowseSessionKey
import com.iptvcinema.tv.features.browse.ContinueWatchingCardFactory
import com.iptvcinema.tv.features.home.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MyListLoadState { Loading, Ready, Empty, Error }

data class MyListUiState(
    val loadState: MyListLoadState = MyListLoadState.Loading,
    val sections: List<HomeSection> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * My List follows favorites, Continue Watching and parental controls as they change, so coming
 * back from details never reloads the screen. Removals hide the card at once and roll back if the
 * server refuses.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyListViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val favoritesRepository: FavoritesRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val contentLoader: MyListContentLoader,
    private val continueWatchingCards: ContinueWatchingCardFactory,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyListUiState())
    val uiState: StateFlow<MyListUiState> = _uiState.asStateFlow()

    private val retryTick = MutableStateFlow(0)
    private val hiddenFavoriteKeys = MutableStateFlow<Set<String>>(emptySet())
    private val hiddenContinueKeys = MutableStateFlow<Set<String>>(emptySet())

    // The latest saved entries by card, so a card's menu can remove the favorite behind it.
    private var entriesByKey: Map<String, MyListEntry> = emptyMap()

    init {
        viewModelScope.launch {
            observeMyList().collect { _uiState.value = it }
        }
    }

    private fun observeMyList(): Flow<MyListUiState> = combine(
        appSessionRepository.sessionState.map {
            BrowseSessionKey(it.currentProfileId, it.currentSourceId, it.isDemoMode)
        },
        userSettingsRepository.observeSettings().map { it?.continueWatchingEnabled ?: true },
        retryTick,
    ) { session, continueWatchingEnabled, tick -> Triple(session, continueWatchingEnabled, tick) }
        .distinctUntilChanged()
        .flatMapLatest { (session, continueWatchingEnabled, _) ->
            val profileId = session.profileId ?: return@flatMapLatest flowOf(MyListUiState(MyListLoadState.Empty))
            val savedFlow = favoritesRepository.observeFavorites(profileId)
                .mapLatest { contentLoader.load(it, session.sourceId, session.isDemoMode) }
            val continueFlow = if (continueWatchingEnabled) {
                watchHistoryRepository.observeContinueWatching(profileId, CONTINUE_WATCHING_LIMIT).mapLatest { history ->
                    history.mapNotNull { continueWatchingCards.create(it, session.sourceId, session.isDemoMode, emptyList()) }
                }
            } else {
                flowOf(emptyList())
            }
            combine(
                savedFlow,
                continueFlow,
                parentalControlsRepository.observeControls(profileId),
                hiddenFavoriteKeys,
                hiddenContinueKeys,
            ) { saved, continueCards, controls, hiddenFavorites, hiddenContinue ->
                entriesByKey = saved.all.associateBy { it.card.favoriteKey() }
                val sections = MyListSectionsBuilder.build(
                    saved = saved,
                    continueWatching = continueCards.filterNot { it.continueWatchingKey() in hiddenContinue },
                    isBlocked = { category, rating ->
                        controls != null && parentalGate.isContentBlocked(category, rating, controls)
                    },
                    hiddenKeys = hiddenFavorites,
                )
                MyListUiState(
                    loadState = if (sections.isEmpty()) MyListLoadState.Empty else MyListLoadState.Ready,
                    sections = sections,
                )
            }.catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "My List failed to load: ${error.safeSummary()}")
                emit(MyListUiState(MyListLoadState.Error, errorMessage = appStrings.get(R.string.mylist_error_load)))
            }
        }

    fun retry() {
        _uiState.update { it.copy(loadState = MyListLoadState.Loading) }
        retryTick.update { it + 1 }
    }

    fun refreshContinueWatching() {
        watchHistoryRepository.invalidate()
    }

    /** Removes a saved title; the card disappears now and comes back if the server refuses. */
    fun removeFavorite(card: HomeContentCard) {
        val key = card.favoriteKey()
        val entry = entriesByKey[key] ?: return
        hiddenFavoriteKeys.update { it + key }
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            try {
                favoritesRepository.removeFavorite(profileId, entry.favorite)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Removing from My List failed: ${error.safeSummary()}")
                hiddenFavoriteKeys.update { it - key }
            }
        }
    }

    fun removeContinueWatching(card: HomeContentCard) {
        hiddenContinueKeys.update { it + card.continueWatchingKey() }
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            try {
                watchHistoryRepository.removeContinueWatching(profileId, card)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Removing from Continue Watching failed: ${error.safeSummary()}")
            }
        }
    }

    private companion object {
        const val TAG = "MyListViewModel"
        const val CONTINUE_WATCHING_LIMIT = 20
    }
}
