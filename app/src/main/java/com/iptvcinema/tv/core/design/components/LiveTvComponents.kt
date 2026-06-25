package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram

private fun minutesRemaining(channel: ChannelItem): Int {
    val remainingFraction = 1f - channel.programProgress.coerceIn(0f, 1f)
    return (remainingFraction * 60).toInt().coerceAtLeast(1)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelListPanel(
    channels: List<ChannelItem>,
    selectedChannelId: String?,
    nowPlayingChannelId: String?,
    onChannelFocused: (ChannelItem) -> Unit,
    onChannelClick: (ChannelItem) -> Unit,
    modifier: Modifier = Modifier,
    listFocusRequester: FocusRequester? = null,
    initialFocusedIndex: Int = 0,
) {
    Column(
        modifier = modifier
            .width(320.dp)
            .clip(CinemaShapes.Medium)
            .background(CinemaColors.SurfaceSoft)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(title = stringResource(R.string.livetv_channels))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                ChannelListRow(
                    channel = channel,
                    isSelected = channel.id == selectedChannelId,
                    isNowPlaying = channel.id == nowPlayingChannelId,
                    onFocused = { onChannelFocused(channel) },
                    onClick = { onChannelClick(channel) },
                    modifier = if (index == initialFocusedIndex && listFocusRequester != null) {
                        Modifier.focusRequester(listFocusRequester)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelListRow(
    channel: ChannelItem,
    isSelected: Boolean,
    isNowPlaying: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var wasFocused by remember(channel.id) { mutableStateOf(false) }
    FocusableCinemaCard(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(
                if (isNowPlaying) {
                    Modifier.border(1.dp, CinemaColors.Accent.copy(alpha = 0.6f), CinemaShapes.Small)
                } else {
                    Modifier
                },
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !wasFocused) {
                    wasFocused = true
                    onFocused()
                } else if (!focusState.isFocused) {
                    wasFocused = false
                }
            },
        onClick = onClick,
        shape = CinemaShapes.Small,
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        isFocused -> CinemaColors.Surface
                        isSelected -> CinemaColors.SurfaceSoft
                        else -> CinemaColors.Background
                    },
                    CinemaShapes.Small,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channel.channelNumber.toString(),
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                modifier = Modifier.width(24.dp),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.SurfaceSoft),
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
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isNowPlaying || isFocused) CinemaColors.White else CinemaColors.TextPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.currentProgram,
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LivePreviewCard(
    channel: ChannelItem,
    onWatchFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CinemaShapes.Medium)
            .background(CinemaColors.SurfaceSoft)
            .padding(CinemaSpacing.ButtonGap),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(CinemaShapes.Small)
                .background(CinemaColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            CinemaAsyncImage(
                imageUrl = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallbackLabel = channel.name,
            )
            BadgeChip(
                text = stringResource(R.string.badge_live),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )
        }
        Text(
            text = channel.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
            ),
        )
        Text(
            text = channel.currentProgram,
            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
        )
        Text(
            text = stringResource(
                R.string.livetv_program_time,
                channel.programStart,
                channel.programEnd,
                pluralStringResource(
                    R.plurals.minutes_remaining,
                    minutesRemaining(channel),
                    minutesRemaining(channel),
                ),
            ),
            style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
        )
        if (channel.programDescription.isNotBlank()) {
            Text(
                text = channel.programDescription,
                style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CinemaShapes.Small)
                .background(CinemaColors.Surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(channel.programProgress.coerceIn(0f, 1f))
                    .background(CinemaColors.Gold),
            )
        }
        CinemaButton(
            text = stringResource(R.string.btn_watch_fullscreen),
            variant = CinemaButtonVariant.PrimaryAccent,
            onClick = onWatchFullscreen,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MiniChannelEpg(
    programs: List<EpgProgram>,
    windowStartMs: Long,
    windowEndMs: Long,
    nowMs: Long,
    modifier: Modifier = Modifier,
    onProgramClick: (EpgProgram) -> Unit = {},
    onProgramFocused: (EpgProgram) -> Unit = {},
) {
    val timelineSlots = remember(windowStartMs, windowEndMs) {
        GuideLayoutHelper.timelineSlotStarts(windowStartMs, windowEndMs)
    }
    val dateLabel = remember(windowStartMs) { GuideLayoutHelper.formatDateLabel(windowStartMs) }
    val currentTimeLabel = remember(nowMs) { GuideLayoutHelper.formatCurrentTimeLabel(nowMs) }
    val windowDurationMs = GuideLayoutHelper.windowDurationMs(windowStartMs, windowEndMs)
    val currentTimeFraction = GuideLayoutHelper.currentTimeFraction(nowMs, windowStartMs, windowEndMs)
    val rowHeight = 56.dp

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.livetv_tv_guide),
                style = MaterialTheme.typography.titleMedium.copy(color = CinemaColors.White),
            )
            CategoryChip(label = dateLabel, isSelected = true, onClick = {})
        }
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    timelineSlots.forEach { slotMs ->
                        Text(
                            text = GuideLayoutHelper.formatSlotLabel(slotMs),
                            modifier = Modifier.weight(1f).padding(8.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (slotMs <= nowMs && nowMs < slotMs + GuideLayoutHelper.SLOT_DURATION_MS) {
                                    CinemaColors.Gold
                                } else {
                                    CinemaColors.TextMuted
                                },
                                fontWeight = if (slotMs <= nowMs && nowMs < slotMs + GuideLayoutHelper.SLOT_DURATION_MS) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                ) {
                    MiniEpgProgramRow(
                        programs = programs,
                        windowStartMs = windowStartMs,
                        windowDurationMs = windowDurationMs,
                        rowHeight = rowHeight,
                        onProgramClick = onProgramClick,
                        onProgramFocused = onProgramFocused,
                    )
                    if (currentTimeFraction != null) {
                        val density = LocalDensity.current
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val timelineWidthPx = with(density) { maxWidth.toPx() }
                            val offsetPx = timelineWidthPx * currentTimeFraction
                            Box(
                                modifier = Modifier
                                    .offset(x = with(density) { (offsetPx / density.density).dp })
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(CinemaColors.Gold),
                            )
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = with(density) { ((offsetPx - 28f * density.density) / density.density).dp },
                                        y = (-8).dp,
                                    )
                                    .background(CinemaColors.Gold, CinemaShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = currentTimeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CinemaColors.Background,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MiniEpgProgramRow(
    programs: List<EpgProgram>,
    windowStartMs: Long,
    windowDurationMs: Long,
    rowHeight: androidx.compose.ui.unit.Dp,
    onProgramClick: (EpgProgram) -> Unit,
    onProgramFocused: (EpgProgram) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight),
    ) {
        val timelineWidth = maxWidth
        programs.forEach { program ->
            val startFraction = GuideLayoutHelper.programStartOffsetFraction(
                program = program,
                windowStartMs = windowStartMs,
                windowDurationMs = windowDurationMs,
            )
            val widthFraction = GuideLayoutHelper.programWidthFraction(
                program = program,
                windowDurationMs = windowDurationMs,
            )
            val cellWidth = timelineWidth * widthFraction
            val xOffset = timelineWidth * startFraction
            var wasFocused by remember(program.id) { mutableStateOf(false) }
            FocusableCinemaCard(
                modifier = Modifier
                    .offset(x = xOffset)
                    .width(cellWidth.coerceAtLeast(48.dp))
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !wasFocused) {
                            wasFocused = true
                            onProgramFocused(program)
                        } else if (!focusState.isFocused) {
                            wasFocused = false
                        }
                    },
                onClick = { onProgramClick(program) },
                shape = CinemaShapes.Small,
            ) { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CinemaColors.Surface, CinemaShapes.Small)
                        .padding(6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column {
                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = GuideLayoutHelper.formatSlotLabel(program.startEpochMs),
                            style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OnNowPanel(
    channels: List<ChannelItem>,
    onViewAllGuide: () -> Unit = {},
    onChannelClick: (ChannelItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CinemaShapes.Large)
            .background(CinemaColors.SurfaceSoft)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.livetv_on_now))
            CinemaButton(
                text = stringResource(R.string.livetv_view_guide),
                variant = CinemaButtonVariant.Ghost,
                onClick = onViewAllGuide,
            )
        }
        channels.take(5).forEach { channel ->
            FocusableCinemaCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onChannelClick(channel) },
                shape = CinemaShapes.Medium,
            ) { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.minutes_remaining,
                                minutesRemaining(channel),
                                minutesRemaining(channel),
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.Gold),
                        )
                    }
                    Text(
                        text = channel.currentProgram,
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CinemaShapes.Small)
                            .background(CinemaColors.Surface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(channel.programProgress.coerceIn(0f, 1f))
                                .background(CinemaColors.Gold),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgramGuideGrid(
    channels: List<ChannelItem>,
    programs: List<EpgProgram>,
    windowStartMs: Long,
    windowEndMs: Long,
    nowMs: Long,
    modifier: Modifier = Modifier,
    nowPlayingChannelId: String? = null,
    onProgramClick: (EpgProgram) -> Unit = {},
    onProgramFocused: (EpgProgram, ChannelItem) -> Unit = { _, _ -> },
) {
    val channelColumnWidth = 160.dp
    val rowHeight = 56.dp
    val timelineSlots = remember(windowStartMs, windowEndMs) {
        GuideLayoutHelper.timelineSlotStarts(windowStartMs, windowEndMs)
    }
    val dateLabel = remember(windowStartMs) { GuideLayoutHelper.formatDateLabel(windowStartMs) }
    val currentTimeLabel = remember(nowMs) { GuideLayoutHelper.formatCurrentTimeLabel(nowMs) }
    val windowDurationMs = GuideLayoutHelper.windowDurationMs(windowStartMs, windowEndMs)
    val currentTimeFraction = GuideLayoutHelper.currentTimeFraction(nowMs, windowStartMs, windowEndMs)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.livetv_tv_guide),
                style = MaterialTheme.typography.titleMedium.copy(color = CinemaColors.White),
            )
            CategoryChip(label = dateLabel, isSelected = true, onClick = {})
        }
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(channelColumnWidth))
                    timelineSlots.forEach { slotMs ->
                        Text(
                            text = GuideLayoutHelper.formatSlotLabel(slotMs),
                            modifier = Modifier.weight(1f).padding(8.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (slotMs <= nowMs && nowMs < slotMs + GuideLayoutHelper.SLOT_DURATION_MS) {
                                    CinemaColors.Gold
                                } else {
                                    CinemaColors.TextMuted
                                },
                                fontWeight = if (slotMs <= nowMs && nowMs < slotMs + GuideLayoutHelper.SLOT_DURATION_MS) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(channels, key = { it.id }) { channel ->
                            GuideChannelRow(
                                channel = channel,
                                isNowPlaying = channel.id == nowPlayingChannelId,
                                programs = GuideLayoutHelper.programsForChannel(
                                    programs = programs,
                                    channelId = channel.id,
                                    windowStartMs = windowStartMs,
                                    windowEndMs = windowEndMs,
                                ),
                                channelColumnWidth = channelColumnWidth,
                                rowHeight = rowHeight,
                                windowStartMs = windowStartMs,
                                windowDurationMs = windowDurationMs,
                                onProgramClick = onProgramClick,
                                onProgramFocused = { program -> onProgramFocused(program, channel) },
                            )
                        }
                    }
                    if (currentTimeFraction != null) {
                        val density = LocalDensity.current
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val timelineWidthPx = with(density) {
                                (maxWidth - channelColumnWidth).toPx()
                            }
                            val offsetPx = channelColumnWidth.value * density.density +
                                timelineWidthPx * currentTimeFraction
                            Box(
                                modifier = Modifier
                                    .offset(x = with(density) { (offsetPx / density.density).dp })
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(CinemaColors.Gold),
                            )
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = with(density) { ((offsetPx - 28f * density.density) / density.density).dp },
                                        y = (-8).dp,
                                    )
                                    .background(CinemaColors.Gold, CinemaShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = currentTimeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CinemaColors.Background,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideChannelRow(
    channel: ChannelItem,
    isNowPlaying: Boolean,
    programs: List<EpgProgram>,
    channelColumnWidth: androidx.compose.ui.unit.Dp,
    rowHeight: androidx.compose.ui.unit.Dp,
    windowStartMs: Long,
    windowDurationMs: Long,
    onProgramClick: (EpgProgram) -> Unit,
    onProgramFocused: (EpgProgram) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(rowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(channelColumnWidth).padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channel.channelNumber.toString(),
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
            )
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isNowPlaying) CinemaColors.Gold else CinemaColors.TextPrimary,
                    fontWeight = if (isNowPlaying) FontWeight.SemiBold else FontWeight.Normal,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val timelineWidth = maxWidth
            programs.forEach { program ->
                val startFraction = GuideLayoutHelper.programStartOffsetFraction(
                    program = program,
                    windowStartMs = windowStartMs,
                    windowDurationMs = windowDurationMs,
                )
                val widthFraction = GuideLayoutHelper.programWidthFraction(
                    program = program,
                    windowDurationMs = windowDurationMs,
                )
                val cellWidth = timelineWidth * widthFraction
                val xOffset = timelineWidth * startFraction
                var wasFocused by remember(program.id) { mutableStateOf(false) }
                FocusableCinemaCard(
                    modifier = Modifier
                        .offset(x = xOffset)
                        .width(cellWidth.coerceAtLeast(48.dp))
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && !wasFocused) {
                                wasFocused = true
                                onProgramFocused(program)
                            } else if (!focusState.isFocused) {
                                wasFocused = false
                            }
                        },
                    onClick = { onProgramClick(program) },
                    shape = CinemaShapes.Small,
                ) { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CinemaColors.Surface, CinemaShapes.Small)
                            .padding(6.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                text = program.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = GuideLayoutHelper.formatSlotLabel(program.startEpochMs),
                                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
