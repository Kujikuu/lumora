package com.iptvcinema.tv.features.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.repository.BrowseContentRepository
import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.isCategory
import com.iptvcinema.tv.core.model.home.toFavoriteContentType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.continueWatchingKey
import com.iptvcinema.tv.core.util.removeContinueWatching
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.withFavorite
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

data class BrowseTabUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val sections: List<HomeSection> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
)

/** The session fields a browse tab depends on; other session changes do not reload it. */
data class BrowseSessionKey(
    val profileId: String?,
    val sourceId: String?,
    val isDemoMode: Boolean,
)

/** Everything a browse tab needs from the app, bundled so subclasses keep short constructors. */
class BrowseTabDependencies @Inject constructor(
    val catalogRepository: CatalogRepository,
    val browseContentRepository: BrowseContentRepository,
    val watchHistoryRepository: WatchHistoryRepository,
    val favoritesRepository: FavoritesRepository,
    val userSettingsRepository: UserSettingsRepository,
    val appSessionRepository: AppSessionRepository,
    val parentalControlsRepository: ParentalControlsRepository,
    val parentalGate: ParentalGate,
    val catalogRefreshController: CatalogRefreshController,
    val catalogSyncProgressTracker: CatalogSyncProgressTracker,
    val continueWatchingCards: ContinueWatchingCardFactory,
    val appStrings: AppStrings,
)

/**
 * A Home-style tab (Movies, Series): the catalog gate decides loading/empty/error, catalog rails
 * reload when the catalog changes and personal rails when history changes. Only a profile,
 * source or demo switch restarts the whole pipeline.
 *
 * Personal rails start empty so they never delay the first frame. Continue Watching, favorites
 * and parental controls are cheap local reads and are awaited, so blocked content never flashes.
 */
abstract class BrowseTabViewModel<Catalog, Personal>(
    protected val deps: BrowseTabDependencies,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowseTabUiState())
    val uiState: StateFlow<BrowseTabUiState> = _uiState.asStateFlow()

    // Hides removed Continue Watching cards right away; the server delete and re-fetch take
    // seconds on a slow TV.
    private val hiddenContinueKeys = MutableStateFlow<Set<String>>(emptySet())

    /** The Continue Watching entries this tab shows. */
    protected abstract val continueWatchingType: WatchHistoryContentType
    protected abstract val emptyCatalog: Catalog
    protected abstract val emptyPersonal: Personal

    protected abstract fun observeAvailability(): Flow<CatalogBrowseState<Unit>>
    protected abstract suspend fun loadCatalog(session: BrowseSessionKey): Catalog
    protected abstract suspend fun loadPersonal(
        session: BrowseSessionKey,
        history: List<WatchHistoryItem>,
        isCategoryBlocked: (String) -> Boolean,
    ): Personal

    protected abstract fun buildSections(
        catalog: Catalog,
        personal: Personal,
        continueWatching: List<HomeContentCard>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
    ): List<HomeSection>

    /** Starts loading. Called from the subclass init, once its abstract members are ready. */
    protected fun start() {
        CatalogRefreshSupport.observeSyncBanner(viewModelScope, deps.catalogRepository) { banner ->
            _uiState.update { it.copy(syncBannerText = banner) }
        }
        viewModelScope.launch {
            combine(observeTab(), hiddenContinueKeys) { state, hidden -> state.withoutHidden(hidden) }
                .collect { next ->
                    _uiState.update { current ->
                        next.copy(syncBannerText = current.syncBannerText, refreshState = current.refreshState)
                    }
                }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeTab(): Flow<BrowseTabUiState> = combine(
        deps.appSessionRepository.sessionState.map {
            BrowseSessionKey(it.currentProfileId, it.currentSourceId, it.isDemoMode)
        },
        deps.userSettingsRepository.observeSettings().map { it?.continueWatchingEnabled ?: true },
    ) { session, continueWatchingEnabled -> session to continueWatchingEnabled }
        .distinctUntilChanged()
        .flatMapLatest { (session, continueWatchingEnabled) ->
            val profileId = session.profileId
            val controlsFlow = profileId?.let { deps.parentalControlsRepository.observeControls(it) } ?: flowOf(null)
            val historyFlow = profileId?.let { deps.watchHistoryRepository.observeHistory(it, HISTORY_LIMIT) }
                ?: flowOf(emptyList())
            val continueFlow = if (profileId != null && continueWatchingEnabled) {
                deps.watchHistoryRepository.observeContinueWatching(profileId, CONTINUE_WATCHING_LIMIT)
            } else {
                flowOf(emptyList())
            }
            val favoritesFlow = profileId?.let { deps.favoritesRepository.observeFavorites(it) } ?: flowOf(emptyList())
            // A sync writes in batches and the gate re-emits on each; let those settle so the
            // catalog queries run once per burst instead of being cancelled over and over.
            val catalogFlow = observeAvailability()
                .withIndex()
                .debounce { if (it.index == 0) 0L else CATALOG_SETTLE_MS }
                .map { it.value }
                .mapLatest { gate ->
                    val catalog = if (gate.loadState == CatalogLoadState.Ready) loadCatalog(session) else emptyCatalog
                    gate to catalog
                }
            val personalFlow = combine(historyFlow, controlsFlow) { history, controls -> history to controls }
                .mapLatest { (history, controls) ->
                    loadPersonal(session, history) { name ->
                        controls != null && deps.parentalGate.isCategoryBlocked(name, controls)
                    }
                }
                .onStart { emit(emptyPersonal) }
            combine(catalogFlow, personalFlow, continueFlow, controlsFlow, favoritesFlow) {
                    (gate, catalog), personal, continueHistory, controls, favorites ->
                TabInputs(gate, session, catalog, personal, continueHistory, controls, favorites)
            }.mapLatest(::toUiState)
        }

    private suspend fun toUiState(inputs: TabInputs<Catalog, Personal>): BrowseTabUiState {
        val gate = inputs.gate
        val controls = inputs.controls
        fun isBlocked(category: String?, rating: String?) =
            controls != null && deps.parentalGate.isContentBlocked(category, rating, controls)

        val continueWatching = inputs.continueHistory
            .filter { it.contentType == continueWatchingType }
            .mapNotNull { item ->
                deps.continueWatchingCards.create(item, inputs.session.sourceId, inputs.session.isDemoMode, inputs.favorites)
            }
        val sourceUnusable = gate.sourceStatus == SourceStatus.EXPIRED ||
            (gate.sourceStatus == SourceStatus.FAILED && gate.sourceType == SourceType.M3U)
        if (sourceUnusable || gate.loadState != CatalogLoadState.Ready) {
            return BrowseTabUiState(
                loadState = gate.loadState,
                sections = continueWatching.takeIf { it.isNotEmpty() }
                    ?.let { listOf(HomeSection.ContinueWatching(it)) }
                    .orEmpty(),
                message = gate.message,
                sourceStatus = gate.sourceStatus,
                sourceType = gate.sourceType,
            )
        }
        val sections = buildSections(
            catalog = inputs.catalog,
            personal = inputs.personal,
            continueWatching = continueWatching,
            isBlocked = ::isBlocked,
            isFavorite = { id, type -> inputs.favorites.any { it.contentId == id && it.contentType == type } },
        )
        return BrowseTabUiState(
            loadState = if (sections.isEmpty()) CatalogLoadState.Empty else CatalogLoadState.Ready,
            sections = sections,
            message = gate.message,
            sourceStatus = gate.sourceStatus,
            sourceType = gate.sourceType,
        )
    }

    /** Label under a category name, e.g. "124 titles". */
    protected fun titleCountLabel(count: Int): String =
        deps.appStrings.getQuantity(com.iptvcinema.tv.R.plurals.browse_title_count, count, count)

    fun refreshContinueWatching() {
        deps.watchHistoryRepository.invalidate()
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

    fun removeContinueWatching(card: HomeContentCard) {
        hiddenContinueKeys.update { it + card.continueWatchingKey() }
        viewModelScope.launch {
            val profileId = deps.appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            runCatchingNonCancellation { deps.watchHistoryRepository.removeContinueWatching(profileId, card) }
        }
    }

    fun toggleFavorite(card: HomeContentCard) {
        if (card.isCategory) return
        viewModelScope.launch {
            val session = deps.appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return@launch
            val contentType = card.toFavoriteContentType()
            runCatchingNonCancellation {
                val isFavorite = deps.favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = card.contentId,
                    contentType = contentType,
                    title = card.title,
                    posterUrl = card.imageUrl,
                    sourceId = session.currentSourceId,
                    currentlyFavorite = card.isFavorite,
                )
                _uiState.update { it.copy(sections = it.sections.withFavorite(card.contentId, contentType, isFavorite)) }
            }
        }
    }

    /** Like runCatching, but lets cancellation propagate. Failures are logged, not shown. */
    private suspend fun runCatchingNonCancellation(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Browse action failed: ${error.safeSummary()}")
        }
    }

    private fun BrowseTabUiState.withoutHidden(hiddenKeys: Set<String>): BrowseTabUiState {
        if (hiddenKeys.isEmpty()) return this
        val filtered = sections.mapNotNull { section ->
            when (section) {
                is HomeSection.ContinueWatching, is HomeSection.NextEpisode ->
                    section.withItems(section.items.filterNot { it.continueWatchingKey() in hiddenKeys })
                        .takeIf { it.items.isNotEmpty() }
                else -> section
            }
        }
        return copy(sections = filtered)
    }

    private data class TabInputs<Catalog, Personal>(
        val gate: CatalogBrowseState<Unit>,
        val session: BrowseSessionKey,
        val catalog: Catalog,
        val personal: Personal,
        val continueHistory: List<WatchHistoryItem>,
        val controls: ParentalControls?,
        val favorites: List<FavoriteItem>,
    )

    private companion object {
        const val TAG = "BrowseTab"
        const val CATALOG_SETTLE_MS = 400L
        const val HISTORY_LIMIT = 50
        const val CONTINUE_WATCHING_LIMIT = 20
    }
}
