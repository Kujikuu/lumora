package com.iptvcinema.tv.features.livetv

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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Sports
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.animateToFocusedItem
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
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
        if (isResolvingChannelSelection) return@LaunchedEffect
        viewModel.selectCategory(categories.getOrNull(selectedCategory))
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
        videoSurfaceFocus.requestFocus()
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
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier
            .height(148.dp)
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
        onClick = onInteraction,
        onLongClick = onToggleFavorite,
        shape = CinemaShapes.Card,
        focusScale = 1.01f,
        contentDescription = channel.name,
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (focused) CinemaColors.Surface else CinemaColors.SurfaceSoft.copy(alpha = 0.92f),
                    CinemaShapes.Card,
                ),
        ) {
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.content_desc_favorite),
                    tint = CinemaColors.Accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(CinemaColors.SurfaceSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    CinemaAsyncImage(
                        imageUrl = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        fallbackLabel = channel.name,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                        style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
                        maxLines = 1,
                    )
                    Text(
                        text = nextProgramTitle ?: channel.programDescription.ifBlank { channel.category },
                        style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CinemaShapes.XLarge)
                        .background(CinemaColors.Background),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(channel.programProgress.coerceIn(0f, 1f))
                            .background(CinemaColors.TextSecondary),
                    )
                }
                if (minutesLeft != null) {
                    Text(
                        text = stringResource(R.string.livetv_minutes_left, minutesLeft),
                        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextSecondary),
                    )
                }
            }
            }
        }
    }
}

private val LiveCategoryColors = listOf(
    listOf(Color(0xFFB8FF2C), Color(0xFF00D6FF), Color(0xFFFF7AE8)),
    listOf(Color(0xFFFF1E64), Color(0xFFFF8A00)),
    listOf(Color(0xFFFF77B7), Color(0xFFC767E8)),
    listOf(Color(0xFF13B8E8), Color(0xFF2ED0C3)),
    listOf(Color(0xFFA72BFF), Color(0xFF7E57FF)),
    listOf(Color(0xFFFF3E36), Color(0xFFFF7659)),
    listOf(Color(0xFF18BFAE), Color(0xFF6FDEA6)),
    listOf(Color(0xFF6B7BFF), Color(0xFF90B2FF)),
)

private fun liveCategoryIcon(label: String, index: Int): ImageVector = when {
    index == 0 || label.contains("all", ignoreCase = true) -> Icons.Default.Apps
    label.contains("entertain", ignoreCase = true) -> Icons.Default.EmojiEmotions
    label.contains("movie", ignoreCase = true) -> Icons.Default.Movie
    label.contains("music", ignoreCase = true) -> Icons.Default.MusicNote
    label.contains("kid", ignoreCase = true) -> Icons.Default.ChildCare
    label.contains("news", ignoreCase = true) -> Icons.Default.Campaign
    label.contains("islam", ignoreCase = true) -> Icons.Default.Nightlight
    label.contains("doc", ignoreCase = true) -> Icons.Default.Lightbulb
    label.contains("sport", ignoreCase = true) -> Icons.Default.Sports
    else -> Icons.Default.LiveTv
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveCategoryRow(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var hadFocusInRow by remember(items) { mutableStateOf(false) }
    var focusedIndex by remember(items) { mutableIntStateOf(-1) }

    LazyRow(
        state = listState,
        modifier = modifier
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus) {
                    hadFocusInRow = false
                    focusedIndex = -1
                }
            }
            .clip(CinemaShapes.XLarge)
            .background(CinemaColors.SurfaceSoft.copy(alpha = 0.86f))
            .padding(horizontal = 17.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(items) { index, label ->
            val selected = index == selectedIndex
            val focused = index == focusedIndex
            val colors = LiveCategoryColors[index % LiveCategoryColors.size]
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = when {
                            focused -> CinemaColors.Accent
                            selected -> CinemaColors.White
                            else -> CinemaColors.TextSecondary
                        },
                        fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FocusableCinemaCard(
                    modifier = Modifier
                        .size(63.dp)
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
                    shape = CircleShape,
                    focusedBorderWidth = 3.dp,
                    focusScale = 1.08f,
                    contentDescription = label,
                ) { chipFocused ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(colors),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = liveCategoryIcon(label, index),
                            contentDescription = null,
                            tint = CinemaColors.White,
                            modifier = Modifier.size(28.dp),
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                                    .width(10.dp)
                                    .height(4.dp)
                                    .clip(CinemaShapes.XLarge)
                                    .background(CinemaColors.White),
                            )
                        }
                        if (chipFocused && !selected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        CinemaColors.Background.copy(alpha = 0.18f),
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
