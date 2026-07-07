package com.iptvcinema.tv.features.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.BadgeChip
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    navController: NavController,
    initialChannelId: String? = null,
    initialOpenGuide: Boolean = false,
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

    LaunchedEffect(initialOpenGuide) {
        viewModel.setFullGuideOpen(initialOpenGuide)
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

    CinemaScreen(showTopNav = false) {
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
                            start = 30.dp,
                            end = 30.dp,
                            top = 20.dp,
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
                        .padding(bottom = 38.dp),
                ) {
                    if (categories.isNotEmpty()) {
                        LiveCategoryRow(
                            items = categories,
                            selectedIndex = selectedCategory.coerceIn(0, categories.lastIndex.coerceAtLeast(0)),
                            onSelected = { selectCategoryAt(it) },
                            chipFocusRequester = chipFocus,
                            focusedChipIndex = selectedCategory,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (previewChannel != null) {
                        LiveNowStrip(
                            channel = previewChannel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 120.dp)
                                .focusRequester(channelListFocus),
                            onClick = { playChannel(previewChannel) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveNowStrip(
    channel: ChannelItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.height(130.dp),
        onClick = onClick,
        shape = CinemaShapes.Medium,
        focusScale = 1.01f,
        contentDescription = channel.name,
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (focused) CinemaColors.Surface else CinemaColors.BackgroundSoft),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 17.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .height(54.dp)
                        .aspectRatio(1f)
                        .clip(CinemaShapes.XLarge)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = channel.currentProgram.ifBlank { channel.name },
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = CinemaColors.White,
                            fontWeight = FontWeight.Black,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(3.dp)
                            .clip(CinemaShapes.XLarge)
                            .background(CinemaColors.Surface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(channel.programProgress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(CinemaColors.TextSecondary),
                        )
                    }
                }
                Column(
                    modifier = Modifier.width(180.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.livetv_next_at, channel.programEnd),
                        style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.TextSecondary),
                        maxLines = 1,
                    )
                    Text(
                        text = channel.programDescription.ifBlank { channel.category },
                        style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            BadgeChip(
                text = stringResource(R.string.badge_live),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp),
            )
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveCategoryRow(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chipFocusRequester: FocusRequester? = null,
    focusedChipIndex: Int = 0,
) {
    LazyRow(
        modifier = modifier
            .clip(CinemaShapes.XLarge)
            .background(CinemaColors.SurfaceSoft.copy(alpha = 0.86f))
            .padding(horizontal = 17.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(items) { index, label ->
            val selected = index == selectedIndex
            val colors = LiveCategoryColors[index % LiveCategoryColors.size]
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                FocusableCinemaCard(
                    modifier = Modifier
                        .width(98.dp)
                        .height(63.dp)
                        .then(
                            if (index == focusedChipIndex && chipFocusRequester != null) {
                                Modifier.focusRequester(chipFocusRequester)
                            } else {
                                Modifier
                            },
                        ),
                    onClick = { onSelected(index) },
                    shape = CinemaShapes.XLarge,
                    focusedBorderWidth = 0.dp,
                ) { focused ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(colors),
                                CinemaShapes.XLarge,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (index == 0) "•••" else label.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = CinemaColors.White,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        if (selected || focused) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 5.dp)
                                    .width(11.dp)
                                    .height(4.dp)
                                    .clip(CinemaShapes.XLarge)
                                    .background(CinemaColors.White),
                            )
                        }
                    }
                }
            }
        }
    }
}
