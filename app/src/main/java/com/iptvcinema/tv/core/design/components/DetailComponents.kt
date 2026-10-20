package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.CastMember
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.player.EpisodeWatchProgress
import com.iptvcinema.tv.core.player.EpisodeWatchVisualState
import com.iptvcinema.tv.core.util.isolateDirection

private val EpisodeLandscapeCardWidth = 220.dp
private val EpisodeLandscapeImageHeight = 124.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeLandscapeCard(
    episodeNumber: Int,
    title: String,
    durationMinutes: Int,
    thumbnailUrl: String?,
    fallbackImageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val episodeLabel = title.ifBlank {
        stringResource(R.string.details_episode_label, episodeNumber)
    }
    val watchState = EpisodeWatchProgress.visualState(progress)
    Column(
        modifier = modifier.width(EpisodeLandscapeCardWidth),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FocusableCinemaCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(EpisodeLandscapeImageHeight),
            onClick = onClick,
            shape = CinemaShapes.Card,
            defaultBorderWidth = 0.dp,
            contentDescription = episodeLabel,
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CinemaShapes.Card)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = thumbnailUrl ?: fallbackImageUrl,
                    contentDescription = episodeLabel,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = episodeLabel,
                )
                ArtworkPillLabel(
                    text = stringResource(R.string.details_episode_number, episodeNumber),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp),
                )
                if (watchState == EpisodeWatchVisualState.WATCHED) {
                    WatchedMark(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp),
                    )
                }
                if (watchState == EpisodeWatchVisualState.PARTIAL && progress != null) {
                    InsetProgressBar(
                        fraction = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
        Text(
            text = episodeLabel.isolateDirection(),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = CinemaColors.White,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (durationMinutes > 0) {
            Text(
                text = stringResource(R.string.details_episode_duration_minutes, durationMinutes),
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                maxLines = 1,
            )
        }
    }
}

/** Small dark pill over artwork, for episode numbers and similar short labels. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ArtworkPillLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .background(CinemaColors.Background.copy(alpha = 0.8f), CinemaShapes.Pill)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            color = CinemaColors.White,
        ),
        maxLines = 1,
    )
}

/** Check mark in a dark circle, for watched episodes. */
@Composable
private fun WatchedMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(CinemaColors.Background.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.player_episode_watched),
            tint = CinemaColors.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun programProgress(program: EpgProgram, nowMs: Long): Float {
    val durationMs = (program.endEpochMs - program.startEpochMs).coerceAtLeast(1)
    val elapsedMs = (nowMs - program.startEpochMs).coerceIn(0, durationMs)
    return elapsedMs.toFloat() / durationMs.toFloat()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelProgramPanel(
    currentProgram: EpgProgram?,
    nextPrograms: List<EpgProgram>,
    nowMs: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = shellContentStart(), end = CinemaSpacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = stringResource(R.string.channel_now_playing))
        if (currentProgram != null) {
            ChannelProgramRow(
                program = currentProgram,
                nowMs = nowMs,
                showProgress = true,
            )
        } else {
            Text(
                text = stringResource(R.string.msg_no_program_info),
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            )
        }
        if (nextPrograms.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.channel_up_next))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nextPrograms.forEach { program ->
                    ChannelProgramRow(
                        program = program,
                        nowMs = nowMs,
                        showProgress = false,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgramLineupCard(
    program: EpgProgram,
    isNowPlaying: Boolean,
    nowMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier
            .width(220.dp)
            .height(120.dp),
        onClick = onClick,
        shape = CinemaShapes.Card,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        isNowPlaying -> CinemaColors.SurfaceRaised
                        focused -> CinemaColors.Surface
                        else -> CinemaColors.SurfaceSoft
                    },
                    CinemaShapes.Card,
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = GuideLayoutHelper.formatSlotLabel(program.startEpochMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                )
                if (isNowPlaying) {
                    BadgeChip(text = stringResource(R.string.badge_live))
                }
            }
            Text(
                text = program.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.ExtraBold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isNowPlaying) {
                Spacer(modifier = Modifier.weight(1f))
                InsetProgressBar(
                    fraction = programProgress(program, nowMs),
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = CinemaColors.Border,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelProgramRow(
    program: EpgProgram,
    nowMs: Long,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CinemaShapes.Row)
            .background(if (showProgress) CinemaColors.SurfaceRaised else CinemaColors.SurfaceSoft, CinemaShapes.Row)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = program.title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.ExtraBold,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                R.string.livetv_program_time,
                GuideLayoutHelper.formatSlotLabel(program.startEpochMs),
                GuideLayoutHelper.formatSlotLabel(program.endEpochMs),
                pluralStringResource(
                    R.plurals.minutes_remaining,
                    program.durationMinutes.coerceAtLeast(1),
                    program.durationMinutes.coerceAtLeast(1),
                ),
            ),
            style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
        )
        if (program.description.isNotBlank()) {
            Text(
                text = program.description.isolateDirection(),
                style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showProgress) {
            InsetProgressBar(
                fraction = programProgress(program, nowMs),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                trackColor = CinemaColors.Border,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CastCard(
    member: CastMember,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.width(100.dp),
        onClick = {},
        shape = CinemaShapes.Card,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
    ) { focused ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(6.dp),
        ) {
            // Round avatar; focus turns it white, like a focused button.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(if (focused) CinemaColors.White else CinemaColors.SurfaceRaised, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = member.name.take(1),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = if (focused) CinemaColors.Background else CinemaColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
            Text(
                text = member.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (focused) CinemaColors.White else CinemaColors.TextSecondary,
                    fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.role,
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeasonSelector(
    seasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
    ) {
        seasons.forEach { season ->
            CategoryChip(
                label = stringResource(R.string.details_season, season),
                isSelected = season == selectedSeason,
                onClick = { onSeasonSelected(season) },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(
    episodeNumber: Int,
    title: String,
    durationMinutes: Int,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
) {
    FocusableCinemaCard(
        modifier = modifier
            .width(260.dp)
            .height(90.dp),
        onClick = onClick,
        shape = CinemaShapes.Card,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        focused -> CinemaColors.Surface
                        isPlaying -> CinemaColors.SurfaceRaised
                        else -> CinemaColors.SurfaceSoft
                    },
                    CinemaShapes.Card,
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 64.dp)
                    .background(CinemaColors.Background, CinemaShapes.Card),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.details_episode_number, episodeNumber),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isPlaying) CinemaColors.AccentSoft else CinemaColors.White,
                        fontWeight = FontWeight.Black,
                    ),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title.isolateDirection(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.details_episode_duration, durationMinutes),
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                )
                if (progress != null && progress > 0f) {
                    InsetProgressBar(
                        fraction = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        trackColor = CinemaColors.Border,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerEpisodeSidebarRow(
    episodeNumber: Int,
    title: String,
    durationMinutes: Int,
    thumbnailUrl: String?,
    fallbackImageUrl: String?,
    progress: Float?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val watchState = EpisodeWatchProgress.visualState(progress)

    FocusableCinemaCard(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        onClick = onClick,
        shape = CinemaShapes.Card,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        focused -> CinemaColors.Surface
                        isPlaying -> CinemaColors.SurfaceRaised
                        else -> CinemaColors.SurfaceSoft.copy(alpha = 0.7f)
                    },
                    CinemaShapes.Card,
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.details_episode_number, episodeNumber),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isPlaying) CinemaColors.AccentSoft else CinemaColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = title.isolateDirection(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(CinemaShapes.Card),
            ) {
                CinemaAsyncImage(
                    imageUrl = thumbnailUrl ?: fallbackImageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (watchState == EpisodeWatchVisualState.WATCHED) 0.62f else 1f
                        },
                    contentScale = ContentScale.Crop,
                    fallbackLabel = episodeNumber.toString(),
                )
                if (watchState == EpisodeWatchVisualState.PARTIAL) {
                    InsetProgressBar(
                        fraction = progress ?: 0f,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 6.dp, bottom = 5.dp),
                        height = 3.dp,
                    )
                }
                if (watchState == EpisodeWatchVisualState.WATCHED) {
                    WatchedMark(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .background(CinemaColors.Background.copy(alpha = 0.8f), CinemaShapes.Pill)
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = stringResource(R.string.details_episode_duration, durationMinutes),
                        style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.White),
                    )
                }
            }
        }
    }
}
