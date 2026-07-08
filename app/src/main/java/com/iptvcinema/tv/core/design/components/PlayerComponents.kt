package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.player.TrackOption
import com.iptvcinema.tv.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerBufferingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.player_buffering),
            style = MaterialTheme.typography.titleLarge.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private val PlayerSidebarWidth = 380.dp

@Composable
fun PlayerScrimOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.72f),
                        0.22f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
            ),
    ) {
        content()
    }
}

@Composable
fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CinemaColors.White,
    mirrorIcon: Boolean = false,
) {
    FocusableCinemaCard(
        modifier = modifier.size(48.dp),
        onClick = onClick,
        shape = CircleShape,
        contentDescription = contentDescription,
        focusScale = 1.08f,
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Surface.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(scaleX = if (mirrorIcon) -1f else 1f),
            )
        }
    }
}

@Composable
fun PlayerCenterButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CinemaColors.White,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    iconSize: androidx.compose.ui.unit.Dp = 30.dp,
    mirrorIcon: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    FocusableCinemaCard(
        modifier = modifier
            .size(size)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        onClick = onClick,
        shape = CircleShape,
        contentDescription = contentDescription,
        focusScale = 1.1f,
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (focused) CinemaColors.Accent else CinemaColors.SurfaceGlass,
                    CircleShape,
                )
                .border(
                    width = 1.dp,
                    color = if (focused) CinemaColors.AccentSoft else CinemaColors.White.copy(alpha = 0.16f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (focused) CinemaColors.Background else tint,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer(scaleX = if (mirrorIcon) -1f else 1f),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerQualityPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .background(CinemaColors.Surface.copy(alpha = 0.55f), CinemaShapes.Large)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium.copy(
            color = CinemaColors.White,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

enum class PlayerTrackTab {
    Audio,
    Subtitles,
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerTopBar(
    title: String,
    subtitle: String,
    isLive: Boolean,
    qualityLabel: String?,
    resumeHint: String?,
    channelLogoUrl: String? = null,
    showEpisodesAction: Boolean,
    showChannelsAction: Boolean,
    onClose: () -> Unit,
    onEpisodes: () -> Unit,
    onChannels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CinemaSpacing.ScreenPadding, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.player_close),
                onClick = onClose,
            )
            if (isLive && !channelLogoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CinemaShapes.Small)
                        .background(CinemaColors.Surface.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CinemaAsyncImage(
                        imageUrl = channelLogoUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CinemaShapes.Small),
                        contentScale = ContentScale.Fit,
                        fallbackLabel = title,
                    )
                }
            }
            if (isLive) {
                BadgeChip(
                    text = stringResource(R.string.badge_live),
                    backgroundColor = CinemaColors.LiveRed,
                )
            }
            qualityLabel?.let { label ->
                PlayerQualityPill(label = label)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp)
                .padding(horizontal = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = CinemaColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            resumeHint?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.Accent),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showEpisodesAction) {
                PlayerIconButton(
                    icon = Icons.Default.VideoLibrary,
                    contentDescription = stringResource(R.string.player_episodes),
                    onClick = onEpisodes,
                )
            }
            if (showChannelsAction) {
                PlayerIconButton(
                    icon = Icons.Default.LiveTv,
                    contentDescription = stringResource(R.string.player_channels),
                    onClick = onChannels,
                )
            }
        }
    }
}

data class PlayerLiveProgramDisplay(
    val title: String,
    val subtitle: String? = null,
    val progress: Float? = null,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerLiveProgramPanel(
    channelName: String,
    currentProgram: PlayerLiveProgramDisplay?,
    nextProgram: PlayerLiveProgramDisplay?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 460.dp)
            .background(CinemaColors.SurfaceGlass, CinemaShapes.Large)
            .border(1.dp, CinemaColors.White.copy(alpha = 0.12f), CinemaShapes.Large)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgeChip(
                text = stringResource(R.string.badge_live),
                backgroundColor = CinemaColors.LiveRed,
            )
            Text(
                text = channelName,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ProgramTextBlock(
            label = stringResource(R.string.player_on_now),
            program = currentProgram,
            fallback = stringResource(R.string.msg_no_program_info),
        )
        currentProgram?.progress?.takeIf { it > 0f }?.let { progress ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.SurfaceSoft),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(CinemaColors.LiveRed),
                )
            }
        }
        if (nextProgram != null) {
            ProgramTextBlock(
                label = stringResource(R.string.player_up_next),
                program = nextProgram,
                fallback = "",
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProgramTextBlock(
    label: String,
    program: PlayerLiveProgramDisplay?,
    fallback: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = CinemaColors.AccentSoft,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = program?.title?.takeIf { it.isNotBlank() } ?: fallback,
            style = MaterialTheme.typography.labelLarge.copy(
                color = CinemaColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        program?.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerThinProgressBar(
    progress: Float,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var previewProgress by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }

    LaunchedEffect(progress) {
        if (!isFocused) {
            previewProgress = progress.coerceIn(0f, 1f)
        }
    }

    val displayProgress = if (isFocused) previewProgress else progress.coerceIn(0f, 1f)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val barHeight = if (isFocused) 6.dp else 4.dp
    val thumbSize = if (isFocused) 14.dp else 0.dp
    val seekStepMs = maxOf(durationMs / 120L, 5_000L)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isFocused) 28.dp else 16.dp)
            .padding(horizontal = CinemaSpacing.ScreenPadding)
            .onFocusChanged { focused ->
                isFocused = focused.isFocused
                if (focused.isFocused) {
                    previewProgress = progress.coerceIn(0f, 1f)
                }
            }
            .onKeyEvent { event ->
                if (!isFocused || durationMs <= 0L) return@onKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onInteraction()
                        val currentMs = (previewProgress * durationMs).toLong()
                        val newMs = if (isRtl) {
                            (currentMs + seekStepMs).coerceAtMost(durationMs)
                        } else {
                            (currentMs - seekStepMs).coerceAtLeast(0L)
                        }
                        previewProgress = newMs.toFloat() / durationMs.toFloat()
                        onSeekTo(newMs)
                        true
                    }
                    Key.DirectionRight -> {
                        onInteraction()
                        val currentMs = (previewProgress * durationMs).toLong()
                        val newMs = if (isRtl) {
                            (currentMs - seekStepMs).coerceAtLeast(0L)
                        } else {
                            (currentMs + seekStepMs).coerceAtMost(durationMs)
                        }
                        previewProgress = newMs.toFloat() / durationMs.toFloat()
                        onSeekTo(newMs)
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .then(
                if (isFocused) {
                    Modifier.border(2.dp, CinemaColors.FocusBorder, CinemaShapes.Small)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = maxWidth
        val thumbProgress = if (isRtl) 1f - displayProgress else displayProgress
        val thumbOffset = trackWidth * thumbProgress - thumbSize / 2

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(CinemaShapes.Small)
                .background(CinemaColors.Surface.copy(alpha = 0.55f)),
        ) {
            Box(
                modifier = Modifier
                    .align(if (isRtl) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                    .height(barHeight)
                    .background(if (isFocused) CinemaColors.Accent else CinemaColors.Accent.copy(alpha = 0.9f)),
            )
        }

        if (thumbSize > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.coerceIn(0.dp, trackWidth - thumbSize))
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(CinemaColors.Accent)
                    .border(2.dp, CinemaColors.Background, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerCenterControls(
    isPlaying: Boolean,
    isLive: Boolean,
    isRtl: Boolean,
    onPlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    playPauseFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isLive) {
            PlayerCenterButton(
                icon = Icons.Default.Replay10,
                contentDescription = stringResource(R.string.player_btn_rewind_10),
                onClick = onRewind10,
                size = 58.dp,
                iconSize = 28.dp,
                mirrorIcon = isRtl,
            )
            Spacer(modifier = Modifier.width(20.dp))
        }
        PlayerCenterButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = stringResource(
                if (isPlaying) R.string.player_btn_pause else R.string.player_btn_play,
            ),
            onClick = onPlayPause,
            size = 76.dp,
            iconSize = 38.dp,
            focusRequester = playPauseFocusRequester,
        )
        if (!isLive) {
            Spacer(modifier = Modifier.width(20.dp))
            PlayerCenterButton(
                icon = Icons.Default.Forward10,
                contentDescription = stringResource(R.string.player_btn_forward_10),
                onClick = onForward10,
                size = 58.dp,
                iconSize = 28.dp,
                mirrorIcon = isRtl,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerBottomControls(
    elapsed: String,
    total: String,
    isPlaying: Boolean,
    isLive: Boolean,
    showNextAction: Boolean,
    nextActionAccent: Boolean,
    isRtl: Boolean,
    onPlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onNext: () -> Unit,
    onSubtitles: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    playPauseFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CinemaSpacing.ScreenPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLive) {
                BadgeChip(
                    text = stringResource(R.string.badge_live),
                    backgroundColor = CinemaColors.LiveRed,
                )
            } else if (total.isNotBlank()) {
                Text(
                    text = stringResource(R.string.player_time_format, elapsed, total),
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.White),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showNextAction) {
                PlayerIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = stringResource(
                        if (isLive) R.string.player_next_channel else R.string.player_next_episode,
                    ),
                    onClick = onNext,
                    tint = if (nextActionAccent) CinemaColors.Accent else CinemaColors.White,
                    mirrorIcon = isRtl,
                )
            }
            PlayerIconButton(
                icon = Icons.Default.Subtitles,
                contentDescription = stringResource(R.string.player_subtitles),
                onClick = onSubtitles,
            )
            PlayerIconButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.player_settings),
                onClick = onSettings,
            )
            PlayerIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.btn_back),
                onClick = onBack,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeekableProgressTimeline(
    progress: Float,
    elapsed: String,
    remaining: String,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var previewProgress by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }

    LaunchedEffect(progress) {
        if (!isFocused) {
            previewProgress = progress.coerceIn(0f, 1f)
        }
    }

    val displayProgress = if (isFocused) previewProgress else progress.coerceIn(0f, 1f)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val seekStepMs = maxOf(durationMs / 120L, 5_000L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CinemaSpacing.ScreenPadding)
            .onFocusChanged { focused ->
                isFocused = focused.isFocused
                if (focused.isFocused) {
                    previewProgress = progress.coerceIn(0f, 1f)
                }
            }
            .onKeyEvent { event ->
                if (!isFocused || durationMs <= 0L) return@onKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onInteraction()
                        val currentMs = (previewProgress * durationMs).toLong()
                        val newMs = if (isRtl) {
                            (currentMs + seekStepMs).coerceAtMost(durationMs)
                        } else {
                            (currentMs - seekStepMs).coerceAtLeast(0L)
                        }
                        previewProgress = newMs.toFloat() / durationMs.toFloat()
                        onSeekTo(newMs)
                        true
                    }
                    Key.DirectionRight -> {
                        onInteraction()
                        val currentMs = (previewProgress * durationMs).toLong()
                        val newMs = if (isRtl) {
                            (currentMs - seekStepMs).coerceAtLeast(0L)
                        } else {
                            (currentMs + seekStepMs).coerceAtMost(durationMs)
                        }
                        previewProgress = newMs.toFloat() / durationMs.toFloat()
                        onSeekTo(newMs)
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .then(
                if (isFocused) {
                    Modifier.border(2.dp, CinemaColors.FocusBorder, CinemaShapes.Small)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 8.dp else 6.dp)
                .clip(CinemaShapes.Small)
                .background(CinemaColors.Surface.copy(alpha = 0.55f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                    .height(if (isFocused) 8.dp else 6.dp)
                    .background(if (isFocused) CinemaColors.Accent else CinemaColors.Accent.copy(alpha = 0.9f)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = elapsed,
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
            )
            Text(
                text = remaining,
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerUpNextRail(
    items: List<PosterCardData>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CinemaSpacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.player_up_next),
            style = MaterialTheme.typography.labelMedium.copy(
                color = CinemaColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
        ) {
            items(items, key = { it.contentId ?: it.title }) { item ->
                item.contentId?.let { contentId ->
                    FocusableCinemaCard(
                        modifier = Modifier
                            .width(120.dp)
                            .height(68.dp),
                        onClick = { onItemClick(contentId) },
                        shape = CinemaShapes.Small,
                        defaultBorderWidth = 0.dp,
                    ) { _ ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            CinemaAsyncImage(
                                imageUrl = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                fallbackLabel = item.title,
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                        ),
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = item.runtime ?: item.title,
                                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.White),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
fun PlayerTrackSidebar(
    audioTracks: List<TrackOption>,
    subtitleTracks: List<TrackOption>,
    selectedAudioIndex: Int,
    selectedSubtitleIndex: Int,
    onSelectAudio: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onDismiss: () -> Unit,
    selectedTab: PlayerTrackTab = PlayerTrackTab.Subtitles,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember(selectedTab) { mutableStateOf(selectedTab) }
    PlayerSidePanel(
        title = stringResource(R.string.player_track_settings),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            CategoryChip(
                label = stringResource(R.string.player_audio_tracks),
                isSelected = activeTab == PlayerTrackTab.Audio,
                onClick = { activeTab = PlayerTrackTab.Audio },
            )
            CategoryChip(
                label = stringResource(R.string.player_subtitles),
                isSelected = activeTab == PlayerTrackTab.Subtitles,
                onClick = { activeTab = PlayerTrackTab.Subtitles },
            )
        }
        when (activeTab) {
            PlayerTrackTab.Audio -> {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(audioTracks, key = { "audio-${it.index}" }) { track ->
                        CinemaButton(
                            text = track.label,
                            variant = if (track.index == selectedAudioIndex) {
                                CinemaButtonVariant.PrimaryAccent
                            } else {
                                CinemaButtonVariant.SecondaryDark
                            },
                            onClick = { onSelectAudio(track.index) },
                        )
                    }
                }
            }
            PlayerTrackTab.Subtitles -> {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        CinemaButton(
                            text = stringResource(R.string.toggle_off),
                            variant = if (selectedSubtitleIndex < 0) {
                                CinemaButtonVariant.PrimaryAccent
                            } else {
                                CinemaButtonVariant.SecondaryDark
                            },
                            onClick = onDisableSubtitles,
                        )
                    }
                    items(subtitleTracks, key = { "sub-${it.index}" }) { track ->
                        CinemaButton(
                            text = track.label,
                            variant = if (track.index == selectedSubtitleIndex) {
                                CinemaButtonVariant.PrimaryAccent
                            } else {
                                CinemaButtonVariant.SecondaryDark
                            },
                            onClick = { onSelectSubtitle(track.index) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerSidePanel(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val panelShape = if (isRtl) {
        RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isRtl) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .width(PlayerSidebarWidth)
                .fillMaxSize()
                .background(CinemaColors.SurfaceGlass, panelShape)
                .padding(CinemaSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                PlayerIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.player_close),
                    onClick = onDismiss,
                )
            }
            content()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOverlay(
    title: String,
    subtitle: String,
    progress: Float,
    elapsed: String,
    total: String,
    remaining: String = "",
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onBack: () -> Unit,
    isLive: Boolean = false,
    onRewind10: () -> Unit = {},
    onForward10: () -> Unit = {},
    onNext: () -> Unit = {},
    onEpisodes: () -> Unit = {},
    onChannels: () -> Unit = {},
    onSubtitles: () -> Unit = {},
    onSettings: () -> Unit = {},
    durationMs: Long = 0L,
    onSeekTo: (Long) -> Unit = {},
    onSeekInteraction: () -> Unit = {},
    qualityLabel: String? = null,
    resumeHint: String? = null,
    channelLogoUrl: String? = null,
    showEpisodesAction: Boolean = false,
    showChannelsAction: Boolean = false,
    showNextAction: Boolean = false,
    nextActionAccent: Boolean = false,
    upNextItems: List<PosterCardData> = emptyList(),
    onUpNextClick: (String) -> Unit = {},
    currentLiveProgram: PlayerLiveProgramDisplay? = null,
    nextLiveProgram: PlayerLiveProgramDisplay? = null,
    modifier: Modifier = Modifier,
    playPauseFocusRequester: FocusRequester? = null,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    PlayerScrimOverlay(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            PlayerTopBar(
                title = title,
                subtitle = subtitle,
                isLive = isLive,
                qualityLabel = qualityLabel,
                resumeHint = resumeHint,
                channelLogoUrl = channelLogoUrl,
                showEpisodesAction = showEpisodesAction,
                showChannelsAction = showChannelsAction,
                onClose = onBack,
                onEpisodes = onEpisodes,
                onChannels = onChannels,
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLive) {
                    PlayerLiveProgramPanel(
                        channelName = title,
                        currentProgram = currentLiveProgram,
                        nextProgram = nextLiveProgram,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = CinemaSpacing.ScreenPadding),
                    )
                }
                PlayerCenterControls(
                    isPlaying = isPlaying,
                    isLive = isLive,
                    isRtl = isRtl,
                    onPlayPause = onPlayPause,
                    onRewind10 = onRewind10,
                    onForward10 = onForward10,
                    playPauseFocusRequester = playPauseFocusRequester,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isLive && upNextItems.isNotEmpty()) {
                    PlayerUpNextRail(
                        items = upNextItems,
                        onItemClick = onUpNextClick,
                    )
                }
                if (!isLive && durationMs > 0L) {
                    SeekableProgressTimeline(
                        progress = progress,
                        elapsed = elapsed,
                        remaining = remaining.ifBlank { "-$total" },
                        durationMs = durationMs,
                        onSeekTo = onSeekTo,
                        onInteraction = onSeekInteraction,
                    )
                }
                PlayerBottomControls(
                    elapsed = elapsed,
                    total = total,
                    isPlaying = isPlaying,
                    isLive = isLive,
                    showNextAction = showNextAction,
                    nextActionAccent = nextActionAccent,
                    isRtl = isRtl,
                    onPlayPause = onPlayPause,
                    onRewind10 = onRewind10,
                    onForward10 = onForward10,
                    onNext = onNext,
                    onSubtitles = onSubtitles,
                    onSettings = onSettings,
                    onBack = onBack,
                    playPauseFocusRequester = playPauseFocusRequester,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgressTimeline(
    progress: Float,
    elapsed: String,
    remaining: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CinemaShapes.Small)
                .background(CinemaColors.Surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(CinemaColors.Accent),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = elapsed, style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted))
            Text(text = remaining, style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AutoplayCountdownOverlay(
    secondsRemaining: Int,
    nextTitle: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            modifier = Modifier
                .padding(CinemaSpacing.ScreenPadding)
                .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.player_up_next_in, secondsRemaining),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = nextTitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            CinemaButton(
                text = stringResource(R.string.btn_cancel),
                variant = CinemaButtonVariant.SecondaryDark,
                onClick = onCancel,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelChangeBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = CinemaSpacing.ScreenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = message,
            modifier = Modifier
                .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                color = CinemaColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerRebufferOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(CinemaSpacing.ScreenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = stringResource(R.string.player_buffering),
            modifier = Modifier
                .background(CinemaColors.Background.copy(alpha = 0.75f), CinemaShapes.Small)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerEpisodeSidebar(
    seriesTitle: String,
    seriesPosterUrl: String?,
    currentEpisodeSubtitle: String?,
    seasons: List<SeasonItem>,
    currentEpisodeId: String?,
    isLoading: Boolean,
    onEpisodeClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSeason by remember(seasons) {
        mutableStateOf(
            seasons.find { season ->
                season.episodes.any { it.id == currentEpisodeId }
            }?.seasonNumber ?: seasons.firstOrNull()?.seasonNumber ?: 1,
        )
    }
    val episodes = seasons.find { it.seasonNumber == selectedSeason }?.episodes.orEmpty()
    val listState = rememberLazyListState()
    val currentEpisodeFocus = remember { FocusRequester() }
    val currentEpisodeIndex = episodes.indexOfFirst { it.id == currentEpisodeId }

    LaunchedEffect(seasons, currentEpisodeId, selectedSeason, episodes.size) {
        if (currentEpisodeId == null || currentEpisodeIndex < 0) return@LaunchedEffect
        listState.scrollToItem(currentEpisodeIndex)
        currentEpisodeFocus.requestFocus()
    }

    PlayerSidePanel(
        title = stringResource(R.string.player_episodes),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = seriesTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                currentEpisodeSubtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextSecondary),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 108.dp)
                    .clip(CinemaShapes.Small),
            ) {
                CinemaAsyncImage(
                    imageUrl = seriesPosterUrl,
                    contentDescription = seriesTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = seriesTitle,
                )
            }
        }
        if (isLoading) {
            Text(
                text = stringResource(R.string.player_loading_episodes),
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextMuted),
            )
        } else if (seasons.isEmpty()) {
            Text(
                text = stringResource(R.string.player_no_episodes),
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextMuted),
            )
        } else {
            SeasonSelector(
                seasons = seasons.map { it.seasonNumber },
                selectedSeason = selectedSeason,
                onSeasonSelected = { selectedSeason = it },
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(episodes, key = { it.id }) { episode ->
                    val isPlaying = episode.id == currentEpisodeId
                    PlayerEpisodeSidebarRow(
                        episodeNumber = episode.episodeNumber,
                        title = episode.title,
                        durationMinutes = episode.durationMinutes,
                        thumbnailUrl = episode.thumbnailUrl,
                        fallbackImageUrl = seriesPosterUrl,
                        isPlaying = isPlaying,
                        onClick = { onEpisodeClick(episode.id) },
                        modifier = if (isPlaying) {
                            Modifier.focusRequester(currentEpisodeFocus)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerChannelSidebar(
    channels: List<ChannelTileData>,
    currentChannelId: String?,
    isLoading: Boolean,
    onChannelClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerSidePanel(
        title = stringResource(R.string.player_channels),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        if (isLoading) {
            Text(
                text = stringResource(R.string.player_loading_channels),
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextMuted),
            )
        } else if (channels.isEmpty()) {
            Text(
                text = stringResource(R.string.player_no_channels),
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextMuted),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(channels, key = { it.id ?: it.channelName }) { channel ->
                    channel.id?.let { channelId ->
                        val isCurrent = channelId == currentChannelId
                        ChannelTile(
                            data = channel,
                            onClick = { onChannelClick(channelId) },
                            modifier = if (isCurrent) {
                                Modifier.border(1.dp, CinemaColors.Accent, CinemaShapes.Medium)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchKeyboard(
    layout: SearchKeyboardLayout,
    onLayoutToggle: () -> Unit,
    onDeviceKeyboard: () -> Unit,
    showDeviceKeyboard: Boolean,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    firstKeyFocusRequester: FocusRequester? = null,
) {
    val rows = SearchKeyboardLayouts.rowsFor(layout)
    val keyWidth = 34.dp
    val keyHeight = 44.dp
    val keyGap = 12.dp
    val layoutLabel = when (layout) {
        SearchKeyboardLayout.English -> "EN"
        SearchKeyboardLayout.Arabic -> "ع"
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(keyGap)) {
                row.forEachIndexed { keyIndex, key ->
                    FocusableCinemaCard(
                        modifier = Modifier
                            .size(width = keyWidth, height = keyHeight)
                            .then(
                                if (rowIndex == 0 && keyIndex == 0 && firstKeyFocusRequester != null) {
                                    Modifier.focusRequester(firstKeyFocusRequester)
                                } else {
                                    Modifier
                                },
                            ),
                        onClick = {
                            val output = if (layout == SearchKeyboardLayout.English) {
                                key.lowercase()
                            } else {
                                key
                            }
                            onKeyPress(output)
                        },
                        shape = CinemaShapes.Pill,
                        focusScale = 1.02f,
                    ) { focused ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (focused) CinemaColors.White else CinemaColors.Background,
                                    CinemaShapes.Pill,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = if (focused) CinemaColors.Background else CinemaColors.White,
                                    fontWeight = FontWeight.Normal,
                                ),
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CinemaButton(
                text = layoutLabel,
                variant = CinemaButtonVariant.PrimaryAccent,
                onClick = onLayoutToggle,
            )
            if (showDeviceKeyboard) {
                CinemaButton(
                    text = stringResource(R.string.search_device_keyboard),
                    variant = CinemaButtonVariant.SecondaryDark,
                    onClick = onDeviceKeyboard,
                )
            }
            CinemaButton(text = stringResource(R.string.btn_space), variant = CinemaButtonVariant.SecondaryDark, onClick = { onKeyPress(" ") })
            CinemaButton(text = stringResource(R.string.btn_clear), variant = CinemaButtonVariant.Ghost, onClick = onClear)
            CinemaButton(text = stringResource(R.string.btn_backspace), variant = CinemaButtonVariant.Ghost, onClick = onBackspace)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RecentSearchChip(
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CategoryChip(label = query, isSelected = false, onClick = onClick, modifier = modifier)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    CinemaTextField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.search_hint),
        modifier = modifier,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearch() },
        ),
    )
}
