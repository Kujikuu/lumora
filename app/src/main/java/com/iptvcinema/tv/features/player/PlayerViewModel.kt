package com.iptvcinema.tv.features.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.catalog.SeasonGrouping
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelItem
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelTileData
import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.player.ChannelDirection
import com.iptvcinema.tv.core.player.NextEpisodeResolver
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository
import com.iptvcinema.tv.core.player.PlaybackRepository
import com.iptvcinema.tv.core.player.PlaybackRequest
import com.iptvcinema.tv.core.player.PlaybackResolveResult
import com.iptvcinema.tv.core.player.PlaybackSessionTracker
import com.iptvcinema.tv.core.player.PlayerCommand
import com.iptvcinema.tv.core.player.PlayerManager
import com.iptvcinema.tv.core.player.PlayerUiState
import com.iptvcinema.tv.core.player.WatchHistoryResumePolicy
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.util.AppStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerScreenState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val loadErrorCode: String? = null,
    val playbackRequest: PlaybackRequest? = null,
    val resumeFromMs: Long = 0L,
    val upNextItems: List<PosterCardData> = emptyList(),
    val showAutoplayCountdown: Boolean = false,
    val autoplayCountdownSeconds: Int = 0,
    val channelChangeBanner: String? = null,
    val isEpisode: Boolean = false,
    val seriesId: String? = null,
    val nextEpisodeTitle: String? = null,
    val episodePickerOpen: Boolean = false,
    val episodePickerSeasons: List<SeasonItem> = emptyList(),
    val episodePickerLoading: Boolean = false,
    val channelPickerOpen: Boolean = false,
    val channelPickerChannels: List<ChannelTileData> = emptyList(),
    val channelPickerLoading: Boolean = false,
    val seriesTitle: String? = null,
    val seriesPosterUrl: String? = null,
    val currentLiveProgram: PlayerLiveProgramUi? = null,
    val nextLiveProgram: PlayerLiveProgramUi? = null,
)

data class PlayerLiveProgramUi(
    val title: String,
    val subtitle: String? = null,
    val progress: Float? = null,
    val startMs: Long? = null,
    val endMs: Long? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playbackRepository: PlaybackRepository,
    private val playerManager: PlayerManager,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val appSessionRepository: AppSessionRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val catalogRepository: CatalogRepository,
    private val nextEpisodeResolver: NextEpisodeResolver,
    private val episodeCatalogRepository: EpisodeCatalogRepository,
    private val playbackSessionTracker: PlaybackSessionTracker,
    private val appStrings: AppStrings,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val contentId: String = savedStateHandle.get<String>("contentId").orEmpty()
    private val contentType: String = savedStateHandle.get<String>("contentType").orEmpty()
    private val seriesIdArg: String? = savedStateHandle.get<String>("seriesId")
    private val resumePositionArg: Long? = savedStateHandle.get<Long>("resumePositionMs")?.takeIf { it >= 0L }

    private val _screenState = MutableStateFlow(PlayerScreenState(seriesId = seriesIdArg))
    val screenState: StateFlow<PlayerScreenState> = _screenState.asStateFlow()

    val playerState: StateFlow<PlayerUiState> = playerManager.state

    private var progressSaveJob: Job? = null
    private var positionTickerJob: Job? = null
    private var autoplayJob: Job? = null
    private var channelBannerJob: Job? = null
    private var epgRefreshJob: Job? = null
    private var lastSavedPositionMs: Long = 0L
    private var currentEpisode: CatalogEpisode? = null
    private var autoplayState = AutoplayState.IDLE
    private var autoplaySuppressedContentId: String? = null
    private var isEpisodeTransitionInProgress = false
    private var continueWatchingEnabled = true
    private var autoplayNextEpisode = false
    private var pendingNextEpisode: CatalogEpisode? = null
    private var pendingPreviousRequest: PlaybackRequest? = null
    private var pendingTransitionSourceId: String? = null
    private var channelPickerJob: Job? = null

    init {
        startPositionTicker()
        viewModelScope.launch {
            userSettingsRepository.observeSettings().collect { settings ->
                continueWatchingEnabled = settings?.continueWatchingEnabled ?: true
                autoplayNextEpisode = settings?.autoplayNextEpisode ?: true
            }
        }
        viewModelScope.launch {
            loadPlayback()
        }
        viewModelScope.launch {
            playerManager.state.collect { state ->
                val playbackEnded = state.playbackEnded
                if (playbackEnded) {
                    handlePlaybackEnded()
                    playerManager.clearPlaybackEnded()
                }
                if (!state.isPlaying &&
                    state.hasFirstFrame &&
                    state.errorMessage == null &&
                    !state.isBuffering &&
                    !state.isReconnecting &&
                    !playbackEnded
                ) {
                    scheduleProgressSave()
                }
                if (!playbackEnded) {
                    checkAutoplayThreshold(state)
                }
            }
        }
    }

    fun getExoPlayer() = playerManager.getExoPlayer()

    fun onCommand(command: PlayerCommand) {
        when (command) {
            PlayerCommand.PlayPause, PlayerCommand.Play, PlayerCommand.Pause -> {
                playerManager.handleCommand(command)
                scheduleProgressSave()
            }
            is PlayerCommand.SeekRelative, is PlayerCommand.SeekTo -> {
                playerManager.handleCommand(command)
                scheduleProgressSave()
                if (_screenState.value.showAutoplayCountdown) {
                    cancelAutoplayCountdown()
                }
            }
            PlayerCommand.ChannelPrevious -> changeChannel(ChannelDirection.PREVIOUS)
            PlayerCommand.ChannelNext -> changeChannel(ChannelDirection.NEXT)
            PlayerCommand.EpisodePrevious -> skipToPreviousEpisode()
            PlayerCommand.EpisodeNext -> skipToNextEpisode()
            else -> playerManager.handleCommand(command)
        }
    }

    fun retry() {
        val pendingEpisode = pendingNextEpisode
        val sourceId = pendingTransitionSourceId
        val previousRequest = pendingPreviousRequest
        if (pendingEpisode != null && sourceId != null) {
            _screenState.value = _screenState.value.copy(loadError = null, loadErrorCode = null)
            viewModelScope.launch {
                playEpisode(pendingEpisode, sourceId, previousRequest = previousRequest)
            }
            return
        }
        _screenState.value = _screenState.value.copy(loadError = null, loadErrorCode = null)
        playerManager.handleCommand(PlayerCommand.Retry)
        scheduleProgressSave()
    }

    fun skipToPreviousEpisode() {
        viewModelScope.launch {
            if (isEpisodeTransitionInProgress) return@launch
            val request = _screenState.value.playbackRequest ?: return@launch
            val sourceId = request.sourceId ?: return@launch
            val episode = resolveCurrentEpisode(sourceId, request) ?: return@launch
            val previous = nextEpisodeResolver.previousEpisode(sourceId, episode) ?: return@launch
            dismissAutoplayUi()
            playEpisode(previous, sourceId)
        }
    }

    fun skipToNextEpisode() {
        viewModelScope.launch {
            if (isEpisodeTransitionInProgress) return@launch
            val request = _screenState.value.playbackRequest ?: return@launch
            val sourceId = request.sourceId ?: return@launch
            val episode = resolveCurrentEpisode(sourceId, request) ?: return@launch
            val next = nextEpisodeResolver.nextEpisode(sourceId, episode) ?: run {
                showInfoBanner(appStrings.get(R.string.player_no_next_episode))
                return@launch
            }
            dismissAutoplayUi()
            playEpisode(next, sourceId, previousRequest = request)
        }
    }

    fun playUpNextEpisode(episodeId: String) {
        playEpisodeById(episodeId, markCurrentCompleted = true)
    }

    fun playEpisodeFromPicker(episodeId: String) {
        playEpisodeById(episodeId, markCurrentCompleted = false)
        dismissEpisodePicker()
    }

    fun openEpisodePicker() {
        if (!_screenState.value.isEpisode) return
        viewModelScope.launch {
            val request = _screenState.value.playbackRequest ?: return@launch
            val sourceId = request.sourceId ?: return@launch
            val seriesId = request.seriesId ?: _screenState.value.seriesId ?: seriesIdArg ?: return@launch
            _screenState.value = _screenState.value.copy(
                episodePickerOpen = true,
                episodePickerLoading = true,
            )
            val episodes = episodeCatalogRepository.getEpisodesForSeries(sourceId, seriesId)
            val seasons = SeasonGrouping.toSeasonItems(episodes, seriesId)
            _screenState.value = _screenState.value.copy(
                episodePickerSeasons = seasons,
                episodePickerLoading = false,
            )
        }
    }

    fun dismissEpisodePicker() {
        _screenState.value = _screenState.value.copy(
            episodePickerOpen = false,
            episodePickerSeasons = emptyList(),
            episodePickerLoading = false,
        )
    }

    fun openChannelPicker() {
        val request = _screenState.value.playbackRequest ?: return
        if (!request.isLive) return
        channelPickerJob?.cancel()
        channelPickerJob = viewModelScope.launch {
            val sourceId = request.sourceId ?: return@launch
            _screenState.value = _screenState.value.copy(
                channelPickerOpen = true,
                channelPickerLoading = true,
            )
            try {
                val currentChannel = catalogRepository.getChannel(sourceId, request.contentId)
                val channels = if (currentChannel != null) {
                    catalogRepository.getOrderedChannelsInCategory(
                        sourceId = sourceId,
                        categoryId = currentChannel.categoryId,
                        categoryName = currentChannel.categoryName,
                    )
                } else {
                    emptyList()
                }
                val nowMs = System.currentTimeMillis()
                val nowPlayingId = request.contentId
                val currentPrograms = catalogRepository.getCurrentProgramsForChannels(
                    sourceId = sourceId,
                    channelIds = channels.map { it.id },
                    nowMs = nowMs,
                )
                val noProgramInfoTitle = appStrings.get(R.string.msg_no_program_info)
                val tiles = channels.map { channel ->
                    channel.toChannelItem(
                        currentProgram = currentPrograms[channel.id],
                        nowMs = nowMs,
                        noProgramInfoTitle = noProgramInfoTitle,
                    ).toChannelTileData(nowPlayingChannelId = nowPlayingId)
                }
                _screenState.value = _screenState.value.copy(
                    channelPickerChannels = tiles,
                )
            } finally {
                _screenState.value = _screenState.value.copy(channelPickerLoading = false)
            }
        }
    }

    fun dismissChannelPicker() {
        _screenState.value = _screenState.value.copy(
            channelPickerOpen = false,
            channelPickerChannels = emptyList(),
            channelPickerLoading = false,
        )
    }

    fun switchToChannel(channelId: String) {
        viewModelScope.launch {
            val request = _screenState.value.playbackRequest ?: return@launch
            if (!request.isLive) return@launch
            if (request.contentId == channelId) {
                dismissChannelPicker()
                return@launch
            }
            val sourceId = request.sourceId ?: return@launch
            switchToResolvedChannel(sourceId, channelId)
            dismissChannelPicker()
        }
    }

    private fun playEpisodeById(episodeId: String, markCurrentCompleted: Boolean) {
        viewModelScope.launch {
            if (isEpisodeTransitionInProgress) return@launch
            val request = _screenState.value.playbackRequest ?: return@launch
            val sourceId = request.sourceId ?: return@launch
            if (request.contentId == episodeId) return@launch
            val episode = catalogRepository.getEpisode(sourceId, episodeId)
                ?: episodeCatalogRepository.getEpisode(
                    sourceId,
                    episodeId,
                    request.seriesId ?: seriesIdArg,
                )
                ?: return@launch
            dismissAutoplayUi()
            playEpisode(
                episode = episode,
                sourceId = sourceId,
                previousRequest = request.takeIf { markCurrentCompleted },
            )
        }
    }

    private suspend fun resolveCurrentEpisode(
        sourceId: String,
        request: PlaybackRequest,
    ): CatalogEpisode? = runCatching {
        currentEpisode
            ?: catalogRepository.getEpisode(sourceId, request.contentId)
            ?: episodeCatalogRepository.getEpisode(
                sourceId,
                request.contentId,
                request.seriesId ?: seriesIdArg,
            )
            ?.also { currentEpisode = it }
    }.getOrNull()

    fun cancelAutoplayCountdown() {
        val contentId = _screenState.value.playbackRequest?.contentId ?: return
        autoplaySuppressedContentId = contentId
        autoplayState = AutoplayState.SUPPRESSED
        dismissAutoplayUi(userCancelled = true)
    }

    private fun dismissAutoplayUi(userCancelled: Boolean = false) {
        autoplayJob?.cancel()
        autoplayJob = null
        if (userCancelled) {
            autoplayState = AutoplayState.SUPPRESSED
        } else if (autoplayState == AutoplayState.COUNTDOWN_ACTIVE) {
            autoplayState = AutoplayState.IDLE
        }
        _screenState.value = _screenState.value.copy(
            showAutoplayCountdown = false,
            autoplayCountdownSeconds = 0,
        )
    }

    private suspend fun loadPlayback() {
        val result = runCatching {
            playbackRepository.resolve(contentId, contentType, seriesIdArg)
        }.getOrElse {
            _screenState.value = PlayerScreenState(
                isLoading = false,
                loadError = appStrings.get(R.string.error_unable_load_content),
                loadErrorCode = appStrings.get(R.string.error_catalog_code),
                seriesId = seriesIdArg,
            )
            return
        }
        when (result) {
            is PlaybackResolveResult.Success -> {
                val resumeMs = when {
                    resumePositionArg != null && resumePositionArg > 0L -> resumePositionArg
                    continueWatchingEnabled -> loadResumePosition(result.request)
                    else -> 0L
                }
                _screenState.value = PlayerScreenState(
                    isLoading = false,
                    playbackRequest = result.request,
                    resumeFromMs = resumeMs,
                    isEpisode = result.request.contentType == WatchHistoryContentType.EPISODE,
                    seriesId = result.request.seriesId ?: seriesIdArg,
                )
                playerManager.play(result.request, resumeMs, isXtreamSource())
                updateLivePlaybackSession(result.request)
                scheduleProgressSave()
                loadLiveProgramInfo(result.request)
                startEpgRefresh(result.request)
                loadUpNext(result.request)
                loadSeriesMetadata(result.request)
                prefetchEpisodeCatalog(result.request)
            }
            is PlaybackResolveResult.Error -> {
                _screenState.value = PlayerScreenState(
                    isLoading = false,
                    loadError = result.message,
                    loadErrorCode = result.errorCode,
                    seriesId = seriesIdArg,
                )
            }
        }
    }

    private suspend fun loadSeriesMetadata(request: PlaybackRequest) {
        if (request.contentType != WatchHistoryContentType.EPISODE) return
        val sourceId = request.sourceId ?: return
        val seriesId = request.seriesId ?: seriesIdArg ?: return
        runCatching {
            val series = catalogRepository.getSeries(sourceId, seriesId)
            _screenState.value = _screenState.value.copy(
                seriesTitle = series?.title,
                seriesPosterUrl = series?.posterUrl,
            )
        }
    }

    private suspend fun loadUpNext(request: PlaybackRequest) {
        if (request.contentType != WatchHistoryContentType.EPISODE) return
        val sourceId = request.sourceId ?: return
        val seriesId = request.seriesId ?: seriesIdArg ?: return
        runCatching {
            val cached = catalogRepository.getEpisodesForSeries(sourceId, seriesId)
            val episodes = episodeCatalogRepository.getEpisodesForSeries(
                sourceId = sourceId,
                seriesId = seriesId,
                forceRefresh = cached.isEmpty(),
            )
            val episode = episodes.find { it.id == request.contentId }
                ?: catalogRepository.getEpisode(sourceId, request.contentId)
                ?: episodeCatalogRepository.getEpisode(
                    sourceId,
                    request.contentId,
                    seriesId,
                )
                ?: return
            currentEpisode = episode
            val next = nextEpisodeResolver.nextEpisode(sourceId, episode)
            val upNext = nextEpisodeResolver.upNextEpisodes(sourceId, episode, limit = 3)
            _screenState.value = _screenState.value.copy(
                nextEpisodeTitle = next?.title,
                upNextItems = upNext.map { it.toPosterCardData() },
            )
        }
    }

    private suspend fun prefetchEpisodeCatalog(request: PlaybackRequest) {
        if (request.contentType != WatchHistoryContentType.EPISODE) return
        val sourceId = request.sourceId ?: return
        val seriesId = request.seriesId ?: seriesIdArg ?: return
        runCatching {
            val cached = catalogRepository.getEpisodesForSeries(sourceId, seriesId)
            episodeCatalogRepository.getEpisodesForSeries(
                sourceId = sourceId,
                seriesId = seriesId,
                forceRefresh = cached.isEmpty(),
            )
        }
    }

    private suspend fun playEpisode(
        episode: CatalogEpisode,
        sourceId: String,
        previousRequest: PlaybackRequest? = null,
    ) {
        if (isEpisodeTransitionInProgress) return
        isEpisodeTransitionInProgress = true
        dismissAutoplayUi()
        autoplayState = AutoplayState.TRANSITIONING
        lastSavedPositionMs = 0L
        pendingNextEpisode = episode
        pendingPreviousRequest = previousRequest
        pendingTransitionSourceId = sourceId
        try {
            val result = resolveEpisodeForPlayback(sourceId, episode)
            when (result) {
                is PlaybackResolveResult.Success -> {
                    if (previousRequest != null) {
                        markEpisodeCompleted(previousRequest)
                    }
                    pendingNextEpisode = null
                    pendingPreviousRequest = null
                    pendingTransitionSourceId = null
                    currentEpisode = catalogRepository.getEpisode(sourceId, episode.id) ?: episode
                    autoplaySuppressedContentId = null
                    autoplayState = AutoplayState.IDLE
                    _screenState.value = _screenState.value.copy(
                        playbackRequest = result.request,
                        resumeFromMs = 0L,
                        loadError = null,
                        loadErrorCode = null,
                        isEpisode = true,
                        seriesId = episode.seriesId,
                        upNextItems = emptyList(),
                        nextEpisodeTitle = null,
                        episodePickerOpen = false,
                        episodePickerSeasons = emptyList(),
                    )
                    playerManager.play(result.request, 0L, isXtreamSource())
                    scheduleProgressSave()
                    loadLiveProgramInfo(result.request)
                    stopEpgRefresh()
                    loadUpNext(result.request)
                    loadSeriesMetadata(result.request)
                    prefetchEpisodeCatalog(result.request)
                }
                is PlaybackResolveResult.Error -> {
                    autoplaySuppressedContentId = _screenState.value.playbackRequest?.contentId
                    autoplayState = AutoplayState.SUPPRESSED
                    _screenState.value = _screenState.value.copy(
                        loadError = result.message,
                        loadErrorCode = result.errorCode,
                    )
                }
            }
        } finally {
            isEpisodeTransitionInProgress = false
            if (autoplayState == AutoplayState.TRANSITIONING) {
                autoplayState = AutoplayState.IDLE
            }
        }
    }

    private suspend fun resolveEpisodeForPlayback(
        sourceId: String,
        episode: CatalogEpisode,
    ): PlaybackResolveResult {
        val firstAttempt = runCatching {
            resolveEpisodeAttempt(sourceId, episode)
        }.getOrElse {
            return PlaybackResolveResult.Error(
                message = appStrings.get(R.string.error_unable_load_content),
                errorCode = appStrings.get(R.string.error_catalog_code),
            )
        }
        if (firstAttempt is PlaybackResolveResult.Success) return firstAttempt

        runCatching {
            episodeCatalogRepository.getEpisodesForSeries(
                sourceId = sourceId,
                seriesId = episode.seriesId,
                forceRefresh = true,
            )
        }
        val refreshedEpisode = episodeCatalogRepository.getEpisode(
            sourceId = sourceId,
            episodeId = episode.id,
            seriesId = episode.seriesId,
        ) ?: episode

        return runCatching {
            resolveEpisodeAttempt(sourceId, refreshedEpisode)
        }.getOrElse {
            PlaybackResolveResult.Error(
                message = appStrings.get(R.string.error_unable_load_content),
                errorCode = appStrings.get(R.string.error_catalog_code),
            )
        }
    }

    private suspend fun resolveEpisodeAttempt(
        sourceId: String,
        episode: CatalogEpisode,
    ): PlaybackResolveResult {
        if (episode.streamUrl.isNotBlank()) {
            return playbackRepository.resolveEpisode(sourceId, episode)
        }
        return playbackRepository.resolve(episode.id, "episode", episode.seriesId)
    }

    private fun changeChannel(direction: ChannelDirection) {
        viewModelScope.launch {
            runCatching {
                val request = _screenState.value.playbackRequest ?: return@runCatching
                if (!request.isLive) return@runCatching
                val sourceId = request.sourceId ?: return@runCatching
                val adjacent = catalogRepository.getAdjacentChannel(sourceId, request.contentId, direction)
                    ?: return@runCatching
                switchToResolvedChannel(sourceId, adjacent.id)
            }
        }
    }

    private suspend fun switchToResolvedChannel(sourceId: String, channelId: String) {
        val result = runCatching {
            playbackRepository.resolveChannel(sourceId, channelId)
        }.getOrElse {
            _screenState.value = _screenState.value.copy(
                loadError = appStrings.get(R.string.error_unable_load_content),
                loadErrorCode = appStrings.get(R.string.error_catalog_code),
            )
            return
        }
        when (result) {
            is PlaybackResolveResult.Success -> {
                val channel = runCatching { catalogRepository.getChannel(sourceId, channelId) }.getOrNull()
                val programTitle = runCatching { catalogRepository.getCurrentProgram(sourceId, channelId)?.title }.getOrNull()
                val metadata = buildList {
                    add("LIVE")
                    channel?.categoryName?.takeIf { it.isNotBlank() }?.let { add(it) }
                    programTitle?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
                val updatedRequest = result.request.copy(metadata = metadata)
                _screenState.value = _screenState.value.copy(
                    playbackRequest = updatedRequest,
                    channelChangeBanner = buildString {
                        append(channel?.name ?: result.request.title)
                        programTitle?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                )
                playerManager.play(updatedRequest, 0L, isXtreamSource())
                updateLivePlaybackSession(updatedRequest)
                saveLiveWatchEntry(updatedRequest)
                loadLiveProgramInfo(updatedRequest)
                startEpgRefresh(updatedRequest)
                showChannelBannerBriefly()
            }
            is PlaybackResolveResult.Error -> Unit
        }
    }

    private suspend fun loadLiveProgramInfo(request: PlaybackRequest) {
        if (!request.isLive) {
            stopEpgRefresh()
            _screenState.value = _screenState.value.copy(
                currentLiveProgram = null,
                nextLiveProgram = null,
            )
            return
        }
        val sourceId = request.sourceId ?: return
        val nowMs = System.currentTimeMillis()
        runCatching {
            val programs = catalogRepository.getEpgForChannels(
                sourceId = sourceId,
                channelIds = listOf(request.contentId),
                windowStartMs = nowMs - 5 * 60_000L,
                windowEndMs = nowMs + 4 * 60 * 60_000L,
            ).filter { it.channelId == request.contentId }
                .sortedBy { it.startEpochMs }
            val current = programs.firstOrNull { it.startEpochMs <= nowMs && it.endEpochMs > nowMs }
            val next = programs.firstOrNull { it.startEpochMs > nowMs }
            _screenState.value = _screenState.value.copy(
                currentLiveProgram = current?.toPlayerLiveProgram(nowMs),
                nextLiveProgram = next?.toPlayerLiveProgram(nowMs),
            )
        }
    }

    private fun startEpgRefresh(request: PlaybackRequest) {
        epgRefreshJob?.cancel()
        if (!request.isLive) return
        epgRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(EPG_REFRESH_INTERVAL_MS)
                val currentRequest = _screenState.value.playbackRequest ?: break
                if (!currentRequest.isLive) break
                loadLiveProgramInfo(currentRequest)
            }
        }
    }

    private fun stopEpgRefresh() {
        epgRefreshJob?.cancel()
        epgRefreshJob = null
    }

    private fun EpgProgram.toPlayerLiveProgram(nowMs: Long): PlayerLiveProgramUi {
        val durationMs = (endEpochMs - startEpochMs).coerceAtLeast(1L)
        val progress = if (startEpochMs <= nowMs && endEpochMs > nowMs) {
            ((nowMs - startEpochMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
        return PlayerLiveProgramUi(
            title = title,
            subtitle = "${GuideLayoutHelper.formatSlotLabel(startEpochMs)} - ${GuideLayoutHelper.formatSlotLabel(endEpochMs)}",
            progress = progress,
            startMs = startEpochMs,
            endMs = endEpochMs,
        )
    }

    private fun handlePlaybackEnded() {
        val request = _screenState.value.playbackRequest ?: return
        if (request.isLive || request.contentType != WatchHistoryContentType.EPISODE) return
        if (!autoplayNextEpisode || isEpisodeTransitionInProgress) return
        if (request.contentId == autoplaySuppressedContentId) return
        if (_screenState.value.showAutoplayCountdown) return
        if (autoplayState != AutoplayState.IDLE) return
        viewModelScope.launch {
            val sourceId = request.sourceId ?: return@launch
            val episode = resolveCurrentEpisode(sourceId, request) ?: return@launch
            val next = nextEpisodeResolver.nextEpisode(sourceId, episode) ?: run {
                markEpisodeCompleted(request)
                showInfoBanner(appStrings.get(R.string.player_no_next_episode))
                return@launch
            }
            playEpisode(next, sourceId, previousRequest = request)
        }
    }

    private fun showInfoBanner(message: String) {
        channelBannerJob?.cancel()
        _screenState.value = _screenState.value.copy(channelChangeBanner = message)
        channelBannerJob = viewModelScope.launch {
            delay(3_000)
            _screenState.value = _screenState.value.copy(channelChangeBanner = null)
        }
    }

    private fun showChannelBannerBriefly() {
        channelBannerJob?.cancel()
        channelBannerJob = viewModelScope.launch {
            delay(3_000)
            _screenState.value = _screenState.value.copy(channelChangeBanner = null)
        }
    }

    private fun checkAutoplayThreshold(state: PlayerUiState) {
        val request = _screenState.value.playbackRequest ?: return
        if (request.isLive || request.contentType != WatchHistoryContentType.EPISODE) return
        if (!autoplayNextEpisode || isEpisodeTransitionInProgress) return
        val durationMs = state.durationMs ?: return
        if (durationMs <= 0L) return

        if (WatchHistoryResumePolicy.isBeforeAutoplayWindow(state.positionMs, durationMs)) {
            if (request.contentId != autoplaySuppressedContentId) {
                autoplayState = AutoplayState.IDLE
            }
            return
        }

        if (autoplayState != AutoplayState.IDLE) return
        if (request.contentId == autoplaySuppressedContentId) return
        if (_screenState.value.showAutoplayCountdown) return
        if (!WatchHistoryResumePolicy.shouldShowAutoplay(state.positionMs, durationMs)) return

        autoplayState = AutoplayState.COUNTDOWN_ACTIVE
        startAutoplayCountdown()
    }

    private fun startAutoplayCountdown() {
        autoplayJob?.cancel()
        autoplayJob = viewModelScope.launch {
            val request = _screenState.value.playbackRequest ?: run {
                resetAutoplayIfNotSuppressed()
                return@launch
            }
            if (request.contentId == autoplaySuppressedContentId) {
                autoplayState = AutoplayState.SUPPRESSED
                return@launch
            }
            val sourceId = request.sourceId ?: run {
                resetAutoplayIfNotSuppressed()
                return@launch
            }
            val episode = currentEpisode
                ?: catalogRepository.getEpisode(sourceId, request.contentId)
                ?: episodeCatalogRepository.getEpisode(
                    sourceId,
                    request.contentId,
                    request.seriesId ?: seriesIdArg,
                )
                ?: run {
                    resetAutoplayIfNotSuppressed()
                    return@launch
                }
            currentEpisode = episode
            val next = nextEpisodeResolver.nextEpisode(sourceId, episode) ?: run {
                autoplayState = AutoplayState.IDLE
                return@launch
            }
            if (request.contentId == autoplaySuppressedContentId) {
                autoplayState = AutoplayState.SUPPRESSED
                return@launch
            }
            _screenState.value = _screenState.value.copy(
                showAutoplayCountdown = true,
                autoplayCountdownSeconds = AUTOPLAY_COUNTDOWN_SECONDS,
                nextEpisodeTitle = next.title,
            )
            var remaining = AUTOPLAY_COUNTDOWN_SECONDS
            while (remaining > 0 && isActive) {
                delay(1_000)
                if (request.contentId == autoplaySuppressedContentId) {
                    dismissAutoplayUi(userCancelled = true)
                    return@launch
                }
                remaining--
                _screenState.value = _screenState.value.copy(autoplayCountdownSeconds = remaining)
            }
            if (isActive && request.contentId != autoplaySuppressedContentId) {
                playEpisode(next, sourceId, previousRequest = request)
            } else {
                dismissAutoplayUi(userCancelled = true)
            }
        }
    }

    private fun resetAutoplayIfNotSuppressed() {
        val contentId = _screenState.value.playbackRequest?.contentId
        autoplayState = if (contentId == autoplaySuppressedContentId) {
            AutoplayState.SUPPRESSED
        } else {
            AutoplayState.IDLE
        }
    }

    private suspend fun markEpisodeCompleted(request: PlaybackRequest) {
        if (!continueWatchingEnabled) return
        runCatching {
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return
            val durationMs = resolveDurationMs(playerManager.state.value, request) ?: return
            if (durationMs <= 0L) return
            persistWatchProgress(
                profileId = profileId,
                request = request,
                positionMs = durationMs,
                durationMs = durationMs,
            )
            lastSavedPositionMs = durationMs
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                playerManager.tickPosition()
                delay(500)
            }
        }
    }

    private fun scheduleProgressSave() {
        if (!continueWatchingEnabled) return
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(PROGRESS_SAVE_DEBOUNCE_MS)
            saveProgressNow()
        }
    }

    private suspend fun loadResumePosition(request: PlaybackRequest): Long {
        if (request.isLive) return 0L
        return runCatching {
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return 0L
            val history = watchHistoryRepository.getProgress(
                profileId = profileId,
                contentId = request.contentId,
                contentType = request.contentType,
            ) ?: return 0L
            val durationMs = history.durationMs ?: request.durationMs ?: return 0L
            WatchHistoryResumePolicy.resumePositionMs(history.positionMs, durationMs)
        }.getOrDefault(0L)
    }

    private fun saveProgressNow() {
        if (!continueWatchingEnabled) return
        viewModelScope.launch {
            runCatching {
                val request = _screenState.value.playbackRequest ?: return@runCatching
                if (request.isLive) {
                    saveLiveWatchEntry(request)
                    return@runCatching
                }
                val state = playerManager.state.value
                val positionMs = state.positionMs
                if (positionMs <= 0L) return@runCatching
                if (positionMs == lastSavedPositionMs) return@runCatching
                lastSavedPositionMs = positionMs
                val session = appSessionRepository.sessionState.first()
                val profileId = session.currentProfileId ?: return@runCatching
                val durationMs = resolveDurationMs(state, request)
                persistWatchProgress(
                    profileId = profileId,
                    request = request,
                    positionMs = positionMs,
                    durationMs = durationMs,
                )
                val seriesId = resolveSeriesIdForHistory(request)
                if (request.contentType == WatchHistoryContentType.EPISODE && seriesId != null && request.sourceId != null) {
                    prefetchEpisodesForSeries(request.sourceId, seriesId)
                }
            }
        }
    }

    private suspend fun persistWatchProgress(
        profileId: String,
        request: PlaybackRequest,
        positionMs: Long,
        durationMs: Long?,
    ) {
        watchHistoryRepository.upsertProgress(
            profileId = profileId,
            contentId = request.contentId,
            contentType = request.contentType,
            title = request.title,
            posterUrl = request.posterUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            sourceId = request.sourceId,
            seriesId = resolveSeriesIdForHistory(request),
        )
    }

    private suspend fun resolveSeriesIdForHistory(request: PlaybackRequest): String? {
        request.seriesId?.takeIf { it.isNotBlank() }?.let { return it }
        _screenState.value.seriesId?.takeIf { it.isNotBlank() }?.let { return it }
        seriesIdArg?.takeIf { it.isNotBlank() }?.let { return it }
        if (request.contentType != WatchHistoryContentType.EPISODE) return null
        currentEpisode?.takeIf { it.id == request.contentId }?.seriesId
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        val sourceId = request.sourceId ?: return null
        catalogRepository.getEpisode(sourceId, request.contentId)?.seriesId
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        val navSeriesId = _screenState.value.seriesId ?: seriesIdArg
        episodeCatalogRepository.getEpisode(sourceId, request.contentId, navSeriesId)?.seriesId
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return null
    }

    private fun prefetchEpisodesForSeries(sourceId: String, seriesId: String) {
        viewModelScope.launch {
            runCatching {
                episodeCatalogRepository.getEpisodesForSeries(sourceId, seriesId)
            }
        }
    }

    private suspend fun saveLiveWatchEntry(request: PlaybackRequest) {
        if (!continueWatchingEnabled) return
        runCatching {
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return
            watchHistoryRepository.upsertProgress(
                profileId = profileId,
                contentId = request.contentId,
                contentType = WatchHistoryContentType.CHANNEL,
                title = request.title,
                posterUrl = request.posterUrl,
                positionMs = 0L,
                durationMs = null,
                sourceId = request.sourceId,
            )
        }
    }

    override fun onCleared() {
        val finalRequest = _screenState.value.playbackRequest
        val finalPlayerState = playerManager.state.value
        progressSaveJob?.cancel()
        positionTickerJob?.cancel()
        autoplayJob?.cancel()
        channelBannerJob?.cancel()
        stopEpgRefresh()
        playbackSessionTracker.setCurrentLiveChannel(null)
        if (finalRequest != null) {
            applicationScope.launch {
                saveProgressSnapshot(finalRequest, finalPlayerState)
            }
        }
        playerManager.release()
        super.onCleared()
    }

    private suspend fun saveProgressSnapshot(request: PlaybackRequest, state: PlayerUiState) {
        if (!continueWatchingEnabled) return
        runCatching {
            if (request.isLive) {
                saveLiveWatchEntry(request)
                return@runCatching
            }
            val positionMs = state.positionMs
            if (positionMs <= 0L) return@runCatching
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId ?: return@runCatching
            val durationMs = resolveDurationMs(state, request)
            persistWatchProgress(
                profileId = profileId,
                request = request,
                positionMs = positionMs,
                durationMs = durationMs,
            )
        }
    }

    private fun resolveDurationMs(state: PlayerUiState, request: PlaybackRequest): Long? =
        state.durationMs?.takeIf { it > 0 } ?: request.durationMs?.takeIf { it > 0 }

    private suspend fun isXtreamSource(): Boolean =
        appSessionRepository.sessionState.first().sourceType == SourceType.XTREAM_CODES

    private fun updateLivePlaybackSession(request: PlaybackRequest) {
        playbackSessionTracker.setCurrentLiveChannel(
            request.contentId.takeIf { request.isLive },
        )
    }

    companion object {
        private const val PROGRESS_SAVE_DEBOUNCE_MS = 10_000L
        private const val AUTOPLAY_COUNTDOWN_SECONDS = 10
        private const val EPG_REFRESH_INTERVAL_MS = 60_000L
    }
}

private enum class AutoplayState {
    IDLE,
    COUNTDOWN_ACTIVE,
    SUPPRESSED,
    TRANSITIONING,
}

private fun CatalogEpisode.toPosterCardData(): PosterCardData = PosterCardData(
    title = title,
    runtime = "S${seasonNumber}E$episodeNumber",
    imageUrl = thumbnailUrl,
    contentId = id,
)
