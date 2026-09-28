package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
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
    // Light scrim so the previous channel's last frame stays visible while the next loads.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CinemaSpinner()
            Text(
                text = stringResource(R.string.player_buffering),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
            )
        }
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

data class PlayerLiveProgramDisplay(
    val title: String,
    val subtitle: String? = null,
    val progress: Float? = null,
)

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

/**
 * Shown for the last seconds of an episode. "Play now" has focus, so OK starts the next
 * episode right away; Cancel (or Back) keeps watching to the end.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AutoplayCountdownOverlay(
    secondsRemaining: Int,
    nextTitle: String,
    nextCode: String?,
    imageUrl: String?,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playNowFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playNowFocus.requestFocus() } }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Row(
            modifier = Modifier
                .padding(CinemaSpacing.ScreenPadding)
                .width(460.dp)
                .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CinemaAsyncImage(
                imageUrl = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(150.dp)
                    .aspectRatio(16f / 9f)
                    .clip(CinemaShapes.Small),
                contentScale = ContentScale.Crop,
                fallbackLabel = nextCode.orEmpty(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.player_up_next_in, secondsRemaining),
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
                )
                Text(
                    text = listOfNotNull(nextCode, nextTitle.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CinemaButton(
                        text = stringResource(R.string.player_play_now),
                        variant = CinemaButtonVariant.PrimaryAccent,
                        onClick = onPlayNow,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.focusRequester(playNowFocus),
                    )
                    CinemaButton(
                        text = stringResource(R.string.btn_cancel),
                        variant = CinemaButtonVariant.SecondaryDark,
                        onClick = onCancel,
                    )
                }
            }
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
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CinemaSpinner()
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
