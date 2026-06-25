package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TrackPickerOverlay(
    title: String,
    tracks: List<TrackOption>,
    selectedIndex: Int,
    showOffOption: Boolean,
    onSelect: (Int) -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background.copy(alpha = 0.92f))
            .padding(CinemaSpacing.ScreenPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showOffOption) {
                    item {
                        CinemaButton(
                            text = stringResource(R.string.toggle_off),
                            variant = if (selectedIndex < 0) CinemaButtonVariant.PrimaryAccent else CinemaButtonVariant.SecondaryDark,
                            onClick = onDisable,
                        )
                    }
                }
                items(tracks, key = { it.index }) { track ->
                    CinemaButton(
                        text = track.label,
                        variant = if (track.index == selectedIndex) {
                            CinemaButtonVariant.PrimaryAccent
                        } else {
                            CinemaButtonVariant.SecondaryDark
                        },
                        onClick = { onSelect(track.index) },
                    )
                }
            }
            CinemaButton(text = stringResource(R.string.btn_close), variant = CinemaButtonVariant.Ghost, onClick = onDismiss)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOverlay(
    title: String,
    metadata: List<String>,
    progress: Float,
    elapsed: String,
    remaining: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onBack: () -> Unit,
    isLive: Boolean = false,
    onRewind: () -> Unit = {},
    onRewind10: () -> Unit = {},
    onForward10: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onEpisodes: () -> Unit = {},
    onChannels: () -> Unit = {},
    onSubtitles: () -> Unit = {},
    onAudio: () -> Unit = {},
    upNextItems: List<PosterCardData> = emptyList(),
    onUpNextClick: (PosterCardData) -> Unit = {},
    durationMs: Long = 0L,
    onSeekTo: (Long) -> Unit = {},
    onSeekInteraction: () -> Unit = {},
    qualityLabel: String? = null,
    resumeHint: String? = null,
    showSkipNext: Boolean = false,
    modifier: Modifier = Modifier,
    playPauseFocusRequester: FocusRequester? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background.copy(alpha = 0.85f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CinemaSpacing.ScreenPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CinemaLogo(compact = true)
                Text(
                    text = stringResource(R.string.player_now_playing),
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = metadata.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
                )
                resumeHint?.let { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.Accent),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLive) {
                    PlaybackControlButton(icon = Icons.Default.FastRewind, onClick = onRewind)
                    PlaybackControlButton(icon = Icons.Default.Replay10, onClick = onRewind10)
                }
                if (showSkipNext) {
                    PlaybackControlButton(icon = Icons.Default.SkipPrevious, onClick = onSkipPrevious)
                }
                PlaybackControlButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = onPlayPause,
                    large = true,
                    modifier = playPauseFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
                if (!isLive) {
                    PlaybackControlButton(icon = Icons.Default.Forward10, onClick = onForward10)
                    if (showSkipNext) {
                        PlaybackControlButton(icon = Icons.Default.SkipNext, onClick = onSkipNext)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isLive && durationMs > 0L) {
                    SeekableProgressBar(
                        progress = progress,
                        durationMs = durationMs,
                        elapsed = elapsed,
                        remaining = remaining,
                        onSeekTo = onSeekTo,
                        onInteraction = onSeekInteraction,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
                ) {
                    if (showSkipNext) {
                        CinemaButton(text = stringResource(R.string.player_episodes), variant = CinemaButtonVariant.Ghost, onClick = onEpisodes)
                    }
                    if (isLive) {
                        CinemaButton(text = stringResource(R.string.player_channels), variant = CinemaButtonVariant.Ghost, onClick = onChannels)
                    }
                    CinemaButton(text = stringResource(R.string.player_subtitles), variant = CinemaButtonVariant.Ghost, onClick = onSubtitles)
                    CinemaButton(text = stringResource(R.string.player_audio), variant = CinemaButtonVariant.Ghost, onClick = onAudio)
                    qualityLabel?.let { label ->
                        BadgeChip(text = label, backgroundColor = CinemaColors.SurfaceSoft)
                    }
                    CinemaButton(text = stringResource(R.string.btn_back), variant = CinemaButtonVariant.SecondaryDark, onClick = onBack)
                }
                if (upNextItems.isNotEmpty()) {
                    ContentRail(title = stringResource(R.string.rail_up_next), items = upNextItems) { item ->
                        PosterCard(data = item, variant = PosterCardVariant.CompactPoster, onClick = { onUpNextClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    large: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val size = if (large) 72.dp else 48.dp
    FocusableCinemaCard(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .size(size),
        onClick = onClick,
        shape = CinemaShapes.Large,
    ) { _ ->
        FocusableCardSurface(
            backgroundColor = if (large) CinemaColors.White else CinemaColors.SurfaceSoft.copy(alpha = 0.6f),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (large) CinemaColors.Background else CinemaColors.White,
                modifier = Modifier.size(if (large) 36.dp else 24.dp),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeekableProgressBar(
    progress: Float,
    durationMs: Long,
    elapsed: String,
    remaining: String,
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
    val barHeight = if (isFocused) 8.dp else 6.dp
    val thumbSize = if (isFocused) 18.dp else 12.dp
    val seekStepMs = maxOf(durationMs / 120L, 5_000L)

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
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
                            val newMs = (currentMs - seekStepMs).coerceAtLeast(0L)
                            previewProgress = newMs.toFloat() / durationMs.toFloat()
                            onSeekTo(newMs)
                            true
                        }
                        Key.DirectionRight -> {
                            onInteraction()
                            val currentMs = (previewProgress * durationMs).toLong()
                            val newMs = (currentMs + seekStepMs).coerceAtMost(durationMs)
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
            val thumbOffset = trackWidth * displayProgress - thumbSize / 2

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.Surface),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                        .height(barHeight)
                        .background(if (isFocused) CinemaColors.Accent else CinemaColors.Accent.copy(alpha = 0.85f)),
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.coerceIn(0.dp, trackWidth - thumbSize))
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(if (isFocused) CinemaColors.Accent else CinemaColors.White)
                    .then(
                        if (isFocused) {
                            Modifier.border(2.dp, CinemaColors.Background, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
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
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isFocused) CinemaColors.White else CinemaColors.TextMuted,
                ),
            )
            Text(
                text = if (isFocused) stringResource(R.string.player_seek_hint) else remaining,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isFocused) CinemaColors.TextSecondary else CinemaColors.TextMuted,
                ),
            )
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
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background.copy(alpha = 0.7f)),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            modifier = Modifier
                .padding(CinemaSpacing.ScreenPadding)
                .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                .padding(CinemaSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
        ) {
            Text(
                text = stringResource(R.string.player_up_next_in, secondsRemaining),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = nextTitle,
                style = MaterialTheme.typography.bodyLarge,
            )
            CinemaButton(text = stringResource(R.string.btn_cancel), variant = CinemaButtonVariant.SecondaryDark, onClick = onCancel)
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
fun EpisodePickerOverlay(
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background.copy(alpha = 0.92f))
            .padding(CinemaSpacing.ScreenPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
        ) {
            Text(
                text = stringResource(R.string.player_episodes),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
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
                        EpisodeCard(
                            episodeNumber = episode.episodeNumber,
                            title = episode.title,
                            durationMinutes = episode.durationMinutes,
                            progress = episode.progress,
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
            CinemaButton(text = stringResource(R.string.btn_close), variant = CinemaButtonVariant.Ghost, onClick = onDismiss)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelPickerOverlay(
    channels: List<ChannelTileData>,
    isLoading: Boolean,
    onChannelClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background.copy(alpha = 0.92f))
            .padding(CinemaSpacing.ScreenPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            Text(
                text = stringResource(R.string.player_channels),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
                ) {
                    items(channels, key = { it.id ?: it.channelName }) { channel ->
                        channel.id?.let { channelId ->
                            ChannelTile(
                                data = channel,
                                onClick = { onChannelClick(channelId) },
                            )
                        }
                    }
                }
            }
            CinemaButton(text = stringResource(R.string.btn_close), variant = CinemaButtonVariant.Ghost, onClick = onDismiss)
        }
    }
}

@Composable
fun playerRemoteHints(isLive: Boolean = false, isEpisode: Boolean = false): List<RemoteHint> {
    val hints = mutableListOf(
        RemoteHint(R.string.hint_player_playpause_key, R.string.hint_player_playpause_desc),
        RemoteHint(R.string.hint_player_seek_key, R.string.hint_player_seek_desc),
    )
    if (isLive) {
        hints.add(RemoteHint(R.string.hint_player_channel_key, R.string.hint_player_channel_desc))
    }
    if (isEpisode) {
        hints.add(RemoteHint(R.string.hint_player_episode_key, R.string.hint_player_episode_desc))
    }
    hints.add(RemoteHint(R.string.hint_back_key, R.string.hint_back_desc))
    return hints
}

@Composable
fun playerRemoteHints(): List<RemoteHint> = playerRemoteHints(isLive = false, isEpisode = false)

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
) {
    val rows = SearchKeyboardLayouts.rowsFor(layout)
    val layoutLabel = when (layout) {
        SearchKeyboardLayout.English -> "EN"
        SearchKeyboardLayout.Arabic -> "ع"
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    FocusableCinemaCard(
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            val output = if (layout == SearchKeyboardLayout.English) {
                                key.lowercase()
                            } else {
                                key
                            }
                            onKeyPress(output)
                        },
                        shape = CinemaShapes.Small,
                    ) { _ ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CinemaColors.SurfaceSoft, CinemaShapes.Small),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = key, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
