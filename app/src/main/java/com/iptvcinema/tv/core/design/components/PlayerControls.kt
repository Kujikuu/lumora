package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.util.isolateDirection
import kotlinx.coroutines.delay

/** Pure scrubbing and time rules for the player overlay, unit tested. */
internal object PlayerScrubLogic {
    /** Seek is committed this long after the last scrub press, so holding a key seeks once. */
    const val COMMIT_DELAY_MS = 600L
    private const val MIN_STEP_MS = 10_000L

    /** Step grows the longer the user keeps scrubbing, so long films are quick to cross. */
    fun stepMs(durationMs: Long, consecutivePresses: Int): Long {
        val base = maxOf(durationMs / 100L, MIN_STEP_MS)
        val multiplier = when {
            consecutivePresses < 4 -> 1
            consecutivePresses < 10 -> 3
            else -> 6
        }
        return base * multiplier
    }

    fun target(currentMs: Long, deltaMs: Long, durationMs: Long): Long =
        (currentMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))

    fun format(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        return if (hours > 0) "$hours:$mm:$ss" else "$minutes:$ss"
    }
}

/**
 * YouTube-style player overlay: title info at the top, and every control in one bottom
 * area (timeline or live programme info, then a single row of buttons). One focus row
 * means no bouncing between top, centre and bottom zones. Back closes the player, so
 * there is no close button.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOverlay(
    title: String,
    subtitle: String,
    isLive: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    qualityLabel: String? = null,
    resumeHint: String? = null,
    channelLogoUrl: String? = null,
    currentLiveProgram: PlayerLiveProgramDisplay? = null,
    nextLiveProgram: PlayerLiveProgramDisplay? = null,
    nextEpisodeCode: String? = null,
    showNextEpisode: Boolean = false,
    showEpisodes: Boolean = false,
    showChannels: Boolean = false,
    onNextEpisode: () -> Unit = {},
    onEpisodes: () -> Unit = {},
    onChannels: () -> Unit = {},
    onTracks: () -> Unit = {},
    playPauseFocusRequester: FocusRequester? = null,
) {
    PlayerScrimOverlay(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CinemaSpacing.ScreenPadding, vertical = 28.dp),
        ) {
            PlayerInfoHeader(
                title = title,
                subtitle = subtitle,
                isLive = isLive,
                qualityLabel = qualityLabel,
                resumeHint = resumeHint,
                channelLogoUrl = channelLogoUrl,
            )
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (isLive) {
                    PlayerLiveNowNext(current = currentLiveProgram, next = nextLiveProgram)
                } else if (durationMs > 0L) {
                    PlayerScrubber(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeekTo = onSeekTo,
                        onInteraction = onInteraction,
                    )
                }
                PlayerControlRow(
                    isLive = isLive,
                    isPlaying = isPlaying,
                    showNextEpisode = showNextEpisode,
                    nextEpisodeCode = nextEpisodeCode,
                    showEpisodes = showEpisodes,
                    showChannels = showChannels,
                    onPlayPause = onPlayPause,
                    onSeekRelative = onSeekRelative,
                    onNextEpisode = onNextEpisode,
                    onEpisodes = onEpisodes,
                    onChannels = onChannels,
                    onTracks = onTracks,
                    playPauseFocusRequester = playPauseFocusRequester,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerInfoHeader(
    title: String,
    subtitle: String,
    isLive: Boolean,
    qualityLabel: String?,
    resumeHint: String?,
    channelLogoUrl: String?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLive && !channelLogoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CinemaShapes.Medium)
                    .background(CinemaColors.Surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                CinemaAsyncImage(
                    imageUrl = channelLogoUrl,
                    contentDescription = title,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit,
                    fallbackLabel = title,
                )
            }
        }
        Column(
            modifier = Modifier.widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLive) {
                    BadgeChip(text = stringResource(R.string.badge_live), backgroundColor = CinemaColors.LiveRed)
                }
                qualityLabel?.let { PlayerQualityPill(label = it) }
            }
            Text(
                text = title.isolateDirection(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle.isolateDirection(),
                    style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            resumeHint?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.AccentSoft),
                )
            }
        }
    }
}

/**
 * Timeline. D-pad moves a preview marker instantly; the real seek happens once, after
 * the presses stop, instead of rebuffering on every press. The fill is drawn, not laid
 * out, so position ticks only redraw it.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var previewMs by remember { mutableStateOf<Long?>(null) }
    var consecutivePresses by remember { mutableIntStateOf(0) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val forwardKey = if (isRtl) Key.DirectionLeft else Key.DirectionRight
    val backKey = if (isRtl) Key.DirectionRight else Key.DirectionLeft

    LaunchedEffect(previewMs) {
        val target = previewMs ?: return@LaunchedEffect
        delay(PlayerScrubLogic.COMMIT_DELAY_MS)
        onSeekTo(target)
        consecutivePresses = 0
        previewMs = null
    }

    val shownMs = previewMs ?: positionMs
    val fraction = if (durationMs > 0L) (shownMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val scrubbing = previewMs != null
    val seekLabel = stringResource(R.string.player_seek_hint)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val direction = when (event.key) {
                    forwardKey -> 1
                    backKey -> -1
                    else -> return@onKeyEvent false
                }
                onInteraction()
                consecutivePresses++
                val step = PlayerScrubLogic.stepMs(durationMs, consecutivePresses)
                previewMs = PlayerScrubLogic.target(previewMs ?: positionMs, direction * step, durationMs)
                true
            }
            .focusable()
            .semantics { contentDescription = seekLabel },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .drawBehind {
                    val trackHeight = if (focused) 8.dp.toPx() else 4.dp.toPx()
                    val top = (size.height - trackHeight) / 2f
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    val radius = CornerRadius(trackHeight / 2f)
                    drawRoundRect(
                        color = CinemaColors.White.copy(alpha = 0.25f),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, trackHeight),
                        cornerRadius = radius,
                    )
                    val fillWidth = size.width * fraction
                    drawRoundRect(
                        color = CinemaColors.Accent,
                        topLeft = Offset(if (rtl) size.width - fillWidth else 0f, top),
                        size = Size(fillWidth, trackHeight),
                        cornerRadius = radius,
                    )
                    if (focused) {
                        val thumbX = if (rtl) size.width - fillWidth else fillWidth
                        drawCircle(
                            color = CinemaColors.White,
                            radius = size.height / 2f,
                            center = Offset(thumbX, size.height / 2f),
                        )
                    }
                },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimeLabel(
                text = PlayerScrubLogic.format(shownMs),
                color = if (scrubbing) CinemaColors.AccentSoft else CinemaColors.White,
                bold = scrubbing,
            )
            TimeLabel(
                text = "-" + PlayerScrubLogic.format((durationMs - shownMs).coerceAtLeast(0L)),
                color = CinemaColors.TextSecondary,
            )
        }
    }
}

/**
 * Times always read left to right ("-1:26:43"), even in Arabic. Without an explicit LTR
 * direction the RTL paragraph moved the minus sign to the end and wrapped the label.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimeLabel(text: String, color: Color, bold: Boolean = false) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        style = MaterialTheme.typography.labelLarge.copy(
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            textDirection = TextDirection.Ltr,
        ),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerLiveNowNext(
    current: PlayerLiveProgramDisplay?,
    next: PlayerLiveProgramDisplay?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.player_on_now),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = CinemaColors.AccentSoft,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = current?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.msg_no_program_info),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        current?.progress?.takeIf { it > 0f }?.let { progress ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .drawBehind {
                        val rtl = layoutDirection == LayoutDirection.Rtl
                        val radius = CornerRadius(size.height / 2f)
                        drawRoundRect(CinemaColors.White.copy(alpha = 0.25f), cornerRadius = radius)
                        val fillWidth = size.width * progress.coerceIn(0f, 1f)
                        drawRoundRect(
                            color = CinemaColors.LiveRed,
                            topLeft = Offset(if (rtl) size.width - fillWidth else 0f, 0f),
                            size = Size(fillWidth, size.height),
                            cornerRadius = radius,
                        )
                    },
            )
        }
        next?.title?.takeIf { it.isNotBlank() }?.let { nextTitle ->
            Text(
                text = stringResource(R.string.player_up_next) + " · " + nextTitle,
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayerControlRow(
    isLive: Boolean,
    isPlaying: Boolean,
    showNextEpisode: Boolean,
    nextEpisodeCode: String?,
    showEpisodes: Boolean,
    showChannels: Boolean,
    onPlayPause: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onNextEpisode: () -> Unit,
    onEpisodes: () -> Unit,
    onChannels: () -> Unit,
    onTracks: () -> Unit,
    playPauseFocusRequester: FocusRequester?,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isLive) {
                ControlButton(
                    icon = Icons.Default.Replay10,
                    label = stringResource(R.string.player_btn_rewind_10),
                    onClick = { onSeekRelative(-SeekStepMs) },
                    mirrorIcon = isRtl,
                )
            }
            ControlButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = stringResource(if (isPlaying) R.string.player_btn_pause else R.string.player_btn_play),
                onClick = onPlayPause,
                size = 60.dp,
                focusRequester = playPauseFocusRequester,
            )
            if (!isLive) {
                ControlButton(
                    icon = Icons.Default.Forward10,
                    label = stringResource(R.string.player_btn_forward_10),
                    onClick = { onSeekRelative(SeekStepMs) },
                    mirrorIcon = isRtl,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showNextEpisode) {
                ControlButton(
                    icon = Icons.Default.SkipNext,
                    label = nextEpisodeCode?.let { stringResource(R.string.player_next_episode_code, it) }
                        ?: stringResource(R.string.player_next_episode),
                    onClick = onNextEpisode,
                    mirrorIcon = isRtl,
                )
            }
            if (showEpisodes) {
                ControlButton(
                    icon = Icons.Default.VideoLibrary,
                    label = stringResource(R.string.player_episodes),
                    onClick = onEpisodes,
                )
            }
            if (showChannels) {
                ControlButton(
                    icon = Icons.Default.LiveTv,
                    label = stringResource(R.string.player_channels),
                    onClick = onChannels,
                )
            }
            ControlButton(
                icon = Icons.Default.Subtitles,
                label = stringResource(R.string.player_track_settings),
                onClick = onTracks,
            )
        }
    }
}

private const val SeekStepMs = 10_000L

/**
 * Round control. Focused it turns white with a dark icon and shows its label underneath.
 * The label slot is always reserved and only faded, so focus moves never shift the row.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    size: Dp = 52.dp,
    mirrorIcon: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FocusableCinemaCard(
            modifier = Modifier
                .size(size)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onFocusChanged { focused = it.isFocused },
            onClick = onClick,
            shape = CircleShape,
            focusedBorderWidth = 0.dp,
            focusScale = 1.08f,
            contentDescription = label,
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isFocused) CinemaColors.White else CinemaColors.White.copy(alpha = 0.14f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) CinemaColors.Background else CinemaColors.White,
                    modifier = Modifier
                        .size(size * 0.5f)
                        .graphicsLayer { scaleX = if (mirrorIcon) -1f else 1f },
                )
            }
        }
        Text(
            text = label,
            modifier = Modifier.graphicsLayer { alpha = if (focused) 1f else 0f },
            style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.White),
            maxLines = 1,
        )
    }
}
