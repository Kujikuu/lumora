package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.CastMember
import com.iptvcinema.tv.core.model.EpgProgram

private val DetailPosterInsetWidth = 120.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailHero(
    title: String,
    metadata: List<String>,
    synopsis: String,
    onWatchNow: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier,
    primaryActionLabel: String = "",
    favoriteLabel: String = "",
    favoritedLabel: String = "",
    showTrailer: Boolean = false,
    onTrailer: (() -> Unit)? = null,
    backdropUrl: String? = null,
    posterUrl: String? = null,
    watchNowFocusRequester: FocusRequester? = null,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val sideGradientColors = if (isRtl) {
        listOf(
            Color.Transparent,
            CinemaColors.Background.copy(alpha = 0.25f),
            CinemaColors.Background.copy(alpha = 0.86f),
        )
    } else {
        listOf(
            CinemaColors.Background.copy(alpha = 0.86f),
            CinemaColors.Background.copy(alpha = 0.25f),
            Color.Transparent,
        )
    }
    val resolvedPrimaryActionLabel = primaryActionLabel.ifBlank {
        stringResource(R.string.btn_watch_now)
    }
    val resolvedFavoriteLabel = favoriteLabel.ifBlank {
        stringResource(R.string.btn_add_to_favorites)
    }
    val resolvedFavoritedLabel = favoritedLabel.ifBlank {
        stringResource(R.string.btn_favorited)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CinemaSpacing.HeroMinHeight, max = CinemaSpacing.HeroMaxHeight),
    ) {
        CinemaAsyncImage(
            imageUrl = backdropUrl,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallbackLabel = title,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            CinemaColors.Background.copy(alpha = 0.6f),
                            CinemaColors.Background,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        sideGradientColors,
                    ),
                ),
        )

        if (!posterUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(if (isRtl) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(
                        start = if (isRtl) 0.dp else CinemaSpacing.ContentStart,
                        end = if (isRtl) CinemaSpacing.ContentStart else 0.dp,
                        bottom = 20.dp,
                    )
                    .width(DetailPosterInsetWidth)
                    .height(DetailPosterInsetWidth * 1.5f)
                    .clip(CinemaShapes.Medium)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = posterUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = title,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.6f)
                .padding(
                    start = CinemaSpacing.ContentStart +
                        if (!posterUrl.isNullOrBlank() && !isRtl) DetailPosterInsetWidth + 16.dp else 0.dp,
                    end = if (!posterUrl.isNullOrBlank() && isRtl) DetailPosterInsetWidth + 16.dp else 0.dp,
                    bottom = 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (metadata.isNotEmpty()) {
                MetadataRow(items = metadata.take(5))
            }
            if (synopsis.isNotBlank()) {
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextPrimary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                CinemaButton(
                    text = resolvedPrimaryActionLabel,
                    variant = CinemaButtonVariant.PrimaryAccent,
                    icon = Icons.Default.PlayArrow,
                    onClick = onWatchNow,
                    modifier = watchNowFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
                CinemaButton(
                    text = if (isFavorite) resolvedFavoritedLabel else resolvedFavoriteLabel,
                    variant = CinemaButtonVariant.Ghost,
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    onClick = onFavorite,
                )
                if (showTrailer && onTrailer != null) {
                    CinemaButton(
                        text = stringResource(R.string.btn_trailer),
                        variant = CinemaButtonVariant.SecondaryDark,
                        onClick = onTrailer,
                    )
                }
            }
        }
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
            .padding(horizontal = CinemaSpacing.NavRailWidth + 16.dp),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
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
        shape = CinemaShapes.Medium,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (focused) CinemaColors.Surface else CinemaColors.SurfaceSoft,
                    CinemaShapes.Medium,
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isNowPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CinemaShapes.Small)
                        .background(CinemaColors.Surface),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(programProgress(program, nowMs).coerceIn(0f, 1f))
                            .background(CinemaColors.Accent),
                    )
                }
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
            .clip(CinemaShapes.Medium)
            .background(CinemaColors.SurfaceSoft, CinemaShapes.Medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = program.title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.SemiBold,
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
                text = program.description,
                style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showProgress) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.Surface),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(programProgress(program, nowMs).coerceIn(0f, 1f))
                        .background(CinemaColors.Accent),
                )
            }
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
        shape = CinemaShapes.Medium,
        defaultBorderWidth = 0.dp,
    ) { _ ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CinemaShapes.XLarge)
                    .background(CinemaColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = member.name.take(1),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Text(
                text = member.name,
                style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextPrimary),
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
        shape = CinemaShapes.Medium,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (focused) CinemaColors.Surface else CinemaColors.SurfaceSoft,
                    CinemaShapes.Medium,
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 64.dp)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.details_episode_number, episodeNumber),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.details_episode_duration, durationMinutes),
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                )
                if (progress != null && progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(3.dp)
                            .background(CinemaColors.Background),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(CinemaColors.Accent),
                        )
                    }
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
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        onClick = onClick,
        shape = CinemaShapes.Medium,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        focused -> CinemaColors.Surface
                        isPlaying -> CinemaColors.SurfaceSoft.copy(alpha = 0.85f)
                        else -> CinemaColors.Background.copy(alpha = 0.5f)
                    },
                    CinemaShapes.Medium,
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.details_episode_number, episodeNumber),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isPlaying) CinemaColors.Accent else CinemaColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(CinemaShapes.Small),
            ) {
                CinemaAsyncImage(
                    imageUrl = thumbnailUrl ?: fallbackImageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = episodeNumber.toString(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(CinemaColors.Background.copy(alpha = 0.75f), CinemaShapes.Small)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
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
