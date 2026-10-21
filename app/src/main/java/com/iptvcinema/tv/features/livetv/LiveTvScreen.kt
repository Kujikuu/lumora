package com.iptvcinema.tv.features.livetv

import com.iptvcinema.tv.core.navigation.requestFocusWhenReady
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.BadgeChip
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.animateToFocusedItem
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.components.KeepScreenOn
import com.iptvcinema.tv.core.design.components.LivePill
import com.iptvcinema.tv.core.design.components.roundedProgressTrack
import com.iptvcinema.tv.core.design.components.PlayerBufferingOverlay
import com.iptvcinema.tv.core.design.components.PlayerRebufferOverlay
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import kotlinx.coroutines.launch
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.util.rememberPrototypeFeedback
import kotlinx.coroutines.delay

private const val OVERLAY_HIDE_DELAY_MS = 4_000L

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    navController: NavController,
    initialChannelId: String? = null,
    initialOpenGuide: Boolean = false,
    viewModel: LiveTvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    KeepScreenOn(enabled = playerState.isPlaying || playerState.isBuffering)
    val activeCategoryName by viewModel.activeCategoryName.collectAsState()
    val isResolvingChannelSelection by viewModel.isResolvingChannelSelection.collectAsState()
    val showFeedback = rememberPrototypeFeedback()
    val feedbackAddedToMyList = stringResource(R.string.feedback_added_to_mylist)
    val feedbackRemovedFromMyList = stringResource(R.string.feedback_removed_from_mylist)
    val channelStripFocus = remember { FocusRequester() }
    val videoSurfaceFocus = remember { FocusRequester() }
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
    var overlaysVisible by remember { mutableStateOf(true) }
    var overlayActivityToken by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val incorrectPinMessage = stringResource(R.string.error_incorrect_pin)
    val emptyDescription = stringResource(R.string.msg_no_live_channels_desc)

    fun revealOverlays() {
        overlaysVisible = true
        overlayActivityToken++
    }

    fun selectChannelAt(index: Int) {
        val channel = uiState.channels.getOrNull(index) ?: return
        focusedChannelIndex = index
        channelFocusState.saveFocusIndex(index)
        viewModel.onChannelSelected(channel)
        revealOverlays()
    }

    fun selectCategoryAt(index: Int) {
        if (viewModel.requiresCategoryPin()) {
            pendingCategoryIndex = index
            showCategoryPin = true
        } else {
            selectedCategory = index
            viewModel.selectCategory(categories.getOrNull(index))
            revealOverlays()
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
                        revealOverlays()
                    }
                } else {
                    categoryPinError = incorrectPinMessage
                }
            },
        )
    }

    LaunchedEffect(categories.getOrNull(selectedCategory), isResolvingChannelSelection) {
        if (isResolvingChannelSelection || categories.isEmpty()) return@LaunchedEffect
        // Only the first sync comes from here; tab presses call the view model directly. Once
        // the view model has a category (a channel opened from Home or My List chose it), a tab
        // index that has not caught up yet must not send Live TV back to the first category.
        val current = categories.getOrNull(selectedCategory)
        val active = activeCategoryName
        if (active != null && !active.equals(current, ignoreCase = true)) return@LaunchedEffect
        viewModel.selectCategory(current)
    }

    LaunchedEffect(initialOpenGuide) {
        viewModel.setFullGuideOpen(initialOpenGuide)
    }

    LaunchedEffect(initialChannelId) {
        if (!initialChannelId.isNullOrBlank()) {
            viewModel.selectChannelById(initialChannelId)
        }
    }

    LaunchedEffect(activeCategoryName, categories) {
        activeCategoryName?.let { categoryName ->
            val index = categories.indexOfFirst { it.equals(categoryName, ignoreCase = true) }
            if (index >= 0) {
                selectedCategory = index
            }
        }
    }

    LaunchedEffect(uiState.channels, uiState.selectedChannel?.id) {
        if (uiState.channels.isEmpty()) return@LaunchedEffect
        val index = uiState.channels.indexOfFirst { it.id == uiState.selectedChannel?.id }
            .takeIf { it >= 0 } ?: 0
        focusedChannelIndex = index
    }

    LaunchedEffect(uiState.channels.size, overlaysVisible) {
        if (uiState.channels.isNotEmpty() && overlaysVisible) {
            if (channelFocusState.hasSavedFocus) {
                channelFocusState.restoreFocus(channelStripFocus)
            } else {
                channelFocusState.requestInitialFocus(channelStripFocus)
                channelFocusState.saveFocusIndex(focusedChannelIndex)
            }
        }
    }

    LaunchedEffect(uiState.playbackNotice) {
        uiState.playbackNotice?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearPlaybackNotice()
        }
    }

    LaunchedEffect(overlaysVisible, overlayActivityToken, uiState.loadState) {
        if (!overlaysVisible || uiState.loadState != CatalogLoadState.Ready) return@LaunchedEffect
        delay(OVERLAY_HIDE_DELAY_MS)
        overlaysVisible = false
        videoSurfaceFocus.requestFocusWhenReady()
    }

    DisposableEffect(Unit) {
        viewModel.onScreenVisible()
        onDispose {
            viewModel.onScreenHidden()
        }
    }

    MainShellBackHandler(navController = navController, isHomeTab = false)
    val previewChannel = uiState.previewChannel
    val nextProgram = uiState.selectedChannelPrograms.getOrNull(1)
        ?: uiState.selectedChannelPrograms.firstOrNull { program ->
            program.channelId == previewChannel?.id && program.startEpochMs > uiState.nowMs
        }
    val currentProgram = uiState.selectedChannelPrograms.firstOrNull { program ->
        program.channelId == previewChannel?.id &&
            program.startEpochMs <= uiState.nowMs &&
            program.endEpochMs > uiState.nowMs
    }
    val minutesLeft = currentProgram?.let { program ->
        ((program.endEpochMs - uiState.nowMs) / 60_000L).toInt().coerceAtLeast(0)
    }

    val catalogCallbacks = rememberCatalogStateCallbacks(
        navController = navController,
        onRetry = viewModel::refreshCurrentSource,
    )
    val isReady = uiState.loadState == CatalogLoadState.Ready && previewChannel != null

    CinemaScreen(showTopNav = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !isReady) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (!overlaysVisible) {
                                if (focusedChannelIndex > 0) {
                                    selectChannelAt(focusedChannelIndex - 1)
                                }
                                true
                            } else {
                                revealOverlays()
                                false
                            }
                        }
                        Key.DirectionRight -> {
                            if (!overlaysVisible) {
                                if (focusedChannelIndex < uiState.channels.lastIndex) {
                                    selectChannelAt(focusedChannelIndex + 1)
                                }
                                true
                            } else {
                                revealOverlays()
                                false
                            }
                        }
                        else -> {
                            if (!overlaysVisible) {
                                revealOverlays()
                            } else {
                                overlayActivityToken++
                            }
                            false
                        }
                    }
                },
        ) {
            if (isReady) {
                LiveTvVideoSurface(viewModel = viewModel)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CinemaColors.Background),
                )
            }

            if (playerState.isBuffering && !playerState.hasFirstFrame) {
                PlayerBufferingOverlay()
            } else if (playerState.isBuffering && playerState.hasFirstFrame || playerState.isReconnecting) {
                PlayerRebufferOverlay()
            }

            CatalogStateContent(
                loadState = uiState.loadState,
                message = uiState.message,
                sourceStatus = uiState.sourceStatus,
                sourceType = uiState.sourceType,
                skeletonStyle = CatalogSkeletonStyle.Epg,
                emptyTitle = stringResource(R.string.msg_no_live_channels),
                emptyDescription = emptyDescription,
                onAddSource = catalogCallbacks.onAddSource,
                onRetry = catalogCallbacks.onRetry,
                onManageSources = catalogCallbacks.onManageSources,
                onEditSource = catalogCallbacks.onEditSource,
                onRefreshCatalog = viewModel::refreshCurrentSource,
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedVisibility(
                    visible = overlaysVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp, vertical = 20.dp),
                    ) {
                        if (!uiState.syncBannerText.isNullOrBlank()) {
                            CatalogRefreshBanner(
                                syncBannerText = uiState.syncBannerText,
                                refreshState = uiState.refreshState,
                                onRefresh = viewModel::refreshCurrentSource,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (categories.isNotEmpty()) {
                            LiveCategoryRow(
                                items = categories,
                                selectedIndex = selectedCategory.coerceIn(0, categories.lastIndex.coerceAtLeast(0)),
                                onSelected = { selectCategoryAt(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.hasFocus) revealOverlays() },
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (previewChannel != null) {
                            LiveChannelStrip(
                                channel = previewChannel,
                                nextProgramTitle = nextProgram?.title,
                                nextProgramTime = nextProgram?.let { formatProgramClock(it) } ?: previewChannel.programEnd,
                                minutesLeft = minutesLeft,
                                canGoPrevious = focusedChannelIndex > 0,
                                canGoNext = focusedChannelIndex < uiState.channels.lastIndex,
                                isFavorite = uiState.isChannelFavorite(previewChannel.id),
                                onPreviousChannel = {
                                    if (focusedChannelIndex > 0) {
                                        selectChannelAt(focusedChannelIndex - 1)
                                    }
                                },
                                onNextChannel = {
                                    if (focusedChannelIndex < uiState.channels.lastIndex) {
                                        selectChannelAt(focusedChannelIndex + 1)
                                    }
                                },
                                onToggleFavorite = {
                                    val wasFavorite = uiState.isChannelFavorite(previewChannel.id)
                                    viewModel.toggleChannelFavorite(previewChannel)
                                    showFeedback(
                                        if (wasFavorite) feedbackRemovedFromMyList else feedbackAddedToMyList,
                                    )
                                },
                                onInteraction = { revealOverlays() },
                                onOpenCatchup = {
                                    navController.navigate(AppRoute.channelDetails(previewChannel.id))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 120.dp)
                                    .focusRequester(channelStripFocus),
                            )
                        }
                    }
                }
            }

            if (isReady && !overlaysVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(videoSurfaceFocus)
                        .focusable(),
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LiveTvVideoSurface(
    viewModel: LiveTvViewModel,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setKeepContentOnPlayerReset(true)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { playerView ->
            playerView.player = viewModel.getExoPlayer()
        },
    )
}

private fun formatProgramClock(program: EpgProgram): String {
    val hour = program.startHour
    val minute = program.startMinute
    return "%02d:%02d".format(hour, minute)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveChannelStrip(
    channel: ChannelItem,
    nextProgramTitle: String?,
    nextProgramTime: String,
    minutesLeft: Int?,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    isFavorite: Boolean,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInteraction: () -> Unit,
    onOpenCatchup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier
            .height(160.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                onInteraction()
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (canGoPrevious) {
                            onPreviousChannel()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionRight -> {
                        if (canGoNext) {
                            onNextChannel()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            },
        // Channels with an archive open their details page, where past programmes can be replayed.
        onClick = if (channel.hasCatchup) onOpenCatchup else onInteraction,
        onLongClick = onToggleFavorite,
        shape = CinemaShapes.XLarge,
        focusScale = 1.01f,
        contentDescription = channel.name,
    ) { focused ->
        // Solid panel (no blur or gradient) so it stays cheap over live video.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (focused) CinemaColors.SurfaceRaised else CinemaColors.Background.copy(alpha = 0.92f),
                    CinemaShapes.XLarge,
                ),
        ) {
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.content_desc_favorite),
                    tint = CinemaColors.Accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LiveStripInfoRow(
                    channel = channel,
                    nextProgramTitle = nextProgramTitle,
                    nextProgramTime = nextProgramTime,
                )
                LiveStripProgressRow(progress = channel.programProgress, minutesLeft = minutesLeft)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveStripInfoRow(
    channel: ChannelItem,
    nextProgramTitle: String?,
    nextProgramTime: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CinemaShapes.XLarge)
                .background(CinemaColors.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            CinemaAsyncImage(
                imageUrl = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentScale = ContentScale.Fit,
                fallbackLabel = channel.name,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LivePill()
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = CinemaColors.TextSecondary,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (channel.hasCatchup) {
                    BadgeChip(
                        text = stringResource(R.string.catchup_badge),
                        backgroundColor = CinemaColors.OverlayFill,
                    )
                }
            }
            Text(
                text = stringResource(R.string.livetv_on_now),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CinemaColors.AccentSoft,
                    fontWeight = FontWeight.ExtraBold,
                ),
                maxLines = 1,
            )
            Text(
                text = channel.currentProgram.ifBlank { channel.name },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier.width(190.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stringResource(R.string.livetv_next_at, nextProgramTime),
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextMuted),
                maxLines = 1,
            )
            Text(
                text = nextProgramTitle ?: channel.programDescription.ifBlank { channel.category },
                style = MaterialTheme.typography.titleSmall.copy(
                    color = CinemaColors.TextSecondary,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveStripProgressRow(progress: Float, minutesLeft: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .roundedProgressTrack(fraction = progress, trackColor = CinemaColors.Border),
        )
        if (minutesLeft != null) {
            Text(
                text = stringResource(R.string.livetv_minutes_left, minutesLeft),
                style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun LiveCategoryRow(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
    )
    val scope = rememberCoroutineScope()
    var hadFocusInRow by remember(items) { mutableStateOf(false) }
    var focusedIndex by remember(items) { mutableIntStateOf(-1) }
    val selectedRequester = remember { FocusRequester() }
    var selectedChipComposed by remember { mutableStateOf(false) }

    // Keep the current category on screen, without re-scrolling when it is already visible.
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex !in items.indices) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == selectedIndex }
        if (!visible) listState.scrollToItem(selectedIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier
            // Entering the row lands on the current category, not the nearest or first chip.
            .focusProperties {
                enter = { if (selectedChipComposed) selectedRequester else FocusRequester.Default }
            }
            .focusGroup()
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus) {
                    hadFocusInRow = false
                    focusedIndex = -1
                }
            }
            // Room for the focus scale; the pills are solid, so no backing panel over the video.
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(items) { index, label ->
            val selected = index == selectedIndex
            if (selected) {
                DisposableEffect(Unit) {
                    selectedChipComposed = true
                    onDispose { selectedChipComposed = false }
                }
            }
            FocusableCinemaCard(
                modifier = Modifier
                    .height(38.dp)
                    .widthIn(max = 240.dp)
                    .then(if (selected) Modifier.focusRequester(selectedRequester) else Modifier)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusedIndex = index
                            scope.launch {
                                hadFocusInRow = true
                                listState.animateToFocusedItem(index)
                            }
                        }
                    },
                onClick = { onSelected(index) },
                shape = CinemaShapes.Pill,
                focusedBorderWidth = 0.dp,
                focusScale = 1.04f,
                contentDescription = label,
            ) { chipFocused ->
                LiveCategoryPill(label = label, selected = selected, focused = chipFocused)
            }
        }
    }
}

/** Category pill: Surface at rest, raised with an accent dot when selected, white when focused. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveCategoryPill(label: String, selected: Boolean, focused: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .background(
                when {
                    focused -> CinemaColors.White
                    selected -> CinemaColors.SurfaceRaised
                    else -> CinemaColors.Surface
                },
                CinemaShapes.Pill,
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected && !focused) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(CinemaColors.Accent, CircleShape),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = when {
                    focused -> CinemaColors.Background
                    selected -> CinemaColors.White
                    else -> CinemaColors.TextSecondary
                },
                fontWeight = if (focused || selected) FontWeight.ExtraBold else FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
