package com.iptvcinema.tv.features.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.AutoplayCountdownOverlay
import com.iptvcinema.tv.core.design.components.ChannelChangeBanner
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.components.PlayerBufferingOverlay
import com.iptvcinema.tv.core.design.components.PlayerChannelSidebar
import com.iptvcinema.tv.core.design.components.PlayerEpisodeSidebar
import com.iptvcinema.tv.core.design.components.PlayerLiveProgramDisplay
import com.iptvcinema.tv.core.design.components.PlayerOverlay
import com.iptvcinema.tv.core.design.components.PlayerRebufferOverlay
import com.iptvcinema.tv.core.design.components.PlayerTrackSidebar
import com.iptvcinema.tv.core.design.components.PlayerTrackTab
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.player.PlayerCommand
import kotlinx.coroutines.delay

private const val OVERLAY_HIDE_DELAY_MS = 5_000L

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
fun PlayerScreen(
    contentId: String,
    contentType: String,
    seriesId: String? = null,
    resumePositionMs: Long? = null,
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val screenState by viewModel.screenState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val focusState = rememberScreenFocusState("player")
    val playPauseFocus = remember { FocusRequester() }
    val videoSurfaceFocus = remember { FocusRequester() }
    var trackPickerOpen by remember { mutableStateOf(false) }
    var trackPickerTab by remember { mutableStateOf(PlayerTrackTab.Subtitles) }
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

    fun toggleOverlay() {
        if (isOverlayVisible) {
            dismissOverlay()
        } else {
            revealOverlay()
        }
    }

    fun handlePlayerKeyAction(action: PlayerKeyAction) {
        when (action) {
            PlayerKeyAction.PlayPause,
            PlayerKeyAction.Play,
            PlayerKeyAction.Pause,
            is PlayerKeyAction.SeekRelative,
            PlayerKeyAction.ChannelPrevious,
            PlayerKeyAction.ChannelNext,
            PlayerKeyAction.EpisodePrevious,
            PlayerKeyAction.EpisodeNext,
            -> {
                PlayerKeyHandler.toCommand(action)?.let { viewModel.onCommand(it) }
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
            trackPickerOpen -> trackPickerOpen = false
            isOverlayVisible -> navController.popBackStack()
            else -> revealOverlay()
        }
    }

    LaunchedEffect(
        isOverlayVisible,
        overlayActivityToken,
        trackPickerOpen,
        pickerOpen,
        screenState.isLoading,
    ) {
        if (
            !screenState.isLoading &&
            !trackPickerOpen &&
            !pickerOpen &&
            isOverlayVisible &&
            !screenState.showAutoplayCountdown
        ) {
            delay(OVERLAY_HIDE_DELAY_MS)
            dismissOverlay()
        }
    }

    LaunchedEffect(isOverlayVisible, trackPickerOpen, pickerOpen) {
        if (trackPickerOpen || pickerOpen || !isOverlayVisible) {
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
                switchStreamLabel = stringResource(R.string.btn_back_to_guide),
                backLabel = stringResource(R.string.btn_back),
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
        !trackPickerOpen &&
        !pickerOpen &&
        !screenState.showAutoplayCountdown
    val resumeHint = screenState.resumeFromMs.takeIf { it > 0L }?.let {
        stringResource(R.string.player_resume_from, formatTimeMs(it))
    }
    val nextEpisodeTitle = screenState.nextEpisodeTitle
        ?: screenState.upNextItems.firstOrNull()?.title
        ?: stringResource(R.string.player_next_episode)
    val currentEpisodeId = screenState.playbackRequest?.contentId
    val currentChannelId = screenState.playbackRequest?.contentId
    val playbackRequest = screenState.playbackRequest
    val overlayTitle = when {
        screenState.isEpisode && !screenState.seriesTitle.isNullOrBlank() -> screenState.seriesTitle.orEmpty()
        else -> playerState.title
    }
    val overlaySubtitle = when {
        screenState.isEpisode && playbackRequest?.seasonNumber != null && playbackRequest.episodeNumber != null -> {
            stringResource(
                R.string.player_season_episode,
                playbackRequest.seasonNumber,
                playbackRequest.episodeNumber,
            )
        }
        playerState.metadata.isNotEmpty() -> playerState.metadata.joinToString(" · ")
        else -> ""
    }
    val totalTime = if (playerState.isLive || durationMs == null || durationMs <= 0L) {
        ""
    } else {
        formatTimeMs(durationMs)
    }
    val remainingTime = if (playerState.isLive || durationMs == null || durationMs <= 0L) {
        ""
    } else {
        "-${formatTimeMs((durationMs - playerState.positionMs).coerceAtLeast(0L))}"
    }
    val channelLogoUrl = if (playerState.isLive) playbackRequest?.posterUrl else null
    val layoutDirection = LocalLayoutDirection.current
    val sidebarSlideOffset = if (layoutDirection == LayoutDirection.Rtl) {
        { fullWidth: Int -> -fullWidth }
    } else {
        { fullWidth: Int -> fullWidth }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
                .onPreviewKeyEvent { event ->
                    if (trackPickerOpen || pickerOpen) return@onPreviewKeyEvent false
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
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (!trackPickerOpen && !pickerOpen) {
                                toggleOverlay()
                            }
                        }
                    },
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

            if (!controlsVisible && !trackPickerOpen && !pickerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(videoSurfaceFocus)
                        .focusable(),
                )
            }

            if (playerState.isBuffering && !playerState.hasFirstFrame) {
                PlayerBufferingOverlay()
            } else if (playerState.isBuffering && playerState.hasFirstFrame || playerState.isReconnecting) {
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
                PlayerOverlay(
                    title = overlayTitle,
                    subtitle = overlaySubtitle,
                    progress = progress,
                    elapsed = formatTimeMs(playerState.positionMs),
                    total = totalTime,
                    remaining = remainingTime,
                    isPlaying = playerState.isPlaying,
                    isLive = playerState.isLive,
                    durationMs = durationMs ?: 0L,
                    qualityLabel = playerState.qualityLabel,
                    resumeHint = resumeHint,
                    channelLogoUrl = channelLogoUrl,
                    showEpisodesAction = screenState.isEpisode,
                    showChannelsAction = playerState.isLive,
                    showNextAction = screenState.isEpisode || playerState.isLive,
                    nextActionAccent = screenState.isEpisode,
                    upNextItems = if (screenState.isEpisode) screenState.upNextItems else emptyList(),
                    onUpNextClick = { episodeId -> viewModel.playUpNextEpisode(episodeId) },
                    currentLiveProgram = screenState.currentLiveProgram?.let { program ->
                        PlayerLiveProgramDisplay(
                            title = program.title,
                            subtitle = program.subtitle,
                            progress = program.progress,
                        )
                    },
                    nextLiveProgram = screenState.nextLiveProgram?.let { program ->
                        PlayerLiveProgramDisplay(
                            title = program.title,
                            subtitle = program.subtitle,
                            progress = program.progress,
                        )
                    },
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
                    onNext = {
                        revealOverlay()
                        if (playerState.isLive) {
                            viewModel.onCommand(PlayerCommand.ChannelNext)
                        } else {
                            viewModel.skipToNextEpisode()
                        }
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
                        trackPickerTab = PlayerTrackTab.Subtitles
                        trackPickerOpen = true
                    },
                    onSettings = {
                        revealOverlay()
                        trackPickerTab = PlayerTrackTab.Audio
                        trackPickerOpen = true
                    },
                    onBack = { navController.popBackStack() },
                    playPauseFocusRequester = playPauseFocus,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (screenState.showAutoplayCountdown) {
                AutoplayCountdownOverlay(
                    secondsRemaining = screenState.autoplayCountdownSeconds,
                    nextTitle = nextEpisodeTitle,
                    onCancel = { viewModel.cancelAutoplayCountdown() },
                )
            }

            AnimatedVisibility(
                visible = screenState.episodePickerOpen,
                enter = slideInHorizontally(initialOffsetX = sidebarSlideOffset),
                exit = slideOutHorizontally(targetOffsetX = sidebarSlideOffset),
            ) {
                PlayerEpisodeSidebar(
                    seriesTitle = screenState.seriesTitle ?: playerState.title,
                    seriesPosterUrl = screenState.seriesPosterUrl ?: playbackRequest?.posterUrl,
                    currentEpisodeSubtitle = overlaySubtitle.takeIf { it.isNotBlank() },
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
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedVisibility(
                visible = screenState.channelPickerOpen,
                enter = slideInHorizontally(initialOffsetX = sidebarSlideOffset),
                exit = slideOutHorizontally(targetOffsetX = sidebarSlideOffset),
            ) {
                PlayerChannelSidebar(
                    channels = screenState.channelPickerChannels,
                    currentChannelId = currentChannelId,
                    isLoading = screenState.channelPickerLoading,
                    onChannelClick = { channelId ->
                        viewModel.switchToChannel(channelId)
                        revealOverlay()
                    },
                    onDismiss = {
                        viewModel.dismissChannelPicker()
                        revealOverlay()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedVisibility(
                visible = trackPickerOpen,
                enter = slideInHorizontally(initialOffsetX = sidebarSlideOffset),
                exit = slideOutHorizontally(targetOffsetX = sidebarSlideOffset),
            ) {
                PlayerTrackSidebar(
                    audioTracks = playerState.audioTracks,
                    subtitleTracks = playerState.subtitleTracks,
                    selectedAudioIndex = playerState.selectedAudioIndex,
                    selectedSubtitleIndex = playerState.selectedSubtitleIndex,
                    selectedTab = trackPickerTab,
                    onSelectAudio = { index ->
                        viewModel.onCommand(PlayerCommand.SelectAudioTrack(index))
                    },
                    onDisableSubtitles = {
                        viewModel.onCommand(PlayerCommand.DisableSubtitles)
                    },
                    onSelectSubtitle = { index ->
                        viewModel.onCommand(PlayerCommand.SelectSubtitleTrack(index))
                    },
                    onDismiss = {
                        trackPickerOpen = false
                        revealOverlay()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
