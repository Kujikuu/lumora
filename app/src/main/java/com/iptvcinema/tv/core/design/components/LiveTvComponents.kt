package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram

private fun minutesRemaining(channel: ChannelItem): Int {
    val remainingFraction = 1f - channel.programProgress.coerceIn(0f, 1f)
    return (remainingFraction * 60).toInt().coerceAtLeast(1)
}

/** Rounded square behind channel logos in rows, tiles and the guide. */
internal val ChannelLogoShape = RoundedCornerShape(9.dp)
private val QualityTagShape = RoundedCornerShape(6.dp)

/** Light track behind the accent fill on a white (focused) row. */
private val FocusedTrackColor = Color(0xFFD6D8DC)

/** Red LIVE pill with a white dot. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LivePill(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.badge_live),
) {
    Row(
        modifier = modifier
            .background(CinemaColors.LiveRed, CinemaShapes.Pill)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(CinemaColors.White, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Black,
            ),
            maxLines = 1,
        )
    }
}

/** Small outlined quality tag (HD, FHD, 4K) that takes the row's text colour. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ChannelQualityTag(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.55f), QualityTagShape)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = color.copy(alpha = 0.7f),
            fontWeight = FontWeight.Black,
        ),
        maxLines = 1,
    )
}

/** Rounded logo tile; the logo is fitted, not cropped, so wide marks stay readable. */
@Composable
internal fun ChannelLogoTile(
    logoUrl: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = ChannelLogoShape,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(CinemaColors.SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        CinemaAsyncImage(
            imageUrl = logoUrl,
            contentDescription = name,
            modifier = Modifier
                .fillMaxSize()
                .padding(size / 10),
            contentScale = ContentScale.Fit,
            fallbackLabel = name,
        )
    }
}

/** "ON NOW" style label in the accent colour. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AccentLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(
            color = CinemaColors.AccentSoft,
            fontWeight = FontWeight.ExtraBold,
        ),
        maxLines = 1,
    )
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
        modifier = modifier.width(320.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(title = stringResource(R.string.livetv_channels))
        LazyColumn(
            modifier = Modifier.weight(1f),
            // Room for the focused row's scale inside the list's clip.
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
            .height(56.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !wasFocused) {
                    wasFocused = true
                    onFocused()
                } else if (!focusState.isFocused) {
                    wasFocused = false
                }
            },
        onClick = onClick,
        shape = CinemaShapes.XLarge,
        focusedBorderWidth = 0.dp,
        focusScale = 1.03f,
    ) { isFocused ->
        val contentColor = if (isFocused) CinemaColors.Background else CinemaColors.White
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        isFocused -> CinemaColors.White
                        isSelected -> CinemaColors.SurfaceRaised
                        else -> CinemaColors.SurfaceSoft
                    },
                    CinemaShapes.XLarge,
                )
                .then(
                    if (isNowPlaying && !isFocused) {
                        Modifier.border(2.dp, CinemaColors.Accent, CinemaShapes.XLarge)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channel.channelNumber.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = contentColor.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                modifier = Modifier.width(26.dp),
            )
            ChannelLogoTile(logoUrl = channel.logoUrl, name = channel.name, size = 36.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = contentColor,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    channel.qualityBadge?.takeIf { it.isNotBlank() }?.let { quality ->
                        ChannelQualityTag(label = quality, color = contentColor)
                    }
                }
                Text(
                    text = channel.currentProgram,
                    style = MaterialTheme.typography.labelSmall.copy(color = contentColor.copy(alpha = 0.75f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (channel.hasCatchup) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.catchup_badge),
                    tint = contentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .roundedProgressTrack(
                        fraction = channel.programProgress,
                        trackColor = if (isFocused) FocusedTrackColor else CinemaColors.Border,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LivePreviewCard(
    channel: ChannelItem,
    onWatchLive: () -> Unit,
    onDetails: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(CinemaShapes.XLarge)
                .background(CinemaColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            CinemaAsyncImage(
                imageUrl = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentScale = ContentScale.Fit,
                fallbackLabel = channel.name,
            )
            LivePill(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccentLabel(text = stringResource(R.string.livetv_on_now))
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = channel.currentProgram,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .roundedProgressTrack(
                    fraction = channel.programProgress,
                    trackColor = CinemaColors.Border,
                ),
        )
        CinemaButton(
            text = stringResource(R.string.btn_watch_live),
            variant = CinemaButtonVariant.SecondaryDark,
            icon = Icons.Default.PlayArrow,
            onClick = onWatchLive,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CinemaButton(
                text = stringResource(R.string.btn_details),
                variant = CinemaButtonVariant.SecondaryDark,
                icon = Icons.Default.Info,
                onClick = onDetails,
            )
            CinemaButton(
                text = "",
                variant = CinemaButtonVariant.Icon,
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                onClick = onFavorite,
            )
        }
    }
}

/** Header row shared by the guide grids: title and the date pill. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideHeader(
    dateLabel: String,
    large: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.livetv_tv_guide),
            style = if (large) {
                MaterialTheme.typography.headlineSmall.copy(color = CinemaColors.White, fontWeight = FontWeight.Black)
            } else {
                MaterialTheme.typography.titleMedium.copy(color = CinemaColors.White, fontWeight = FontWeight.ExtraBold)
            },
        )
        CategoryChip(label = dateLabel, isSelected = true, onClick = {})
    }
}

/** Half-hour labels above the grid; the current slot is in the accent colour. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideTimeline(
    timelineSlots: List<Long>,
    nowMs: Long,
    leadingWidth: Dp,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (leadingWidth > 0.dp) {
            Box(modifier = Modifier.width(leadingWidth))
        }
        timelineSlots.forEach { slotMs ->
            val isCurrentSlot = slotMs <= nowMs && nowMs < slotMs + GuideLayoutHelper.SLOT_DURATION_MS
            Text(
                text = GuideLayoutHelper.formatSlotLabel(slotMs),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isCurrentSlot) CinemaColors.AccentSoft else CinemaColors.TextMuted,
                    fontWeight = FontWeight.ExtraBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Accent "now" line with its time pill, offset from the start of the timeline. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideNowLine(
    currentTimeFraction: Float,
    currentTimeLabel: String,
    leadingWidth: Dp,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val timelineWidthPx = with(density) { (maxWidth - leadingWidth).toPx() }
        val offsetPx = leadingWidth.value * density.density + timelineWidthPx * currentTimeFraction
        Box(
            modifier = Modifier
                .offset(x = with(density) { (offsetPx / density.density).dp })
                .width(2.dp)
                .fillMaxHeight()
                .background(CinemaColors.Accent),
        )
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { ((offsetPx - 28f * density.density) / density.density).dp },
                    y = (-8).dp,
                )
                .background(CinemaColors.Accent, CinemaShapes.Pill)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = currentTimeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
            )
        }
    }
}

/** One programme block: rounded Surface cell, white with dark text when focused. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideProgramCell(
    program: EpgProgram,
    xOffset: Dp,
    cellWidth: Dp,
    onProgramClick: (EpgProgram) -> Unit,
    onProgramFocused: (EpgProgram) -> Unit,
) {
    var wasFocused by remember(program.id) { mutableStateOf(false) }
    FocusableCinemaCard(
        modifier = Modifier
            .offset(x = xOffset)
            .width(cellWidth.coerceAtLeast(48.dp))
            .fillMaxHeight()
            .padding(horizontal = 1.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !wasFocused) {
                    wasFocused = true
                    onProgramFocused(program)
                } else if (!focusState.isFocused) {
                    wasFocused = false
                }
            },
        onClick = { onProgramClick(program) },
        shape = CinemaShapes.Card,
        focusedBorderWidth = 0.dp,
        focusScale = 1.02f,
    ) { focused ->
        val contentColor = if (focused) CinemaColors.Background else CinemaColors.White
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (focused) CinemaColors.White else CinemaColors.Surface, CinemaShapes.Card)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = contentColor,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = GuideLayoutHelper.formatSlotLabel(program.startEpochMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = contentColor.copy(alpha = 0.7f)),
                    maxLines = 1,
                )
            }
        }
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
        GuideHeader(dateLabel = dateLabel, large = false)
        Column(modifier = Modifier.fillMaxWidth()) {
            GuideTimeline(timelineSlots = timelineSlots, nowMs = nowMs, leadingWidth = 0.dp)
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
                    GuideNowLine(
                        currentTimeFraction = currentTimeFraction,
                        currentTimeLabel = currentTimeLabel,
                        leadingWidth = 0.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniEpgProgramRow(
    programs: List<EpgProgram>,
    windowStartMs: Long,
    windowDurationMs: Long,
    rowHeight: Dp,
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
            GuideProgramCell(
                program = program,
                xOffset = timelineWidth * startFraction,
                cellWidth = timelineWidth * widthFraction,
                onProgramClick = onProgramClick,
                onProgramFocused = onProgramFocused,
            )
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
            .clip(CinemaShapes.Row)
            .background(CinemaColors.SurfaceSoft)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentLabel(text = stringResource(R.string.livetv_on_now))
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
                shape = CinemaShapes.XLarge,
                focusedBorderWidth = 0.dp,
                focusScale = 1.03f,
            ) { focused ->
                val contentColor = if (focused) CinemaColors.Background else CinemaColors.White
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (focused) CinemaColors.White else CinemaColors.Surface, CinemaShapes.XLarge)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = contentColor,
                                fontWeight = FontWeight.ExtraBold,
                            ),
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
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (focused) CinemaColors.Background.copy(alpha = 0.7f) else CinemaColors.TextMuted,
                            ),
                        )
                    }
                    Text(
                        text = channel.currentProgram,
                        style = MaterialTheme.typography.labelSmall.copy(color = contentColor.copy(alpha = 0.75f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .roundedProgressTrack(
                                fraction = channel.programProgress,
                                trackColor = if (focused) FocusedTrackColor else CinemaColors.Border,
                            ),
                    )
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
    val channelColumnWidth = 168.dp
    val rowHeight = 56.dp
    val timelineSlots = remember(windowStartMs, windowEndMs) {
        GuideLayoutHelper.timelineSlotStarts(windowStartMs, windowEndMs)
    }
    val dateLabel = remember(windowStartMs) { GuideLayoutHelper.formatDateLabel(windowStartMs) }
    val currentTimeLabel = remember(nowMs) { GuideLayoutHelper.formatCurrentTimeLabel(nowMs) }
    val windowDurationMs = GuideLayoutHelper.windowDurationMs(windowStartMs, windowEndMs)
    val currentTimeFraction = GuideLayoutHelper.currentTimeFraction(nowMs, windowStartMs, windowEndMs)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideHeader(dateLabel = dateLabel, large = true)
        Column(modifier = Modifier.fillMaxWidth()) {
            GuideTimeline(timelineSlots = timelineSlots, nowMs = nowMs, leadingWidth = channelColumnWidth)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            ) {
                val programsByChannel = remember(programs, windowStartMs, windowEndMs) {
                    GuideLayoutHelper.programsByChannel(programs, windowStartMs, windowEndMs)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(channels, key = { it.id }, contentType = { "guideRow" }) { channel ->
                        GuideChannelRow(
                            channel = channel,
                            isNowPlaying = channel.id == nowPlayingChannelId,
                            programs = programsByChannel[channel.id].orEmpty(),
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
                    GuideNowLine(
                        currentTimeFraction = currentTimeFraction,
                        currentTimeLabel = currentTimeLabel,
                        leadingWidth = channelColumnWidth,
                    )
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
    channelColumnWidth: Dp,
    rowHeight: Dp,
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
            modifier = Modifier
                .width(channelColumnWidth)
                .fillMaxHeight()
                .padding(end = 4.dp)
                .background(CinemaColors.SurfaceSoft, CinemaShapes.Card)
                .then(
                    if (isNowPlaying) {
                        Modifier.border(2.dp, CinemaColors.Accent, CinemaShapes.Card)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channel.channelNumber.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CinemaColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            ChannelLogoTile(
                logoUrl = channel.logoUrl,
                name = channel.name,
                size = 30.dp,
                shape = CinemaShapes.Large,
            )
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CinemaColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                ),
                maxLines = 1,
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
                GuideProgramCell(
                    program = program,
                    xOffset = timelineWidth * startFraction,
                    cellWidth = timelineWidth * widthFraction,
                    onProgramClick = onProgramClick,
                    onProgramFocused = onProgramFocused,
                )
            }
        }
    }
}
