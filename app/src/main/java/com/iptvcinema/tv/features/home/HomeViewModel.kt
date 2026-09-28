package com.iptvcinema.tv.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.HomeContentRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.FeaturedCatalogContent
import com.iptvcinema.tv.core.model.home.HomeCardAction
import com.iptvcinema.tv.core.model.home.HomeCatalogSnapshot
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.HomePersonalSnapshot
import com.iptvcinema.tv.core.model.home.toFavoriteContentType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.RemainingWatchTimeFormatter
import com.iptvcinema.tv.core.util.SyncStatusFormatter
import com.iptvcinema.tv.core.util.continueWatchingKey
import com.iptvcinema.tv.core.util.removeContinueWatching
import com.iptvcinema.tv.core.util.safeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val homeContentRepository: HomeContentRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
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
            combine(observeHome(), hiddenContinueKeys) { state, hidden -> state.withoutContinueWatching(hidden) }
                .collect { next ->
                val current = _uiState.value
                _uiState.value = next.copy(
                    syncBannerText = current.syncBannerText,
                    refreshState = current.refreshState,
                )
            }
        }
    }

    /**
     * The catalog gate decides loading/empty/error. Catalog sections reload when the catalog
     * changes and personal sections when history changes; neither restarts the other. Only a
     * profile, source or demo switch restarts the whole pipeline.
     *
     * Personal sections (history lookups) start empty so they never delay the first frame.
     * Continue Watching, favorites and parental controls are cheap local reads and are awaited:
     * showing rails before parental controls load could flash blocked content.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeHome(): Flow<HomeUiState> = combine(
        appSessionRepository.sessionState.map { HomeSessionKey(it.currentProfileId, it.currentSourceId, it.isDemoMode) },
        userSettingsRepository.observeSettings().map { it?.continueWatchingEnabled ?: true },
    ) { session, continueWatchingEnabled -> session to continueWatchingEnabled }
        .distinctUntilChanged()
        .flatMapLatest { (session, continueWatchingEnabled) ->
            val profileId = session.profileId
            val controlsFlow = profileId?.let { parentalControlsRepository.observeControls(it) } ?: flowOf(null)
            val historyFlow = profileId?.let { watchHistoryRepository.observeHistory(it, HISTORY_LIMIT) }
                ?: flowOf(emptyList())
            val continueFlow = if (profileId != null && continueWatchingEnabled) {
                watchHistoryRepository.observeContinueWatching(profileId, CONTINUE_WATCHING_LIMIT)
            } else {
                flowOf(emptyList())
            }
            val favoritesFlow = profileId?.let { favoritesRepository.observeFavorites(it) } ?: flowOf(emptyList())
            // A sync writes in batches and the gate re-emits on each; let those settle so the
            // catalog queries run once per burst instead of being cancelled over and over.
            val catalogFlow = catalogRepository.observeHomeContent()
                .withIndex()
                .debounce { if (it.index == 0) 0L else CATALOG_SETTLE_MS }
                .map { it.value }
                .mapLatest { gate ->
                    val snapshot = if (gate.loadState == CatalogLoadState.Ready) {
                        homeContentRepository.loadCatalog(session.sourceId, session.isDemoMode)
                    } else {
                        HomeCatalogSnapshot()
                    }
                    gate to snapshot
                }
            val personalFlow = combine(historyFlow, controlsFlow) { history, controls -> history to controls }
                .mapLatest { (history, controls) ->
                    homeContentRepository.loadPersonal(
                        sourceId = session.sourceId,
                        isDemoMode = session.isDemoMode,
                        history = history,
                        isCategoryBlocked = { name -> controls != null && parentalGate.isCategoryBlocked(name, controls) },
                    )
                }
                .onStart { emit(HomePersonalSnapshot()) }
            combine(catalogFlow, personalFlow, continueFlow, controlsFlow, favoritesFlow) {
                    (gate, catalog), personal, continueHistory, controls, favorites ->
                HomeInputs(gate, session, catalog, personal, continueHistory, controls, favorites)
            }.mapLatest(::toUiState)
        }

    private suspend fun toUiState(inputs: HomeInputs): HomeUiState {
        val gate = inputs.gate
        val continueWatching = inputs.continueHistory.mapNotNull { item ->
            item.toContinueCard(inputs.session, inputs.favorites)
        }
        val sourceUnusable = gate.sourceStatus == SourceStatus.EXPIRED ||
            (gate.sourceStatus == SourceStatus.FAILED && gate.sourceType == SourceType.M3U)
        if (sourceUnusable || gate.loadState != CatalogLoadState.Ready) {
            return HomeUiState(
                loadState = gate.loadState,
                sections = continueWatching.takeIf { it.isNotEmpty() }
                    ?.let { listOf(HomeSection.ContinueWatching(it)) }
                    .orEmpty(),
                message = gate.message,
                sourceStatus = gate.sourceStatus,
                sourceType = gate.sourceType,
            )
        }
        val controls = inputs.controls
        val content = HomeSectionsBuilder.build(
            catalog = inputs.catalog,
            personal = inputs.personal,
            continueWatching = continueWatching,
            isBlocked = { category, rating ->
                controls != null && parentalGate.isContentBlocked(category, rating, controls)
            },
            isFavorite = { id, type -> inputs.favorites.isFavorite(id, type) },
        )
        val isEmpty = content.hero.isEmpty() && content.sections.isEmpty()
        return HomeUiState(
            loadState = if (isEmpty) CatalogLoadState.Empty else CatalogLoadState.Ready,
            hero = content.hero,
            sections = content.sections,
            message = gate.message,
            sourceStatus = gate.sourceStatus,
            sourceType = gate.sourceType,
        )
    }

    private suspend fun WatchHistoryItem.toContinueCard(
        session: HomeSessionKey,
        favorites: List<FavoriteItem>,
    ): HomeContentCard? {
        val (contentType, favoriteType) = when (this.contentType) {
            WatchHistoryContentType.MOVIE -> "movie" to FavoriteContentType.MOVIE
            WatchHistoryContentType.EPISODE -> "episode" to FavoriteContentType.EPISODE
            WatchHistoryContentType.CHANNEL -> return null
        }
        val display = catalogRepository.resolveWatchHistoryCardDisplay(
            sourceId = session.sourceId,
            item = this,
            isDemoMode = session.isDemoMode,
        )
        return HomeContentCard(
            contentId = contentId,
            contentType = contentType,
            seriesId = seriesId,
            title = display.title,
            subtitle = display.subtitle,
            imageUrl = display.posterUrl,
            backdropUrl = display.backdropUrl,
            progress = durationMs?.takeIf { it > 0 }?.let { (positionMs.toFloat() / it).coerceIn(0f, 1f) },
            remainingTimeLabel = RemainingWatchTimeFormatter.formatFromWatchHistory(this, appStrings),
            isFavorite = favorites.isFavorite(contentId, favoriteType),
            primaryAction = HomeCardAction.ContinueWatching,
        )
    }

    fun refreshContinueWatching() {
        watchHistoryRepository.invalidate()
    }

    // Hidden right away; the server delete and re-fetch take seconds on a slow TV.
    private val hiddenContinueKeys = MutableStateFlow<Set<String>>(emptySet())

    fun removeContinueWatching(card: HomeContentCard) {
        val key = card.continueWatchingKey()
        hiddenContinueKeys.update { it + key }
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            runCatchingNonCancellation { watchHistoryRepository.removeContinueWatching(profileId, card) }
        }
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
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return@launch
            val contentType = card.toFavoriteContentType()
            runCatchingNonCancellation {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = card.contentId,
                    contentType = contentType,
                    title = card.title,
                    posterUrl = card.imageUrl,
                    sourceId = session.currentSourceId,
                    currentlyFavorite = card.isFavorite,
                )
                val state = _uiState.value
                _uiState.value = state.copy(sections = state.sections.withFavorite(card.contentId, contentType, isFavorite))
            }
        }
    }

    fun toggleHeroFavorite(movie: MovieItem) {
        viewModelScope.launch {
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return@launch
            runCatchingNonCancellation {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = movie.id,
                    contentType = FavoriteContentType.MOVIE,
                    title = movie.title,
                    posterUrl = movie.imageUrl,
                    sourceId = session.currentSourceId,
                    currentlyFavorite = movie.isFavorite,
                )
                val state = _uiState.value
                _uiState.value = state.copy(
                    hero = state.hero.map { if (it.id == movie.id) it.copy(isFavorite = isFavorite) else it },
                    sections = state.sections.withFavorite(movie.id, FavoriteContentType.MOVIE, isFavorite),
                )
            }
        }
    }

    fun addHeroToList(movie: MovieItem) {
        if (!movie.isFavorite) toggleHeroFavorite(movie)
    }

    /** Like runCatching, but lets cancellation propagate. Failures are logged, not shown. */
    private suspend fun runCatchingNonCancellation(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Home action failed: ${error.safeSummary()}")
        }
    }

    private fun List<FavoriteItem>.isFavorite(contentId: String, contentType: FavoriteContentType): Boolean =
        any { it.contentId == contentId && it.contentType == contentType }

    private data class HomeInputs(
        val gate: CatalogBrowseState<FeaturedCatalogContent>,
        val session: HomeSessionKey,
        val catalog: HomeCatalogSnapshot,
        val personal: HomePersonalSnapshot,
        val continueHistory: List<WatchHistoryItem>,
        val controls: ParentalControls?,
        val favorites: List<FavoriteItem>,
    )

    /** The session fields Home depends on; other session changes do not restart the pipeline. */
    private data class HomeSessionKey(
        val profileId: String?,
        val sourceId: String?,
        val isDemoMode: Boolean,
    )

    private companion object {
        const val TAG = "HomeViewModel"
        const val CATALOG_SETTLE_MS = 400L
        const val HISTORY_LIMIT = 50
        const val CONTINUE_WATCHING_LIMIT = 10
    }
}
