package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.CastMember

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailHero(
    title: String,
    metadata: List<String>,
    synopsis: String,
    onWatchNow: () -> Unit,
    onTrailer: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier,
    primaryActionLabel: String = "",
    favoriteLabel: String = "",
    favoritedLabel: String = "",
    backdropUrl: String? = null,
    watchNowFocusRequester: FocusRequester? = null,
) {
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
            .heightIn(min = CinemaSpacing.HeroMinHeight, max = CinemaSpacing.HeroMaxHeight)
            .clip(CinemaShapes.Large),
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
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            CinemaColors.Background.copy(alpha = 0.94f),
                            CinemaColors.Background.copy(alpha = 0.68f),
                            CinemaColors.Background.copy(alpha = 0.18f),
                            androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .padding(CinemaSpacing.SectionGap),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metadata.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.65f),
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                CinemaButton(
                    text = resolvedPrimaryActionLabel,
                    variant = CinemaButtonVariant.PrimaryGold,
                    icon = Icons.Default.PlayArrow,
                    onClick = onWatchNow,
                    modifier = watchNowFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
                CinemaButton(text = stringResource(R.string.btn_trailer), variant = CinemaButtonVariant.SecondaryDark, onClick = onTrailer)
                CinemaButton(
                    text = if (isFavorite) resolvedFavoritedLabel else resolvedFavoriteLabel,
                    variant = CinemaButtonVariant.Ghost,
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    onClick = onFavorite,
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
        modifier = modifier.width(120.dp),
        onClick = {},
        shape = CinemaShapes.Medium,
    ) { _ ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CinemaShapes.Large)
                    .background(CinemaColors.SurfaceSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = member.name.take(1),
                    style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.Gold),
                )
            }
            Text(
                text = member.name,
                style = MaterialTheme.typography.labelMedium,
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
            .width(280.dp)
            .height(100.dp)
            .then(
                if (isPlaying) {
                    Modifier.border(2.dp, CinemaColors.Gold, CinemaShapes.Medium)
                } else {
                    Modifier
                },
            ),
        onClick = onClick,
        shape = CinemaShapes.Medium,
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinemaColors.SurfaceSoft, CinemaShapes.Medium)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CinemaShapes.Small)
                    .background(CinemaColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.details_episode_number, episodeNumber),
                    style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.Gold),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
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
                            .background(CinemaColors.Surface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(CinemaColors.Gold),
                        )
                    }
                }
            }
        }
    }
}
