package com.iptvcinema.tv.features.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.player.PlaybackSessionTracker
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
}

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val playbackSessionTracker: PlaybackSessionTracker,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val guideWindowStartMs = MutableStateFlow(GuideLayoutHelper.defaultWindowStart(System.currentTimeMillis()))
    private val focusedProgram = MutableStateFlow<EpgProgram?>(null)
    private val selectedChannel = MutableStateFlow<ChannelItem?>(null)
    private val nowMs = MutableStateFlow(System.currentTimeMillis())
    private val epgPrograms = MutableStateFlow<List<EpgProgram>>(emptyList())
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

    private var epgLoadJob: Job? = null
    private var clockJob: Job? = null

    init {
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
                combine(
                    catalogRepository.observeLiveTv(categoryName),
                    controlsFlow,
                ) { state, controls ->
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
                    )
                }
            }.collect { result ->
                browseState.value = result.slice
                currentParentalControls = result.controls
                val slice = result.slice
                if (slice.loadState == CatalogLoadState.Ready && slice.channels.isNotEmpty()) {
                    selectedChannel.value = selectedChannel.value?.takeIf { selected ->
                        slice.channels.any { it.id == selected.id }
                    } ?: slice.channels.firstOrNull()
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
                combine(nowMs, epgPrograms, playbackSessionTracker.currentLiveChannelId) { clock, epg, nowPlayingId ->
                    Triple(clock, epg, nowPlayingId)
                },
            ) { left, right ->
                val (clock, epg, nowPlayingId) = right
                val windowEnd = GuideLayoutHelper.windowEndFromStart(left.windowStart)
                val channelPrograms = left.channel?.let { channel ->
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
                    channels = left.browse.channels,
                    epgPrograms = epg,
                    selectedChannelPrograms = channelPrograms,
                    guideWindowStartMs = left.windowStart,
                    guideWindowEndMs = windowEnd,
                    focusedProgram = left.program,
                    selectedChannel = left.channel,
                    nowMs = clock,
                    message = left.browse.message,
                    sourceStatus = left.browse.sourceStatus,
                    sourceType = left.browse.sourceType,
                    nowPlayingChannelId = nowPlayingId,
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
        selectedChannel.value = channel
        focusedProgram.value = null
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

    private fun refreshEpg() {
        epgLoadJob?.cancel()
        val browse = browseState.value
        if (browse.channels.isEmpty() || browse.loadState != CatalogLoadState.Ready) {
            epgPrograms.value = emptyList()
            return
        }
        val windowStart = guideWindowStartMs.value
        val windowEnd = GuideLayoutHelper.windowEndFromStart(windowStart)
        val channelIds = browse.channels.map { it.id }
        epgLoadJob = viewModelScope.launch {
            val session = appSessionRepository.sessionState.first()
            val sourceId = session.currentSourceId
            val programs = if (session.isDemoMode || sourceId == null) {
                FakeDataProvider.epgForChannels(browse.channels, windowStart, windowEnd)
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
        super.onCleared()
    }

    private data class LiveTvCombineLeft(
        val browse: LiveTvBrowseSlice,
        val windowStart: Long,
        val program: EpgProgram?,
        val channel: ChannelItem?,
    )

    private data class LiveTvBrowseResult(
        val slice: LiveTvBrowseSlice,
        val controls: com.iptvcinema.tv.core.model.ParentalControls?,
    )

    private data class LiveTvBrowseSlice(
        val loadState: CatalogLoadState,
        val categories: List<String>,
        val channels: List<ChannelItem>,
        val message: String?,
        val sourceStatus: SourceStatus? = null,
        val sourceType: SourceType? = null,
    )
}
