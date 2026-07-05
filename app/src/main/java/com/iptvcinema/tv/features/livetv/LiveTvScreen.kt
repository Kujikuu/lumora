package com.iptvcinema.tv.features.livetv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.ChannelListPanel
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.LivePreviewCard
import com.iptvcinema.tv.core.design.components.MiniChannelEpg
import com.iptvcinema.tv.core.design.components.OnNowPanel
import com.iptvcinema.tv.core.design.components.ProgramGuideGrid
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    navController: NavController,
    initialChannelId: String? = null,
    viewModel: LiveTvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val channelListFocus = remember { FocusRequester() }
    val chipFocus = remember { FocusRequester() }
    val channelFocusState = rememberScreenFocusState("live_tv")
    val categories = uiState.categories
    var selectedCategory by remember { mutableIntStateOf(0) }
    var focusedChannelIndex by remember(channelFocusState.focusIndex, uiState.channels) {
        mutableIntStateOf(
            if (channelFocusState.hasSavedFocus && uiState.channels.isNotEmpty()) {
                channelFocusState.focusIndex.coerceIn(0, uiState.channels.lastIndex)
            } else {
                0
            },
        )
    }
    var showCategoryPin by remember { mutableStateOf(false) }
    var categoryPinError by remember { mutableStateOf<String?>(null) }
    var pendingCategoryIndex by remember { mutableIntStateOf(-1) }
    var showFullGuide by remember { mutableStateOf(false) }
    val incorrectPinMessage = stringResource(R.string.error_incorrect_pin)
    val emptyDescription = stringResource(R.string.msg_no_live_channels_desc)

    fun playChannel(channel: ChannelItem) {
        navController.navigate(AppRoute.player(channel.id, "live"))
    }

    fun selectCategoryAt(index: Int) {
        if (viewModel.requiresCategoryPin()) {
            pendingCategoryIndex = index
            showCategoryPin = true
        } else {
            selectedCategory = index
            viewModel.selectCategory(categories.getOrNull(index))
        }
    }

    if (showCategoryPin) {
        com.iptvcinema.tv.features.parental.PinEntryDialog(
            mode = com.iptvcinema.tv.features.parental.PinEntryMode.Verify,
            title = stringResource(R.string.livetv_pin_category),
            errorMessage = categoryPinError,
            onDismiss = {
                showCategoryPin = false
                categoryPinError = null
                pendingCategoryIndex = -1
            },
            onPinComplete = { pin ->
                if (viewModel.verifyCategoryPin(pin)) {
                    showCategoryPin = false
                    categoryPinError = null
                    if (pendingCategoryIndex >= 0) {
                        selectedCategory = pendingCategoryIndex
                        viewModel.selectCategory(categories.getOrNull(pendingCategoryIndex))
                        pendingCategoryIndex = -1
                    }
                } else {
                    categoryPinError = incorrectPinMessage
                }
            },
        )
    }

    LaunchedEffect(categories.getOrNull(selectedCategory)) {
        viewModel.selectCategory(categories.getOrNull(selectedCategory))
    }

    LaunchedEffect(initialChannelId, uiState.channels) {
        if (!initialChannelId.isNullOrBlank() && uiState.channels.isNotEmpty()) {
            viewModel.selectChannelById(initialChannelId)
            val index = uiState.channels.indexOfFirst { it.id == initialChannelId }
            if (index >= 0) {
                focusedChannelIndex = index
                channelFocusState.saveFocusIndex(index)
            }
        }
    }

    LaunchedEffect(uiState.channels.size, focusedChannelIndex) {
        if (uiState.channels.isNotEmpty()) {
            if (channelFocusState.hasSavedFocus) {
                channelFocusState.restoreFocus(channelListFocus)
            } else {
                channelFocusState.requestInitialFocus(channelListFocus)
                channelFocusState.saveFocusIndex(focusedChannelIndex)
            }
        }
    }

    MainShellBackHandler(navController = navController, isHomeTab = false)
    val previewChannel = uiState.previewChannel

    val catalogCallbacks = rememberCatalogStateCallbacks(
        navController = navController,
        onRetry = viewModel::refreshCurrentSource,
    )

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.LiveTv,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            CatalogRefreshBanner(
                syncBannerText = uiState.syncBannerText,
                refreshState = uiState.refreshState,
                onRefresh = viewModel::refreshCurrentSource,
                modifier = Modifier.padding(horizontal = CinemaSpacing.ContentStart),
            )
            CatalogStateContent(
                loadState = uiState.loadState,
                message = uiState.message,
                sourceStatus = uiState.sourceStatus,
                sourceType = uiState.sourceType,
                skeletonStyle = CatalogSkeletonStyle.Epg,
                emptyTitle = stringResource(R.string.msg_no_live_channels),
                emptyDescription = emptyDescription,
                onAddSource = catalogCallbacks.onAddSource,
                onTryDemo = catalogCallbacks.onTryDemo,
                onRetry = catalogCallbacks.onRetry,
                onManageSources = catalogCallbacks.onManageSources,
                onEditSource = catalogCallbacks.onEditSource,
                onRefreshCatalog = viewModel::refreshCurrentSource,
                modifier = Modifier.weight(1f),
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = CinemaSpacing.ContentStart,
                        end = 24.dp,
                        top = CinemaSpacing.ScreenPaddingVertical,
                    )
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.PageDown -> {
                                viewModel.shiftGuideWindow(GuideLayoutHelper.SHIFT_HOURS)
                                true
                            }
                            Key.PageUp -> {
                                viewModel.shiftGuideWindow(-GuideLayoutHelper.SHIFT_HOURS)
                                true
                            }
                            else -> false
                        }
                    }
                    .padding(bottom = 52.dp),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                if (categories.isNotEmpty()) {
                    FilterChipRow(
                        items = categories,
                        selectedIndex = selectedCategory.coerceIn(0, categories.lastIndex.coerceAtLeast(0)),
                        onSelected = { selectCategoryAt(it) },
                        chipFocusRequester = chipFocus,
                        focusedChipIndex = selectedCategory,
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    if (uiState.channels.isNotEmpty()) {
                        ChannelListPanel(
                            modifier = Modifier.fillMaxHeight(),
                            channels = uiState.channels,
                            selectedChannelId = uiState.selectedChannelId,
                            nowPlayingChannelId = uiState.nowPlayingChannelId,
                            onChannelFocused = { channel ->
                                viewModel.onChannelSelected(channel)
                                val index = uiState.channels.indexOfFirst { it.id == channel.id }
                                if (index >= 0) {
                                    focusedChannelIndex = index
                                    channelFocusState.saveFocusIndex(index)
                                }
                            },
                            onChannelClick = { channel ->
                                viewModel.onChannelSelected(channel)
                                navController.navigate(AppRoute.channelDetails(channel.id))
                            },
                            listFocusRequester = channelListFocus,
                            initialFocusedIndex = focusedChannelIndex.coerceIn(0, uiState.channels.lastIndex),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                    ) {
                        if (previewChannel != null) {
                            LivePreviewCard(
                                channel = previewChannel,
                                isFavorite = uiState.isChannelFavorite(previewChannel.id),
                                onWatchLive = { playChannel(previewChannel) },
                                onDetails = {
                                    navController.navigate(AppRoute.channelDetails(previewChannel.id))
                                },
                                onFavorite = { viewModel.toggleChannelFavorite(previewChannel) },
                            )
                            MiniChannelEpg(
                                programs = uiState.selectedChannelPrograms,
                                windowStartMs = uiState.guideWindowStartMs,
                                windowEndMs = uiState.guideWindowEndMs,
                                nowMs = uiState.nowMs,
                                onProgramClick = { program ->
                                    viewModel.onProgramFocused(program)
                                    playChannel(previewChannel)
                                },
                                onProgramFocused = viewModel::onProgramFocused,
                            )
                            OnNowPanel(
                                channels = uiState.channels,
                                onViewAllGuide = {
                                    showFullGuide = true
                                    viewModel.jumpGuideToNow()
                                },
                                onChannelClick = { channel ->
                                    viewModel.onChannelSelected(channel)
                                    navController.navigate(AppRoute.channelDetails(channel.id))
                                },
                            )
                            if (showFullGuide) {
                                ProgramGuideGrid(
                                    channels = uiState.channels,
                                    programs = uiState.epgPrograms,
                                    windowStartMs = uiState.guideWindowStartMs,
                                    windowEndMs = uiState.guideWindowEndMs,
                                    nowMs = uiState.nowMs,
                                    nowPlayingChannelId = uiState.nowPlayingChannelId,
                                    onProgramClick = { program ->
                                        val channel = uiState.channels.find { it.id == program.channelId }
                                        if (channel != null) {
                                            viewModel.onChannelSelected(channel)
                                            playChannel(channel)
                                        }
                                    },
                                    onProgramFocused = { program, channel ->
                                        viewModel.onChannelSelected(channel)
                                        viewModel.onProgramFocused(program)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
