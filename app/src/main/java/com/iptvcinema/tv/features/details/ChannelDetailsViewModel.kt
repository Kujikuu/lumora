package com.iptvcinema.tv.features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toChannelItem
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.util.AppStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChannelDetailsUiState(
    val loadState: DetailsLoadState = DetailsLoadState.Loading,
    val channelId: String = "",
    val channelName: String = "",
    val logoUrl: String? = null,
    val category: String = "",
    val isFavorite: Boolean = false,
    val currentProgram: EpgProgram? = null,
    val nextPrograms: List<EpgProgram> = emptyList(),
    val todayPrograms: List<EpgProgram> = emptyList(),
    val relatedChannels: List<ChannelItem> = emptyList(),
    val nowMs: Long = System.currentTimeMillis(),
    val message: String? = null,
    val isDemoMode: Boolean = false,
)

@HiltViewModel
class ChannelDetailsViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val catalogRepository: CatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChannelDetailsUiState())
    val uiState: StateFlow<ChannelDetailsUiState> = _uiState.asStateFlow()

    private var clockJob: Job? = null

    fun loadChannelDetails(channelId: String) {
        viewModelScope.launch {
            _uiState.value = ChannelDetailsUiState(
                loadState = DetailsLoadState.Loading,
                channelId = channelId,
            )
            val session = appSessionRepository.sessionState.first()
            val profileId = session.currentProfileId
            val isFavorite = if (profileId != null) {
                favoritesRepository.isFavorite(profileId, channelId, FavoriteContentType.CHANNEL)
            } else {
                false
            }

            if (session.isDemoMode) {
                val channel = FakeDataProvider.channels.find { it.id == channelId }
                    ?: FakeDataProvider.channels.firstOrNull()
                if (channel == null) {
                    _uiState.value = ChannelDetailsUiState(
                        loadState = DetailsLoadState.Error,
                        channelId = channelId,
                        message = appStrings.get(R.string.msg_no_live_channels),
                    )
                    return@launch
                }
                val nowMs = System.currentTimeMillis()
                val windowStart = GuideLayoutHelper.defaultWindowStart(nowMs)
                val windowEnd = GuideLayoutHelper.windowEndFromStart(windowStart)
                val programs = GuideLayoutHelper.programsForSelectedChannel(
                    programs = FakeDataProvider.epgForChannels(listOf(channel), windowStart, windowEnd),
                    channel = channel,
                    windowStartMs = windowStart,
                    windowEndMs = windowEnd,
                    fallbackTitle = appStrings.get(R.string.msg_no_program_info),
                )
                _uiState.value = buildReadyState(
                    channelId = channel.id,
                    channelName = channel.name,
                    logoUrl = channel.logoUrl,
                    category = channel.category,
                    isFavorite = isFavorite,
                    programs = programs,
                    nowMs = nowMs,
                    relatedChannels = FakeDataProvider.channels
                        .filter { it.id != channel.id && it.category == channel.category }
                        .take(12),
                    isDemoMode = true,
                )
                startClock()
                return@launch
            }

            val sourceId = session.currentSourceId
            if (sourceId == null) {
                _uiState.value = ChannelDetailsUiState(
                    loadState = DetailsLoadState.Error,
                    channelId = channelId,
                    message = appStrings.get(R.string.msg_no_source_connected),
                )
                return@launch
            }

            val catalogChannel = catalogRepository.getChannel(sourceId, channelId)
            if (catalogChannel == null) {
                _uiState.value = ChannelDetailsUiState(
                    loadState = DetailsLoadState.Error,
                    channelId = channelId,
                    message = appStrings.get(R.string.msg_no_live_channels),
                )
                return@launch
            }

            val nowMs = System.currentTimeMillis()
            val windowStart = GuideLayoutHelper.defaultWindowStart(nowMs)
            val windowEnd = GuideLayoutHelper.windowEndFromStart(windowStart)
            val channelItem = buildChannelItem(sourceId, catalogChannel, nowMs)
            val programs = loadProgramsForChannel(
                sourceId = sourceId,
                channelId = channelId,
                channelItem = channelItem,
                windowStartMs = windowStart,
                windowEndMs = windowEnd,
            )
            val relatedChannels = loadRelatedChannels(
                sourceId = sourceId,
                catalogChannel = catalogChannel,
                excludeChannelId = channelId,
                nowMs = nowMs,
            )

            _uiState.value = buildReadyState(
                channelId = catalogChannel.id,
                channelName = catalogChannel.name,
                logoUrl = catalogChannel.logoUrl,
                category = catalogChannel.categoryName.orEmpty(),
                isFavorite = isFavorite,
                programs = programs,
                nowMs = nowMs,
                relatedChannels = relatedChannels,
                isDemoMode = false,
            )
            startClock()
        }
    }

    fun toggleFavorite(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.loadState != DetailsLoadState.Ready) return@launch
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            val session = appSessionRepository.sessionState.first()
            runCatching {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = state.channelId,
                    contentType = FavoriteContentType.CHANNEL,
                    title = state.channelName,
                    posterUrl = state.logoUrl,
                    sourceId = session.currentSourceId,
                )
                _uiState.value = state.copy(isFavorite = isFavorite)
                onResult(isFavorite)
            }
        }
    }

    private suspend fun buildChannelItem(
        sourceId: String,
        catalogChannel: CatalogChannel,
        nowMs: Long,
    ): ChannelItem {
        val currentProgram = catalogRepository.getCurrentProgramsForChannels(
            sourceId = sourceId,
            channelIds = listOf(catalogChannel.id),
            nowMs = nowMs,
        )[catalogChannel.id]
        return catalogChannel.toChannelItem(
            currentProgram = currentProgram,
            nowMs = nowMs,
            noProgramInfoTitle = appStrings.get(R.string.msg_no_program_info),
        )
    }

    private suspend fun loadProgramsForChannel(
        sourceId: String,
        channelId: String,
        channelItem: ChannelItem,
        windowStartMs: Long,
        windowEndMs: Long,
    ): List<EpgProgram> {
        val programs = catalogRepository.getEpgForChannels(
            sourceId = sourceId,
            channelIds = listOf(channelId),
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs,
        )
        return GuideLayoutHelper.programsForSelectedChannel(
            programs = programs,
            channel = channelItem,
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs,
            fallbackTitle = appStrings.get(R.string.msg_no_program_info),
        )
    }

    private suspend fun loadRelatedChannels(
        sourceId: String,
        catalogChannel: CatalogChannel,
        excludeChannelId: String,
        nowMs: Long,
    ): List<ChannelItem> {
        val categoryId = catalogChannel.categoryId
        val categoryName = catalogChannel.categoryName
        val related = catalogRepository.getOrderedChannels(sourceId)
            .filter { channel ->
                channel.id != excludeChannelId &&
                    (
                        (!categoryId.isNullOrBlank() && channel.categoryId == categoryId) ||
                            (
                                categoryId.isNullOrBlank() &&
                                    !categoryName.isNullOrBlank() &&
                                    channel.categoryName.equals(categoryName, ignoreCase = true)
                                )
                        )
            }
            .take(12)
        if (related.isEmpty()) return emptyList()
        val currentPrograms = catalogRepository.getCurrentProgramsForChannels(
            sourceId = sourceId,
            channelIds = related.map { it.id },
            nowMs = nowMs,
        )
        return related.map { channel ->
            channel.toChannelItem(
                currentProgram = currentPrograms[channel.id],
                nowMs = nowMs,
                noProgramInfoTitle = appStrings.get(R.string.msg_no_program_info),
            )
        }
    }

    private fun buildReadyState(
        channelId: String,
        channelName: String,
        logoUrl: String?,
        category: String,
        isFavorite: Boolean,
        programs: List<EpgProgram>,
        nowMs: Long,
        relatedChannels: List<ChannelItem>,
        isDemoMode: Boolean,
    ): ChannelDetailsUiState {
        val currentProgram = programs.find { program ->
            program.startEpochMs <= nowMs && program.endEpochMs > nowMs
        }
        val nextPrograms = programs
            .filter { program -> program.startEpochMs >= nowMs }
            .take(5)
        return ChannelDetailsUiState(
            loadState = DetailsLoadState.Ready,
            channelId = channelId,
            channelName = channelName,
            logoUrl = logoUrl,
            category = category,
            isFavorite = isFavorite,
            currentProgram = currentProgram ?: programs.firstOrNull(),
            nextPrograms = nextPrograms,
            todayPrograms = programs,
            relatedChannels = relatedChannels,
            nowMs = nowMs,
            isDemoMode = isDemoMode,
        )
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                val nowMs = System.currentTimeMillis()
                val state = _uiState.value
                if (state.loadState != DetailsLoadState.Ready) continue
                val currentProgram = state.todayPrograms.find { program ->
                    program.startEpochMs <= nowMs && program.endEpochMs > nowMs
                }
                val nextPrograms = state.todayPrograms
                    .filter { program -> program.startEpochMs >= nowMs }
                    .take(5)
                _uiState.value = state.copy(
                    nowMs = nowMs,
                    currentProgram = currentProgram ?: state.currentProgram,
                    nextPrograms = nextPrograms,
                )
            }
        }
    }

    override fun onCleared() {
        clockJob?.cancel()
        super.onCleared()
    }
}
