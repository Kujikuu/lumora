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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var lastRequest: PlaybackRequest? = null
    private var lastStartPositionMs: Long = 0L
    private var isXtreamSource: Boolean = false
    private var trackedAudioGroupIndex: Int = -1
    private var trackedSubtitleGroupIndex: Int = -1

    fun getExoPlayer(): ExoPlayer = ensurePlayer()

    fun play(request: PlaybackRequest, startPositionMs: Long = 0L, isXtreamSource: Boolean = false) {
        lastRequest = request
        lastStartPositionMs = startPositionMs
        this.isXtreamSource = isXtreamSource
        val exoPlayer = ensurePlayer()
        _state.update {
            it.copy(
                title = request.title,
                metadata = request.metadata,
                isLive = request.isLive,
                isBuffering = true,
                isPlaying = true,
                hasFirstFrame = false,
                positionMs = startPositionMs,
                durationMs = request.durationMs,
                errorMessage = null,
                errorCode = null,
                qualityLabel = null,
            )
        }
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
            PlayerCommand.ChannelPrevious, PlayerCommand.ChannelNext -> Unit
        }
    }

    fun retry() {
        val request = lastRequest ?: return
        play(request, lastStartPositionMs)
    }

    fun release() {
        player?.removeListener(playerListener)
        player?.release()
        player = null
        trackedAudioGroupIndex = -1
        trackedSubtitleGroupIndex = -1
        _state.value = PlayerUiState()
    }

    @OptIn(UnstableApi::class)
    private fun buildDataSourceFactory(headers: PlaybackHeaders): DefaultDataSource.Factory {
        val requestProperties = headers.customHeaders.toMutableMap()
        headers.referer?.let { requestProperties["Referer"] = it }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
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
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000,
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
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    hasFirstFrame = it.hasFirstFrame || playbackState == Player.STATE_READY,
                )
            }
            if (playbackState == Player.STATE_READY) {
                player?.let { exoPlayer ->
                    updateTrackOptions(exoPlayer)
                    val duration = exoPlayer.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET }
                    _state.update { state ->
                        state.copy(
                            durationMs = if (state.isLive) null else duration ?: state.durationMs,
                        )
                    }
                }
            }
        }

        override fun onRenderedFirstFrame() {
            _state.update { it.copy(hasFirstFrame = true, isBuffering = false) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val (message, code) = PlayerErrorMapper.mapPlaybackError(error, isXtreamSource)
            _state.update {
                it.copy(
                    errorMessage = message,
                    errorCode = code,
                    isBuffering = false,
                    isPlaying = false,
                )
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
}
