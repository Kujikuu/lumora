package com.iptvcinema.tv.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelItem
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelTileData
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toMovieItem
import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.catalog.FeaturedCatalogContent
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.player.PlaybackSessionTracker
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.SyncStatusFormatter
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.home.HomeCardAction
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.MoodCategory
import com.iptvcinema.tv.core.model.home.toFavoriteContentType
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard
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

data class HomeUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val featured: FeaturedCatalogContent? = null,
    val heroMovies: List<MovieItem> = emptyList(),
    val continueWatching: List<HomeContentCard> = emptyList(),
    val trending: List<HomeContentCard> = emptyList(),
    val featuredSeries: List<HomeContentCard> = emptyList(),
    val liveChannels: List<ChannelTileData> = emptyList(),
    val newReleases: List<HomeContentCard> = emptyList(),
    val moodCategories: List<MoodCategory> = HomeMoodCategories.defaults,
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val playbackSessionTracker: PlaybackSessionTracker,
    private val catalogRefreshController: CatalogRefreshController,
    private val catalogSyncProgressTracker: CatalogSyncProgressTracker,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                catalogRepository.observeSyncState(),
                catalogRepository.observeSourceMeta(),
            ) { syncState, (status, _) ->
                SyncStatusFormatter.formatBanner(
                    statusSyncing = status == SourceStatus.SYNCING,
                    lastSyncedAtEpochMs = syncState?.lastSyncedAtEpochMs,
                )
            }.collect { banner ->
                _uiState.value = _uiState.value.copy(syncBannerText = banner)
            }
        }
        viewModelScope.launch {
            combine(
                catalogRepository.observeHomeContent(),
                appSessionRepository.sessionState,
            ) { catalogState, session ->
                catalogState to session
            }.flatMapLatest { (catalogState, session) ->
                val profileId = session.currentProfileId
                val continueWatchingEnabled = runCatching {
                    userSettingsRepository.getSettings()?.continueWatchingEnabled
                }.getOrNull() ?: true
                val controlsFlow = profileId?.let { parentalControlsRepository.observeControls(it) } ?: flowOf(null)
                val continueWatchingFlow = if (profileId != null && continueWatchingEnabled) {
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
                    continueWatchingFlow,
                    controlsFlow,
                    playbackSessionTracker.currentLiveChannelId,
                    favoritesFlow,
                ) { history, controls, nowPlayingChannelId, favorites ->
                    HomeFlowBundle(state = catalogState, continueWatchingEnabled = continueWatchingEnabled, history = history, controls = controls, nowPlayingChannelId = nowPlayingChannelId, favorites = favorites)
                }
            }.collect { bundle ->
                val state = bundle.state
                val continueWatchingEnabled = bundle.continueWatchingEnabled
                val history = bundle.history
                val controls = bundle.controls
                val nowPlayingChannelId = bundle.nowPlayingChannelId
                val favorites = bundle.favorites
                val session = appSessionRepository.sessionState.first()
                val featured = state.items.firstOrNull()
                val continueWatching = if (continueWatchingEnabled) {
                    history.mapNotNull { item ->
                        val contentType = when (item.contentType) {
                            WatchHistoryContentType.MOVIE -> "movie"
                            WatchHistoryContentType.EPISODE -> "episode"
                            WatchHistoryContentType.CHANNEL -> return@mapNotNull null
                        }
                        val progress = item.durationMs?.takeIf { it > 0 }?.let { duration ->
                            (item.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        }
                        val display = catalogRepository.resolveWatchHistoryCardDisplay(
                            sourceId = session.currentSourceId,
                            item = item,
                            isDemoMode = session.isDemoMode,
                        )
                        val favoriteType = when (item.contentType) {
                            WatchHistoryContentType.MOVIE -> FavoriteContentType.MOVIE
                            WatchHistoryContentType.EPISODE -> FavoriteContentType.EPISODE
                            WatchHistoryContentType.CHANNEL -> return@mapNotNull null
                        }
                        HomeContentCard(
                            contentId = item.contentId,
                            contentType = contentType,
                            seriesId = item.seriesId,
                            title = display.title,
                            subtitle = display.subtitle,
                            imageUrl = display.posterUrl,
                            progress = progress,
                            isFavorite = favorites.isFavorite(item.contentId, favoriteType),
                            primaryAction = HomeCardAction.ContinueWatching,
                        )
                    }
                } else {
                    emptyList()
                }

                val current = _uiState.value
                val nextState = when {
                    state.sourceStatus == SourceStatus.EXPIRED ||
                        (state.sourceStatus == SourceStatus.FAILED &&
                            state.sourceType == SourceType.M3U) -> {
                        HomeUiState(
                            loadState = state.loadState,
                            continueWatching = continueWatching,
                            message = state.message,
                            sourceStatus = state.sourceStatus,
                            sourceType = state.sourceType,
                        )
                    }
                    state.loadState != CatalogLoadState.Ready || featured == null -> {
                        HomeUiState(
                            loadState = if (state.loadState == CatalogLoadState.Ready && featured == null) {
                                CatalogLoadState.Empty
                            } else {
                                state.loadState
                            },
                            continueWatching = continueWatching,
                            message = state.message ?: "No content synced yet",
                            sourceStatus = state.sourceStatus,
                            sourceType = state.sourceType,
                        )
                    }
                    else -> {
                        val trendingMovies = filterMovies(featured.trendingMovies, controls)
                        val liveChannelItems = if (controls != null) {
                            featured.liveChannels.filter { channel ->
                                !parentalGate.isCategoryBlocked(channel.categoryName.orEmpty(), controls)
                            }
                        } else {
                            featured.liveChannels
                        }
                        val newReleaseMovies = filterMovies(featured.newReleaseMovies, controls)
                        val heroMovies = if (controls != null) {
                            featured.heroMovies.map { movie ->
                                movie.toMovieItem(
                                    isFavorite = favorites.isFavorite(movie.id, FavoriteContentType.MOVIE),
                                )
                            }.filter { movie ->
                                !parentalGate.isContentBlocked(
                                    movie.genres.firstOrNull(),
                                    movie.rating,
                                    controls,
                                )
                            }
                        } else {
                            featured.heroMovies.map { movie ->
                                movie.toMovieItem(
                                    isFavorite = favorites.isFavorite(movie.id, FavoriteContentType.MOVIE),
                                )
                            }
                        }
                        val featuredSeries = filterSeries(featured.featuredSeries, controls)
                        HomeUiState(
                            loadState = CatalogLoadState.Ready,
                            featured = featured,
                            heroMovies = heroMovies,
                            continueWatching = continueWatching,
                            trending = trendingMovies.mapIndexed { index, movie ->
                                movie.toHomeContentCard(
                                    isFavorite = favorites.isFavorite(movie.id, FavoriteContentType.MOVIE),
                                    showTop10Badge = index < 10,
                                )
                            },
                            featuredSeries = featuredSeries.map { series ->
                                series.toHomeContentCard(
                                    isFavorite = favorites.isFavorite(series.id, FavoriteContentType.SERIES),
                                )
                            },
                            liveChannels = liveChannelItems.map {
                                it.toChannelItem(noProgramInfoTitle = appStrings.get(R.string.msg_no_program_info))
                                    .toChannelTileData(nowPlayingChannelId = nowPlayingChannelId)
                            },
                            newReleases = newReleaseMovies.map { movie ->
                                movie.toHomeContentCard(
                                    isFavorite = favorites.isFavorite(movie.id, FavoriteContentType.MOVIE),
                                )
                            },
                            sourceStatus = state.sourceStatus,
                            sourceType = state.sourceType,
                        )
                    }
                }
                _uiState.value = nextState.copy(
                    syncBannerText = current.syncBannerText,
                    refreshState = current.refreshState,
                )
            }
        }
    }

    fun refreshContinueWatching() {
        watchHistoryRepository.invalidate()
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

    fun toggleFavorite(card: HomeContentCard, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            val session = appSessionRepository.sessionState.first()
            val contentType = card.toFavoriteContentType()
            runCatching {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = card.contentId,
                    contentType = contentType,
                    title = card.title,
                    posterUrl = card.imageUrl,
                    sourceId = session.currentSourceId,
                )
                updateCardFavoriteState(card.contentId, contentType, isFavorite)
                onResult(isFavorite)
            }
        }
    }

    fun toggleHeroFavorite(movie: MovieItem, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            val session = appSessionRepository.sessionState.first()
            runCatching {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = movie.id,
                    contentType = FavoriteContentType.MOVIE,
                    title = movie.title,
                    posterUrl = movie.imageUrl,
                    sourceId = session.currentSourceId,
                )
                _uiState.value = _uiState.value.copy(
                    heroMovies = _uiState.value.heroMovies.map {
                        if (it.id == movie.id) it.copy(isFavorite = isFavorite) else it
                    },
                )
                onResult(isFavorite)
            }
        }
    }

    private fun updateCardFavoriteState(contentId: String, contentType: FavoriteContentType, isFavorite: Boolean) {
        fun List<HomeContentCard>.update() = map { card ->
            if (card.contentId == contentId && card.toFavoriteContentType() == contentType) {
                card.copy(isFavorite = isFavorite)
            } else {
                card
            }
        }
        val state = _uiState.value
        _uiState.value = state.copy(
            continueWatching = state.continueWatching.update(),
            trending = state.trending.update(),
            featuredSeries = state.featuredSeries.update(),
            newReleases = state.newReleases.update(),
        )
    }

    private fun filterMovies(movies: List<CatalogMovie>, controls: ParentalControls?): List<CatalogMovie> =
        if (controls != null) {
            movies.filter { movie ->
                !parentalGate.isContentBlocked(movie.genres.firstOrNull(), movie.rating, controls)
            }
        } else {
            movies
        }

    private fun filterSeries(series: List<CatalogSeries>, controls: ParentalControls?): List<CatalogSeries> =
        if (controls != null) {
            series.filter { item ->
                !parentalGate.isContentBlocked(item.categoryName, item.rating, controls)
            }
        } else {
            series
        }

    private fun List<FavoriteItem>.isFavorite(contentId: String, contentType: FavoriteContentType): Boolean =
        any { it.contentId == contentId && it.contentType == contentType }

    private data class HomeFlowBundle(
        val state: CatalogBrowseState<FeaturedCatalogContent>,
        val continueWatchingEnabled: Boolean,
        val history: List<WatchHistoryItem>,
        val controls: ParentalControls?,
        val nowPlayingChannelId: String?,
        val favorites: List<FavoriteItem>,
    )
}
