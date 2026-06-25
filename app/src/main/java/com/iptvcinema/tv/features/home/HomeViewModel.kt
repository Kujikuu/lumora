package com.iptvcinema.tv.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelItem
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelTileData
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toMovieItem
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toPosterCardData
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.player.PlaybackSessionTracker
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.SyncStatusFormatter
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.catalog.FeaturedCatalogContent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class ContinueWatchingItem(
    val contentId: String,
    val contentType: String,
    val seriesId: String? = null,
    val poster: PosterCardData,
)

data class HomeUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val featured: FeaturedCatalogContent? = null,
    val heroMovies: List<MovieItem> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val trending: List<PosterCardData> = emptyList(),
    val featuredSeries: List<PosterCardData> = emptyList(),
    val liveChannels: List<ChannelTileData> = emptyList(),
    val newReleases: List<PosterCardData> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val playbackSessionTracker: PlaybackSessionTracker,
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
                combine(continueWatchingFlow, controlsFlow, playbackSessionTracker.currentLiveChannelId) { history, controls, nowPlayingChannelId ->
                    Triple(catalogState, continueWatchingEnabled, Triple(history, controls, nowPlayingChannelId))
                }
            }.collect { (state, continueWatchingEnabled, historyControlsAndNowPlaying) ->
                val (history, controls, nowPlayingChannelId) = historyControlsAndNowPlaying
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
                        ContinueWatchingItem(
                            contentId = item.contentId,
                            contentType = contentType,
                            seriesId = item.seriesId,
                            poster = PosterCardData(
                                title = display.title,
                                subtitle = display.subtitle,
                                imageUrl = display.posterUrl,
                                progress = progress,
                                contentId = item.contentId,
                            ),
                        )
                    }
                } else {
                    emptyList()
                }

                _uiState.value = when {
                    state.sourceStatus == com.iptvcinema.tv.core.model.SourceStatus.EXPIRED ||
                        (state.sourceStatus == com.iptvcinema.tv.core.model.SourceStatus.FAILED &&
                            state.sourceType == com.iptvcinema.tv.core.model.SourceType.M3U) -> {
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
                        val trendingMovies = if (controls != null) {
                            featured.trendingMovies.filter { movie ->
                                !parentalGate.isContentBlocked(
                                    movie.genres.firstOrNull(),
                                    movie.rating,
                                    controls,
                                )
                            }
                        } else {
                            featured.trendingMovies
                        }
                        val liveChannelItems = if (controls != null) {
                            featured.liveChannels.filter { channel ->
                                !parentalGate.isCategoryBlocked(channel.categoryName.orEmpty(), controls)
                            }
                        } else {
                            featured.liveChannels
                        }
                        val newReleaseMovies = if (controls != null) {
                            featured.newReleaseMovies.filter { movie ->
                                !parentalGate.isContentBlocked(
                                    movie.genres.firstOrNull(),
                                    movie.rating,
                                    controls,
                                )
                            }
                        } else {
                            featured.newReleaseMovies
                        }
                        val heroMovies = if (controls != null) {
                            featured.heroMovies.map { it.toMovieItem() }.filter { movie ->
                                !parentalGate.isContentBlocked(
                                    movie.genres.firstOrNull(),
                                    movie.rating,
                                    controls,
                                )
                            }
                        } else {
                            featured.heroMovies.map { it.toMovieItem() }
                        }
                        val featuredSeries = if (controls != null) {
                            featured.featuredSeries.filter { series ->
                                !parentalGate.isContentBlocked(
                                    series.categoryName,
                                    series.rating,
                                    controls,
                                )
                            }
                        } else {
                            featured.featuredSeries
                        }
                        HomeUiState(
                            loadState = CatalogLoadState.Ready,
                            featured = featured,
                            heroMovies = heroMovies,
                            continueWatching = continueWatching,
                            trending = trendingMovies.map { it.toPosterCardData() },
                            featuredSeries = featuredSeries.map { it.toPosterCardData() },
                            liveChannels = liveChannelItems.map {
                                it.toChannelItem(noProgramInfoTitle = appStrings.get(R.string.msg_no_program_info)).toChannelTileData(nowPlayingChannelId = nowPlayingChannelId)
                            },
                            newReleases = newReleaseMovies.map { it.toPosterCardData() },
                            sourceStatus = state.sourceStatus,
                            sourceType = state.sourceType,
                        )
                    }
                }
            }
        }
    }

    fun refreshContinueWatching() {
        watchHistoryRepository.invalidate()
    }
}
