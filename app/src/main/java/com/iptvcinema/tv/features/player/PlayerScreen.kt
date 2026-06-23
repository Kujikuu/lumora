package com.iptvcinema.tv.features.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.AutoplayCountdownOverlay
import com.iptvcinema.tv.core.design.components.ChannelChangeBanner
import com.iptvcinema.tv.core.design.components.ChannelPickerOverlay
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.components.EpisodePickerOverlay
import com.iptvcinema.tv.core.design.components.PlayerBufferingOverlay
import com.iptvcinema.tv.core.design.components.PlayerOverlay
import com.iptvcinema.tv.core.design.components.PlayerRebufferOverlay
import com.iptvcinema.tv.core.design.components.RemoteHintBar
import com.iptvcinema.tv.core.design.components.TrackPickerOverlay
import com.iptvcinema.tv.core.design.components.playerRemoteHints
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.player.PlayerCommand
import kotlinx.coroutines.delay

private const val OVERLAY_HIDE_DELAY_MS = 5_000L

@Composable
fun PlayerScreen(
    contentId: String,
    contentType: String,
    seriesId: String? = null,
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val screenState by viewModel.screenState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val focusState = rememberScreenFocusState("player")
    val playPauseFocus = remember { FocusRequester() }
    val videoSurfaceFocus = remember { FocusRequester() }
    var trackPickerMode by remember { mutableStateOf<TrackPickerMode?>(null) }
    var isOverlayVisible by remember { mutableStateOf(true) }
    var overlayActivityToken by remember { mutableIntStateOf(0) }

    val pickerOpen = screenState.episodePickerOpen || screenState.channelPickerOpen

    fun registerOverlayActivity() {
        overlayActivityToken++
    }

    fun revealOverlay() {
        isOverlayVisible = true
        registerOverlayActivity()
    }

    fun dismissOverlay() {
        isOverlayVisible = false
    }

    fun handlePlayerKeyAction(action: PlayerKeyAction) {
        when (action) {
            PlayerKeyAction.PlayPause,
            PlayerKeyAction.Play,
            PlayerKeyAction.Pause,
            is PlayerKeyAction.SeekRelative,
            PlayerKeyAction.ChannelPrevious,
            PlayerKeyAction.ChannelNext,
            -> {
                PlayerKeyHandler.toCommand(action)?.let { viewModel.onCommand(it) }
                revealOverlay()
            }
            PlayerKeyAction.EpisodePrevious -> {
                viewModel.skipToPreviousEpisode()
                revealOverlay()
            }
            PlayerKeyAction.EpisodeNext -> {
                viewModel.skipToNextEpisode()
                revealOverlay()
            }
            PlayerKeyAction.RevealOverlay -> revealOverlay()
        }
    }

    BackHandler {
        when {
            screenState.showAutoplayCountdown -> viewModel.cancelAutoplayCountdown()
            screenState.episodePickerOpen -> viewModel.dismissEpisodePicker()
            screenState.channelPickerOpen -> viewModel.dismissChannelPicker()
            trackPickerMode != null -> trackPickerMode = null
            isOverlayVisible -> navController.popBackStack()
            else -> revealOverlay()
        }
    }

    LaunchedEffect(
        isOverlayVisible,
        overlayActivityToken,
        trackPickerMode,
        pickerOpen,
        screenState.isLoading,
    ) {
        if (
            !screenState.isLoading &&
            trackPickerMode == null &&
            !pickerOpen &&
            isOverlayVisible &&
            !screenState.showAutoplayCountdown
        ) {
            delay(OVERLAY_HIDE_DELAY_MS)
            dismissOverlay()
        }
    }

    LaunchedEffect(isOverlayVisible, trackPickerMode, pickerOpen) {
        if (trackPickerMode != null || pickerOpen || !isOverlayVisible) {
            videoSurfaceFocus.requestFocus()
        } else {
            playPauseFocus.requestFocus()
        }
    }

    LaunchedEffect(focusState.hasSavedFocus, screenState.isLoading) {
        if (screenState.isLoading) return@LaunchedEffect
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(playPauseFocus)
        } else {
            focusState.requestInitialFocus(playPauseFocus)
            focusState.saveFocusIndex(0)
        }
        revealOverlay()
    }

    val loadError = screenState.loadError ?: playerState.errorMessage
    val loadErrorCode = screenState.loadErrorCode ?: playerState.errorCode

    if (loadError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            ErrorState(
                title = stringResource(R.string.error_title),
                description = loadError,
                errorCode = loadErrorCode,
                onRetry = { viewModel.retry() },
                onSwitchStream = {
                    when (contentType.lowercase()) {
                        "live" -> navController.navigate(AppRoute.liveTv())
                        "episode" -> {
                            val targetSeriesId = screenState.seriesId ?: seriesId
                            if (targetSeriesId != null) {
                                navController.navigate(AppRoute.seriesDetails(targetSeriesId))
                            } else {
                                navController.navigate(AppRoute.SERIES)
                            }
                        }
                        else -> navController.navigate(AppRoute.MOVIES)
                    }
                },
                onBack = { navController.popBackStack() },
                showSwitchStream = contentType.lowercase() == "live",
                switchStreamLabel = "Back to Guide",
                backLabel = "Back",
            )
        }
        return
    }

    if (screenState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            PlayerBufferingOverlay()
        }
        return
    }

    val durationMs = playerState.durationMs
    val progress = if (playerState.isLive || durationMs == null || durationMs <= 0L) {
        0f
    } else {
        (playerState.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    val controlsVisible = isOverlayVisible &&
        trackPickerMode == null &&
        !pickerOpen &&
        !screenState.showAutoplayCountdown
    val resumeHint = screenState.resumeFromMs.takeIf { it > 0L }?.let {
        "Resuming from ${formatTimeMs(it)}"
    }
    val nextEpisodeTitle = screenState.nextEpisodeTitle
        ?: screenState.upNextItems.firstOrNull()?.title
        ?: "Next episode"
    val currentEpisodeId = screenState.playbackRequest?.contentId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (trackPickerMode != null || pickerOpen) return@onPreviewKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                if (controlsVisible) {
                    registerOverlayActivity()
                }

                val action = PlayerKeyHandler.resolve(
                    key = event.key,
                    isLive = playerState.isLive,
                    isEpisode = screenState.isEpisode,
                    controlsVisible = controlsVisible,
                    pickerOpen = pickerOpen,
                ) ?: return@onPreviewKeyEvent false

                handlePlayerKeyAction(action)
                action != PlayerKeyAction.RevealOverlay || !controlsVisible
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { playerView ->
                playerView.player = viewModel.getExoPlayer()
            },
        )

        if (!controlsVisible && trackPickerMode == null && !pickerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(videoSurfaceFocus)
                    .focusable(),
            )
        }

        if (playerState.isBuffering && !playerState.hasFirstFrame) {
            PlayerBufferingOverlay()
        } else if (playerState.isBuffering && playerState.hasFirstFrame) {
            PlayerRebufferOverlay()
        }

        screenState.channelChangeBanner?.let { banner ->
            ChannelChangeBanner(message = banner)
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PlayerOverlay(
                    title = playerState.title,
                    metadata = playerState.metadata,
                    progress = progress,
                    elapsed = formatTimeMs(playerState.positionMs),
                    remaining = formatRemainingMs(playerState.positionMs, durationMs),
                    isPlaying = playerState.isPlaying,
                    isLive = playerState.isLive,
                    durationMs = durationMs ?: 0L,
                    qualityLabel = playerState.qualityLabel,
                    resumeHint = resumeHint,
                    showSkipNext = screenState.isEpisode,
                    onSeekTo = { positionMs ->
                        revealOverlay()
                        registerOverlayActivity()
                        viewModel.onCommand(PlayerCommand.SeekTo(positionMs))
                    },
                    onSeekInteraction = {
                        revealOverlay()
                        registerOverlayActivity()
                    },
                    onPlayPause = {
                        revealOverlay()
                        viewModel.onCommand(PlayerCommand.PlayPause)
                    },
                    onRewind = {
                        revealOverlay()
                        if (!playerState.isLive) {
                            viewModel.onCommand(PlayerCommand.SeekRelative(-30_000L))
                        }
                    },
                    onRewind10 = {
                        revealOverlay()
                        if (!playerState.isLive) {
                            viewModel.onCommand(PlayerCommand.SeekRelative(-10_000L))
                        }
                    },
                    onForward10 = {
                        revealOverlay()
                        if (!playerState.isLive) {
                            viewModel.onCommand(PlayerCommand.SeekRelative(10_000L))
                        }
                    },
                    onSkipPrevious = {
                        revealOverlay()
                        viewModel.skipToPreviousEpisode()
                    },
                    onSkipNext = {
                        revealOverlay()
                        viewModel.skipToNextEpisode()
                    },
                    onEpisodes = {
                        revealOverlay()
                        viewModel.openEpisodePicker()
                    },
                    onChannels = {
                        revealOverlay()
                        viewModel.openChannelPicker()
                    },
                    onSubtitles = {
                        revealOverlay()
                        trackPickerMode = TrackPickerMode.Subtitles
                    },
                    onAudio = {
                        revealOverlay()
                        trackPickerMode = TrackPickerMode.Audio
                    },
                    onBack = { navController.popBackStack() },
                    upNextItems = screenState.upNextItems,
                    onUpNextClick = { item ->
                        item.contentId?.let { episodeId ->
                            viewModel.playUpNextEpisode(episodeId)
                            revealOverlay()
                        }
                    },
                    playPauseFocusRequester = playPauseFocus,
                    modifier = Modifier.weight(1f),
                )
                RemoteHintBar(
                    hints = playerRemoteHints(
                        isLive = playerState.isLive,
                        isEpisode = screenState.isEpisode,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CinemaSpacing.ScreenPadding, vertical = 8.dp),
                )
            }
        }

        if (screenState.showAutoplayCountdown) {
            AutoplayCountdownOverlay(
                secondsRemaining = screenState.autoplayCountdownSeconds,
                nextTitle = nextEpisodeTitle,
                onCancel = { viewModel.cancelAutoplayCountdown() },
            )
        }

        if (screenState.episodePickerOpen) {
            EpisodePickerOverlay(
                seasons = screenState.episodePickerSeasons,
                currentEpisodeId = currentEpisodeId,
                isLoading = screenState.episodePickerLoading,
                onEpisodeClick = { episodeId ->
                    viewModel.playEpisodeFromPicker(episodeId)
                    revealOverlay()
                },
                onDismiss = {
                    viewModel.dismissEpisodePicker()
                    revealOverlay()
                },
            )
        }

        if (screenState.channelPickerOpen) {
            ChannelPickerOverlay(
                channels = screenState.channelPickerChannels,
                isLoading = screenState.channelPickerLoading,
                onChannelClick = { channelId ->
                    viewModel.switchToChannel(channelId)
                    revealOverlay()
                },
                onDismiss = {
                    viewModel.dismissChannelPicker()
                    revealOverlay()
                },
            )
        }

        trackPickerMode?.let { mode ->
            val tracks = when (mode) {
                TrackPickerMode.Audio -> playerState.audioTracks
                TrackPickerMode.Subtitles -> playerState.subtitleTracks
            }
            TrackPickerOverlay(
                title = if (mode == TrackPickerMode.Audio) "Audio Tracks" else "Subtitles",
                tracks = tracks,
                selectedIndex = when (mode) {
                    TrackPickerMode.Audio -> playerState.selectedAudioIndex
                    TrackPickerMode.Subtitles -> playerState.selectedSubtitleIndex
                },
                showOffOption = mode == TrackPickerMode.Subtitles,
                onSelect = { index ->
                    when (mode) {
                        TrackPickerMode.Audio -> viewModel.onCommand(PlayerCommand.SelectAudioTrack(index))
                        TrackPickerMode.Subtitles -> viewModel.onCommand(PlayerCommand.SelectSubtitleTrack(index))
                    }
                    trackPickerMode = null
                    revealOverlay()
                },
                onDisable = {
                    viewModel.onCommand(PlayerCommand.DisableSubtitles)
                    trackPickerMode = null
                    revealOverlay()
                },
                onDismiss = {
                    trackPickerMode = null
                    revealOverlay()
                },
            )
        }
    }
}

private enum class TrackPickerMode {
    Audio,
    Subtitles,
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun formatRemainingMs(positionMs: Long, durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0L) return ""
    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
    return "-${formatTimeMs(remainingMs)}"
}
