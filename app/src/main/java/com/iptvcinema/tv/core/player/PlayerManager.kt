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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptvcinema.tv.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private var playbackGeneration: Int = 0
    private var pendingErrorMessage: String? = null
    private var pendingErrorCode: String? = null

    fun getExoPlayer(): ExoPlayer = ensurePlayer()

    fun play(request: PlaybackRequest, startPositionMs: Long = 0L, isXtreamSource: Boolean = false) {
        retryJob?.cancel()
        retryAttempt = 0
        pendingErrorMessage = null
        pendingErrorCode = null
        playbackGeneration++
        playInternal(request, startPositionMs, isXtreamSource, playbackGeneration)
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
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        val mediaItem = MediaItem.Builder()
            .setUri(request.streamUrl)
            .build()
        exoPlayer.setMediaSource(
            DefaultMediaSourceFactory(buildDataSourceFactory(request.headers))
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

    fun release() {
        playbackGeneration++
        retryJob?.cancel()
        retryJob = null
        player?.removeListener(playerListener)
        player?.release()
        player = null
        trackedAudioGroupIndex = -1
        trackedSubtitleGroupIndex = -1
        pendingErrorMessage = null
        pendingErrorCode = null
        _state.value = PlayerUiState()
    }

    fun clearPlaybackEnded() {
        _state.update { it.copy(playbackEnded = false) }
    }

    @OptIn(UnstableApi::class)
    private fun buildDataSourceFactory(headers: PlaybackHeaders): DefaultDataSource.Factory {
        val requestProperties = headers.customHeaders.toMutableMap()
        headers.referer?.let { requestProperties["Referer"] = it }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(60_000)
            .setAllowCrossProtocolRedirects(true)
        headers.userAgent?.let { httpFactory.setUserAgent(it) }
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
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 90_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 8_000,
            )
            .build()
        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(playerListener)
                player = exoPlayer
            }
    }

    private fun selectAudioTrack(index: Int) {
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
        _state.update { it.copy(selectedAudioIndex = index) }
    }

    private fun selectSubtitleTrack(index: Int) {
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
        _state.update { it.copy(selectedSubtitleIndex = index) }
    }

    private fun disableSubtitles() {
        val exoPlayer = player ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        trackedSubtitleGroupIndex = -1
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
            val (message, code) = PlayerErrorMapper.mapPlaybackError(error, isXtreamSource)
            val willRetry = error.isTransientNetworkError() && retryAttempt < MAX_RETRY_ATTEMPTS
            if (willRetry) {
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
                scheduleRetryIfTransient(error)
            } else {
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

    private fun scheduleRetryIfTransient(error: PlaybackException) {
        val request = lastRequest ?: return
        if (!error.isTransientNetworkError()) return
        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            showPendingError()
            return
        }
        retryJob?.cancel()
        retryAttempt += 1
        val delayMs = RETRY_BASE_DELAY_MS * (1L shl (retryAttempt - 1))
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

    private fun PlaybackException.isTransientNetworkError(): Boolean =
        errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
            errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_BASE_DELAY_MS = 1_000L
    }
}
