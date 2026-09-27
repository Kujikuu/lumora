package com.iptvcinema.tv.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptvcinema.tv.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var lastRequest: PlaybackRequest? = null
    private var lastStartPositionMs: Long = 0L
    private var isXtreamSource: Boolean = false
    private var trackedAudioGroupIndex: Int = -1
    private var trackedSubtitleGroupIndex: Int = -1
    private var retryAttempt: Int = 0
    private var retryJob: Job? = null
    private var stallJob: Job? = null
    private var playbackGeneration: Int = 0
    private var sessionToken: Int = 0
    private var pendingErrorMessage: String? = null
    private var pendingErrorCode: String? = null
    private var playbackPreferences: PlaybackPreferences? = null
    private var userOverrodeAudio = false
    private var userOverrodeSubtitles = false

    // Dedicated client: no API interceptors (they would override provider user agents),
    // pooled connections so channel zaps reuse sockets to the same provider.
    private val playbackHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(PlaybackTuning.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(PlaybackTuning.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun getExoPlayer(): ExoPlayer = ensurePlayer()

    fun applyPlaybackPreferences(preferences: PlaybackPreferences) {
        playbackPreferences = preferences
        player?.let { exoPlayer ->
            applyQualityCap(exoPlayer, preferences.streamingQuality)
            applyDefaultTracksIfNeeded(exoPlayer)
        }
    }

    /**
     * Starts a stream and returns a token identifying this playback session. Pass the token
     * to [stop] so a screen only stops playback it started, never another screen's stream.
     */
    fun play(request: PlaybackRequest, startPositionMs: Long = 0L, isXtreamSource: Boolean = false): Int {
        sessionToken++
        retryJob?.cancel()
        retryAttempt = 0
        pendingErrorMessage = null
        pendingErrorCode = null
        playbackGeneration++
        userOverrodeAudio = false
        userOverrodeSubtitles = false
        playInternal(request, startPositionMs, isXtreamSource, playbackGeneration)
        return sessionToken
    }

    @OptIn(UnstableApi::class)
    private fun playInternal(
        request: PlaybackRequest,
        startPositionMs: Long = 0L,
        isXtreamSource: Boolean = false,
        generation: Int = playbackGeneration,
    ) {
        if (generation != playbackGeneration) return
        lastRequest = request
        lastStartPositionMs = startPositionMs
        this.isXtreamSource = isXtreamSource
        val exoPlayer = ensurePlayer()
        if (generation != playbackGeneration) return
        _state.update {
            it.copy(
                title = request.title,
                metadata = request.metadata,
                isLive = request.isLive,
                isBuffering = true,
                isPlaying = true,
                hasFirstFrame = false,
                isReconnecting = false,
                playbackEnded = false,
                positionMs = startPositionMs,
                durationMs = request.durationMs,
                errorMessage = null,
                errorCode = null,
                qualityLabel = null,
            )
        }
        stallJob?.cancel()
        val mediaItem = MediaItem.Builder()
            .setUri(request.streamUrl)
            .build()
        // setMediaSource replaces the old stream in place. Skipping stop() keeps the last
        // frame on screen until the new one renders, so zaps do not flash black.
        exoPlayer.setMediaSource(
            DefaultMediaSourceFactory(buildDataSourceFactory(request.headers))
                .setLoadErrorHandlingPolicy(LiveAwareLoadErrorPolicy())
                .createMediaSource(mediaItem),
        )
        exoPlayer.prepare()
        if (startPositionMs > 0L && !request.isLive) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    fun handleCommand(command: PlayerCommand) {
        when (command) {
            PlayerCommand.PlayPause -> {
                val exoPlayer = player ?: return
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }
            PlayerCommand.Play -> player?.play()
            PlayerCommand.Pause -> player?.pause()
            is PlayerCommand.SeekTo -> player?.seekTo(command.positionMs.coerceAtLeast(0L))
            is PlayerCommand.SeekRelative -> {
                val exoPlayer = player ?: return
                val duration = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                val target = (exoPlayer.currentPosition + command.deltaMs).coerceIn(0L, duration)
                exoPlayer.seekTo(target)
            }
            is PlayerCommand.SelectAudioTrack -> selectAudioTrack(command.index)
            is PlayerCommand.SelectSubtitleTrack -> selectSubtitleTrack(command.index)
            PlayerCommand.DisableSubtitles -> disableSubtitles()
            PlayerCommand.Retry -> retry()
            PlayerCommand.ChannelPrevious,
            PlayerCommand.ChannelNext,
            PlayerCommand.EpisodePrevious,
            PlayerCommand.EpisodeNext,
                -> Unit
        }
    }

    fun retry() {
        val request = lastRequest ?: return
        retryJob?.cancel()
        retryAttempt = 0
        pendingErrorMessage = null
        pendingErrorCode = null
        playbackGeneration++
        playInternal(request, lastStartPositionMs, isXtreamSource, playbackGeneration)
    }

    /**
     * Stops the stream started with [token] but keeps the player alive, so the next screen
     * can reuse it. Does nothing if another screen has started playback since.
     */
    fun stop(token: Int) {
        if (token != sessionToken) return
        playbackGeneration++
        retryJob?.cancel()
        retryJob = null
        stallJob?.cancel()
        stallJob = null
        player?.stop()
        player?.clearMediaItems()
        pendingErrorMessage = null
        pendingErrorCode = null
        _state.value = PlayerUiState()
    }

    fun release() {
        playbackGeneration++
        retryJob?.cancel()
        retryJob = null
        stallJob?.cancel()
        stallJob = null
        player?.removeListener(playerListener)
        player?.release()
        player = null
        trackedAudioGroupIndex = -1
        trackedSubtitleGroupIndex = -1
        pendingErrorMessage = null
        pendingErrorCode = null
        _state.value = PlayerUiState()
    }

    /** Updates the on-screen metadata of the current stream without restarting it. */
    fun updateMetadata(metadata: List<String>) {
        _state.update { it.copy(metadata = metadata) }
    }

    fun clearPlaybackEnded() {
        _state.update { it.copy(playbackEnded = false) }
    }

    @OptIn(UnstableApi::class)
    private fun buildDataSourceFactory(headers: PlaybackHeaders): DefaultDataSource.Factory {
        val requestProperties = headers.customHeaders.toMutableMap()
        headers.referer?.let { requestProperties["Referer"] = it }
        // Fall back to the platform agent, which is what the previous HttpURLConnection
        // stack sent. Some providers reject OkHttp's default agent.
        val userAgent = headers.userAgent ?: System.getProperty("http.agent")
        val httpFactory = OkHttpDataSource.Factory(playbackHttpClient)
        userAgent?.let { httpFactory.setUserAgent(it) }
        if (requestProperties.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(requestProperties)
        }
        return DefaultDataSource.Factory(context, httpFactory)
    }

    @OptIn(UnstableApi::class)
    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                PlaybackTuning.MIN_BUFFER_MS,
                PlaybackTuning.MAX_BUFFER_MS,
                PlaybackTuning.BUFFER_FOR_PLAYBACK_MS,
                PlaybackTuning.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            // Size wins over time, so a high-bitrate stream stops buffering at the byte cap.
            .setTargetBufferBytes(PlaybackTuning.targetBufferBytes(Runtime.getRuntime().maxMemory()))
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(playerListener)
                playbackPreferences?.let { applyQualityCap(exoPlayer, it.streamingQuality) }
                player = exoPlayer
            }
    }

    @OptIn(UnstableApi::class)
    private fun applyQualityCap(exoPlayer: ExoPlayer, quality: String) {
        val maxHeight = PlaybackPreferencesApplier.maxVideoHeight(quality) ?: Int.MAX_VALUE
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            .build()
    }

    private fun selectAudioTrack(index: Int, fromUser: Boolean = true) {
        val exoPlayer = player ?: return
        val track = _state.value.audioTracks.getOrNull(index) ?: return
        val groups = exoPlayer.currentTracks.groups
        val group = groups.getOrNull(track.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            .build()
        trackedAudioGroupIndex = track.groupIndex
        if (fromUser) userOverrodeAudio = true
        _state.update { it.copy(selectedAudioIndex = index) }
    }

    private fun selectSubtitleTrack(index: Int, fromUser: Boolean = true) {
        val exoPlayer = player ?: return
        val track = _state.value.subtitleTracks.getOrNull(index) ?: return
        val groups = exoPlayer.currentTracks.groups
        val group = groups.getOrNull(track.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            .build()
        trackedSubtitleGroupIndex = track.groupIndex
        if (fromUser) userOverrodeSubtitles = true
        _state.update { it.copy(selectedSubtitleIndex = index) }
    }

    private fun disableSubtitles(fromUser: Boolean = true) {
        val exoPlayer = player ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        trackedSubtitleGroupIndex = -1
        if (fromUser) userOverrodeSubtitles = true
        _state.update { it.copy(selectedSubtitleIndex = -1) }
    }

    private fun updateTrackOptions(exoPlayer: ExoPlayer) {
        val audioTracks = mutableListOf<TrackOption>()
        val subtitleTracks = mutableListOf<TrackOption>()
        exoPlayer.currentTracks.groups.forEachIndexed { groupIndex, group ->
            if (!group.isSupported) return@forEachIndexed
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val label = format.label?.takeIf { it.isNotBlank() }
                            ?: format.language?.takeIf { it.isNotBlank() }
                            ?: "Audio ${audioTracks.size + 1}"
                        audioTracks += TrackOption(
                            index = audioTracks.size,
                            label = label,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            language = format.language?.takeIf { it.isNotBlank() },
                        )
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val label = format.label?.takeIf { it.isNotBlank() }
                            ?: format.language?.takeIf { it.isNotBlank() }
                            ?: "Subtitle ${subtitleTracks.size + 1}"
                        subtitleTracks += TrackOption(
                            index = subtitleTracks.size,
                            label = label,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            language = format.language?.takeIf { it.isNotBlank() },
                        )
                    }
                }
            }
        }
        _state.update {
            it.copy(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                qualityLabel = extractQualityLabel(exoPlayer),
            )
        }
        applyDefaultTracksIfNeeded(exoPlayer)
    }

    private fun applyDefaultTracksIfNeeded(exoPlayer: ExoPlayer) {
        val prefs = playbackPreferences ?: return
        val audioTracks = _state.value.audioTracks
        val subtitleTracks = _state.value.subtitleTracks
        if (!userOverrodeAudio && audioTracks.isNotEmpty()) {
            val mediaTracks = audioTracks.map { MediaTrackOption(it.index, it.label, it.language) }
            PlaybackPreferencesApplier.selectAudioTrackIndex(mediaTracks, prefs.defaultAudioLanguage)
                ?.takeIf { it != _state.value.selectedAudioIndex }
                ?.let { selectAudioTrack(it, fromUser = false) }
        }
        if (!userOverrodeSubtitles) {
            if (!prefs.subtitlesEnabled) {
                if (_state.value.selectedSubtitleIndex != -1) {
                    disableSubtitles(fromUser = false)
                }
            } else {
                val mediaTracks = subtitleTracks.map { MediaTrackOption(it.index, it.label, it.language) }
                PlaybackPreferencesApplier.selectSubtitleTrackIndex(
                    tracks = mediaTracks,
                    preferredLanguage = prefs.defaultSubtitleLanguage,
                    subtitlesEnabled = true,
                )?.takeIf { it != _state.value.selectedSubtitleIndex }
                    ?.let { selectSubtitleTrack(it, fromUser = false) }
            }
        }
    }

    private fun extractQualityLabel(exoPlayer: ExoPlayer): String? {
        exoPlayer.currentTracks.groups.forEach { group ->
            if (group.type != C.TRACK_TYPE_VIDEO) return@forEach
            for (trackIndex in 0 until group.length) {
                if (group.isTrackSelected(trackIndex)) {
                    val height = group.getTrackFormat(trackIndex).height
                    if (height > 0) return formatQuality(height)
                }
            }
        }
        return null
    }

    private fun formatQuality(height: Int): String = when {
        height >= 2160 -> "4K"
        height >= 1080 -> "1080p"
        height >= 720 -> "720p"
        height >= 480 -> "480p"
        else -> "${height}p"
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING ||
                        (it.isReconnecting && playbackState == Player.STATE_IDLE),
                    hasFirstFrame = it.hasFirstFrame || playbackState == Player.STATE_READY,
                    playbackEnded = playbackState == Player.STATE_ENDED,
                    isPlaying = if (playbackState == Player.STATE_ENDED) false else it.isPlaying,
                )
            }
            if (playbackState == Player.STATE_BUFFERING) {
                armStallWatchdog()
            } else {
                stallJob?.cancel()
            }
            if (playbackState == Player.STATE_READY) {
                retryAttempt = 0
                player?.let { exoPlayer ->
                    updateTrackOptions(exoPlayer)
                    val duration = exoPlayer.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET }
                    _state.update { state ->
                        state.copy(
                            durationMs = if (state.isLive) null else duration ?: state.durationMs,
                            isReconnecting = false,
                        )
                    }
                }
            }
        }

        override fun onRenderedFirstFrame() {
            _state.update { it.copy(hasFirstFrame = true, isBuffering = false, isReconnecting = false) }
        }

        override fun onPlayerError(error: PlaybackException) {
            stallJob?.cancel()
            val (message, code) = PlayerErrorMapper.mapPlaybackError(error, isXtreamSource)
            val action = PlaybackRecoveryPolicy.forError(
                errorCode = error.errorCode,
                isLive = lastRequest?.isLive == true,
                attempt = retryAttempt,
            )
            when (action) {
                PlaybackRecoveryAction.SEEK_TO_LIVE_EDGE -> {
                    retryAttempt += 1
                    markReconnecting(message, code)
                    player?.let { exoPlayer ->
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                    }
                }
                PlaybackRecoveryAction.RETRY -> {
                    markReconnecting(message, code)
                    scheduleRetry()
                }
                PlaybackRecoveryAction.FAIL -> showError(message, code)
            }
        }

        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            player?.let { updateTrackOptions(it) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            player?.let { exoPlayer ->
                _state.update {
                    it.copy(
                        positionMs = exoPlayer.currentPosition,
                        durationMs = if (it.isLive) null else exoPlayer.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET },
                    )
                }
            }
        }
    }

    fun tickPosition() {
        val exoPlayer = player ?: return
        _state.update {
            it.copy(
                positionMs = exoPlayer.currentPosition,
                durationMs = if (it.isLive) null else exoPlayer.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET },
            )
        }
    }

    private fun markReconnecting(message: String, code: String) {
        pendingErrorMessage = message
        pendingErrorCode = code
        _state.update {
            it.copy(
                errorMessage = null,
                errorCode = null,
                isReconnecting = true,
                isBuffering = true,
                isPlaying = false,
            )
        }
    }

    private fun showError(message: String?, code: String?) {
        stallJob?.cancel()
        pendingErrorMessage = null
        pendingErrorCode = null
        _state.update {
            it.copy(
                errorMessage = message,
                errorCode = code,
                isReconnecting = false,
                isBuffering = false,
                isPlaying = false,
            )
        }
    }

    // A stream can hang in BUFFERING without ever raising an error (a live playlist that
    // stops updating, a socket that never times out). Re-open it instead of spinning forever.
    private fun armStallWatchdog() {
        stallJob?.cancel()
        val exoPlayer = player ?: return
        val generation = playbackGeneration
        val positionAtStart = exoPlayer.currentPosition
        stallJob = applicationScope.launch(Dispatchers.Main.immediate) {
            delay(PlaybackTuning.STALL_TIMEOUT_MS)
            val current = player ?: return@launch
            if (generation != playbackGeneration) return@launch
            val stalled = PlaybackRecoveryPolicy.isStalled(
                stillBuffering = current.playbackState == Player.STATE_BUFFERING,
                positionAtStartMs = positionAtStart,
                currentPositionMs = current.currentPosition,
            )
            if (stalled) recoverFromStall()
        }
    }

    private fun recoverFromStall() {
        val (message, code) = PlayerErrorMapper.stalledStreamError()
        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            player?.stop()
            showError(message, code)
            return
        }
        markReconnecting(message, code)
        scheduleRetry(immediate = true)
    }

    private fun scheduleRetry(immediate: Boolean = false) {
        val request = lastRequest ?: return
        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            showPendingError()
            return
        }
        retryJob?.cancel()
        retryAttempt += 1
        val delayMs = if (immediate) 0L else PlaybackRecoveryPolicy.retryDelayMs(retryAttempt)
        val retryStartPositionMs = player?.currentPosition
            ?.takeIf { it > 0L && !request.isLive }
            ?: lastStartPositionMs
        val generationAtSchedule = playbackGeneration
        retryJob = applicationScope.launch {
            delay(delayMs)
            withContext(Dispatchers.Main.immediate) {
                if (generationAtSchedule != playbackGeneration || player == null) return@withContext
                if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
                    showPendingError()
                    return@withContext
                }
                playInternal(request, retryStartPositionMs, isXtreamSource, generationAtSchedule)
            }
        }
    }

    private fun showPendingError() {
        _state.update {
            it.copy(
                errorMessage = pendingErrorMessage,
                errorCode = pendingErrorCode,
                isReconnecting = false,
                isBuffering = false,
                isPlaying = false,
            )
        }
        pendingErrorMessage = null
        pendingErrorCode = null
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = PlaybackTuning.MAX_RETRY_ATTEMPTS
    }
}
