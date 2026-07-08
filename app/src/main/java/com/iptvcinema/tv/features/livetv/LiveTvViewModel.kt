package com.iptvcinema.tv.features.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.catalog.CatalogRefreshController
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.catalog.CatalogRefreshSupport
import com.iptvcinema.tv.core.catalog.CatalogSyncProgressTracker
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.parental.ParentalPlaybackGuard
import com.iptvcinema.tv.core.player.PlaybackRepository
import com.iptvcinema.tv.core.player.PlaybackResolveResult
import com.iptvcinema.tv.core.player.PlayerManager
import com.iptvcinema.tv.core.player.PlayerUiState
import com.iptvcinema.tv.core.player.PlaybackSessionTracker
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.util.AppStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class LiveTvUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val categories: List<String> = emptyList(),
    val channels: List<ChannelItem> = emptyList(),
    val epgPrograms: List<EpgProgram> = emptyList(),
    val selectedChannelPrograms: List<EpgProgram> = emptyList(),
    val guideWindowStartMs: Long = GuideLayoutHelper.defaultWindowStart(System.currentTimeMillis()),
    val guideWindowEndMs: Long = GuideLayoutHelper.defaultWindowEnd(System.currentTimeMillis()),
    val focusedProgram: EpgProgram? = null,
    val selectedChannel: ChannelItem? = null,
    val nowMs: Long = System.currentTimeMillis(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val nowPlayingChannelId: String? = null,
    val favoriteChannelIds: Set<String> = emptySet(),
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
    val playbackNotice: String? = null,
) {
    val previewChannel: ChannelItem?
        get() {
            val channel = selectedChannel ?: channels.firstOrNull() ?: return null
            val program = focusedProgram
            return if (program != null && program.channelId == channel.id) {
                GuideLayoutHelper.channelItemWithProgram(channel, program, nowMs)
            } else {
                channel
            }
        }

    val selectedChannelId: String?
        get() = selectedChannel?.id

    fun isChannelFavorite(channelId: String): Boolean = channelId in favoriteChannelIds
}

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val playbackSessionTracker: PlaybackSessionTracker,
    private val playerManager: PlayerManager,
    private val playbackRepository: PlaybackRepository,
    private val parentalPlaybackGuard: ParentalPlaybackGuard,
    private val appStrings: AppStrings,
    private val catalogRefreshController: CatalogRefreshController,
    private val catalogSyncProgressTracker: CatalogSyncProgressTracker,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val guideWindowStartMs = MutableStateFlow(GuideLayoutHelper.defaultWindowStart(System.currentTimeMillis()))
    private val focusedProgram = MutableStateFlow<EpgProgram?>(null)
    private val selectedChannel = MutableStateFlow<ChannelItem?>(null)
    private val nowMs = MutableStateFlow(System.currentTimeMillis())
    private val epgPrograms = MutableStateFlow<List<EpgProgram>>(emptyList())
    private val favoriteChannelIds = MutableStateFlow<Set<String>>(emptySet())
    private val fullGuideOpen = MutableStateFlow(false)
    private var currentParentalControls: com.iptvcinema.tv.core.model.ParentalControls? = null
    private val browseState = MutableStateFlow(
        LiveTvBrowseSlice(
            loadState = CatalogLoadState.Loading,
            categories = emptyList(),
            channels = emptyList(),
            message = null,
        ),
    )

    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()
    val playerState: StateFlow<PlayerUiState> = playerManager.state

    private var epgLoadJob: Job? = null
    private var clockJob: Job? = null
    private var playbackJob: Job? = null
    private var activePlaybackChannelId: String? = null

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
                val controlsFlow = session.currentProfileId?.let { profileId ->
                    parentalControlsRepository.observeControls(profileId)
                } ?: flowOf(null)
                val favoritesFlow = session.currentProfileId?.let { profileId ->
                    favoritesRepository.observeFavorites(profileId)
                } ?: flowOf(emptyList())
                combine(
                    catalogRepository.observeLiveTv(categoryName),
                    controlsFlow,
                    favoritesFlow,
                ) { state, controls, favorites ->
                    val filteredCategories = if (controls != null) {
                        parentalGate.filterCategoryNames(state.categories, controls)
                    } else {
                        state.categories
                    }
                    val filteredChannels = if (controls != null) {
                        state.items.filter { channel ->
                            !parentalGate.isCategoryBlocked(channel.category, controls)
                        }
                    } else {
                        state.items
                    }
                    LiveTvBrowseResult(
                        slice = LiveTvBrowseSlice(
                            loadState = state.loadState,
                            categories = filteredCategories,
                            channels = filteredChannels,
                            message = state.message,
                            sourceStatus = state.sourceStatus,
                            sourceType = state.sourceType,
                        ),
                        controls = controls,
                        favoriteChannelIds = favorites.channelIds(),
                    )
                }
            }.collect { result ->
                browseState.value = result.slice
                currentParentalControls = result.controls
                favoriteChannelIds.value = result.favoriteChannelIds
                val slice = result.slice
                if (slice.loadState == CatalogLoadState.Ready && slice.channels.isNotEmpty()) {
                    val previousChannelId = selectedChannel.value?.id
                    val resolvedChannel = selectedChannel.value?.takeIf { selected ->
                        slice.channels.any { it.id == selected.id }
                    } ?: slice.channels.firstOrNull()
                    selectedChannel.value = resolvedChannel
                    resolvedChannel?.id?.let { channelId ->
                        if (channelId != previousChannelId || activePlaybackChannelId == null) {
                            startPlayback(channelId)
                        }
                    }
                } else if (slice.channels.isEmpty()) {
                    selectedChannel.value = null
                }
                refreshEpg()
            }
        }

        viewModelScope.launch {
            combine(
                combine(browseState, guideWindowStartMs, focusedProgram, selectedChannel) { browse, windowStart, program, channel ->
                    LiveTvCombineLeft(browse, windowStart, program, channel)
                },
                combine(
                    nowMs,
                    epgPrograms,
                    playbackSessionTracker.currentLiveChannelId,
                    favoriteChannelIds,
                ) { clock, epg, nowPlayingId, favorites ->
                    LiveTvCombineRight(clock, epg, nowPlayingId, favorites)
                },
            ) { left, right ->
                val clock = right.clock
                val epg = right.epg
                val nowPlayingId = right.nowPlayingId
                val windowEnd = GuideLayoutHelper.windowEndFromStart(left.windowStart)
                val currentProgramsByChannel = epg
                    .filter { program -> program.startEpochMs <= clock && program.endEpochMs > clock }
                    .associateBy { program -> program.channelId }
                val channels = left.browse.channels.map { channel ->
                    currentProgramsByChannel[channel.id]?.let { program ->
                        GuideLayoutHelper.channelItemWithProgram(channel, program, clock)
                    } ?: channel
                }
                val selectedChannel = left.channel?.let { channel ->
                    currentProgramsByChannel[channel.id]?.let { program ->
                        GuideLayoutHelper.channelItemWithProgram(channel, program, clock)
                    } ?: channel
                }
                val channelPrograms = selectedChannel?.let { channel ->
                    GuideLayoutHelper.programsForSelectedChannel(
                        programs = epg,
                        channel = channel,
                        windowStartMs = left.windowStart,
                        windowEndMs = windowEnd,
                    )
                } ?: emptyList()
                LiveTvUiState(
                    loadState = left.browse.loadState,
                    categories = left.browse.categories,
                    channels = channels,
                    epgPrograms = epg,
                    selectedChannelPrograms = channelPrograms,
                    guideWindowStartMs = left.windowStart,
                    guideWindowEndMs = windowEnd,
                    focusedProgram = left.program,
                    selectedChannel = selectedChannel,
                    nowMs = clock,
                    message = left.browse.message,
                    sourceStatus = left.browse.sourceStatus,
                    sourceType = left.browse.sourceType,
                    nowPlayingChannelId = nowPlayingId,
                    favoriteChannelIds = right.favoriteChannelIds,
                    syncBannerText = _uiState.value.syncBannerText,
                    refreshState = _uiState.value.refreshState,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        startClock()
    }

    fun selectCategory(categoryName: String?) {
        selectedCategory.value = categoryName
        focusedProgram.value = null
    }

    fun setFullGuideOpen(open: Boolean) {
        if (fullGuideOpen.value == open) return
        fullGuideOpen.value = open
        refreshEpg()
    }

    fun requiresCategoryPin(): Boolean {
        val controls = currentParentalControls ?: return false
        return parentalGate.requiresPinForLiveCategories(controls) &&
            !parentalGate.isPinVerified(controls.profileId)
    }

    fun verifyCategoryPin(pin: String): Boolean {
        val controls = currentParentalControls ?: return true
        return parentalGate.verifyPin(controls, pin)
    }

    fun shiftGuideWindow(hours: Int) {
        guideWindowStartMs.value = GuideLayoutHelper.shiftWindowStart(guideWindowStartMs.value, hours)
        focusedProgram.value = null
        refreshEpg()
    }

    fun jumpGuideToNow() {
        val now = System.currentTimeMillis()
        guideWindowStartMs.value = GuideLayoutHelper.defaultWindowStart(now)
        nowMs.value = now
        focusedProgram.value = null
        refreshEpg()
    }

    fun onChannelSelected(channel: ChannelItem) {
        val previousChannelId = selectedChannel.value?.id
        selectedChannel.value = channel
        focusedProgram.value = null
        if (previousChannelId != channel.id && !fullGuideOpen.value) {
            refreshEpg()
        }
        if (previousChannelId != channel.id) {
            startPlayback(channel.id)
        }
    }

    fun onScreenVisible() {
        selectedChannel.value?.id?.let { channelId ->
            if (activePlaybackChannelId != channelId || !playerManager.state.value.isPlaying) {
                startPlayback(channelId)
            }
        }
    }

    fun onScreenHidden() {
        playbackJob?.cancel()
        playerManager.handleCommand(com.iptvcinema.tv.core.player.PlayerCommand.Pause)
    }

    fun clearPlaybackNotice() {
        if (_uiState.value.playbackNotice != null) {
            _uiState.value = _uiState.value.copy(playbackNotice = null)
        }
    }

    fun getExoPlayer() = playerManager.getExoPlayer()

    private fun startPlayback(channelId: String) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val result = runCatching {
                playbackRepository.resolve(channelId, "live")
            }.getOrElse {
                _uiState.value = _uiState.value.copy(
                    playbackNotice = appStrings.get(R.string.error_unable_load_content),
                )
                return@launch
            }
            when (result) {
                is PlaybackResolveResult.Success -> {
                    val session = appSessionRepository.sessionState.first()
                    if (parentalPlaybackGuard.isPlaybackBlocked(session, result.request)) {
                        _uiState.value = _uiState.value.copy(
                            playbackNotice = appStrings.get(R.string.parental_playback_blocked),
                        )
                        return@launch
                    }
                    activePlaybackChannelId = channelId
                    playerManager.play(
                        request = result.request,
                        startPositionMs = 0L,
                        isXtreamSource = session.sourceType == SourceType.XTREAM_CODES,
                    )
                    playbackSessionTracker.setCurrentLiveChannel(channelId)
                    _uiState.value = _uiState.value.copy(playbackNotice = null)
                }
                is PlaybackResolveResult.Error -> {
                    _uiState.value = _uiState.value.copy(playbackNotice = result.message)
                }
            }
        }
    }

    fun selectChannelById(channelId: String?) {
        if (channelId.isNullOrBlank()) return
        val channel = browseState.value.channels.find { it.id == channelId } ?: return
        onChannelSelected(channel)
    }

    fun onProgramFocused(program: EpgProgram) {
        if (program.channelId == selectedChannel.value?.id) {
            focusedProgram.value = program
        }
    }

    fun toggleChannelFavorite(channel: ChannelItem) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            val session = appSessionRepository.sessionState.first()
            runCatching {
                favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = channel.id,
                    contentType = FavoriteContentType.CHANNEL,
                    title = channel.name,
                    posterUrl = channel.logoUrl,
                    sourceId = session.currentSourceId,
                )
            }
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

    private fun refreshEpg() {
        epgLoadJob?.cancel()
        val browse = browseState.value
        if (browse.channels.isEmpty() || browse.loadState != CatalogLoadState.Ready) {
            epgPrograms.value = emptyList()
            return
        }
        val windowStart = guideWindowStartMs.value
        val windowEnd = GuideLayoutHelper.windowEndFromStart(windowStart)
        val channelIds = if (fullGuideOpen.value) {
            LiveTvGuideLoadPolicy.fullGuideChannelIds(browse.channels)
        } else {
            LiveTvGuideLoadPolicy.defaultProgramChannelIds(
                channels = browse.channels,
                selectedChannelId = selectedChannel.value?.id,
            )
        }
        val channelsForPrograms = browse.channels.filter { channel -> channel.id in channelIds }
        epgLoadJob = viewModelScope.launch {
            val session = appSessionRepository.sessionState.first()
            val sourceId = session.currentSourceId
            val programs = if (session.isDemoMode || sourceId == null) {
                FakeDataProvider.epgForChannels(channelsForPrograms, windowStart, windowEnd)
            } else {
                catalogRepository.getEpgForChannels(sourceId, channelIds, windowStart, windowEnd)
            }
            epgPrograms.value = programs
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                nowMs.value = System.currentTimeMillis()
            }
        }
    }

    override fun onCleared() {
        epgLoadJob?.cancel()
        clockJob?.cancel()
        playbackJob?.cancel()
        playbackSessionTracker.setCurrentLiveChannel(null)
        playerManager.release()
        activePlaybackChannelId = null
        super.onCleared()
    }

    private data class LiveTvCombineLeft(
        val browse: LiveTvBrowseSlice,
        val windowStart: Long,
        val program: EpgProgram?,
        val channel: ChannelItem?,
    )

    private data class LiveTvCombineRight(
        val clock: Long,
        val epg: List<EpgProgram>,
        val nowPlayingId: String?,
        val favoriteChannelIds: Set<String>,
    )

    private data class LiveTvBrowseResult(
        val slice: LiveTvBrowseSlice,
        val controls: com.iptvcinema.tv.core.model.ParentalControls?,
        val favoriteChannelIds: Set<String>,
    )

    private fun List<FavoriteItem>.channelIds(): Set<String> =
        filter { it.contentType == FavoriteContentType.CHANNEL }
            .map { it.contentId }
            .toSet()

    private data class LiveTvBrowseSlice(
        val loadState: CatalogLoadState,
        val categories: List<String>,
        val channels: List<ChannelItem>,
        val message: String?,
        val sourceStatus: SourceStatus? = null,
        val sourceType: SourceType? = null,
    )
}
