package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType

data class CatalogStateCallbacks(
    val onAddSource: () -> Unit,
    val onRetry: () -> Unit,
    val onManageSources: () -> Unit,
    val onEditSource: () -> Unit,
)

enum class CatalogSkeletonStyle {
    Home,
    PosterGrid,
    PosterGridWithFeatured,
    ChannelList,
    Epg,
    ProfileRow,
    SourceCards,
    Simple,
    DetailHero,
}

@Composable
fun CatalogStateContent(
    loadState: CatalogLoadState,
    message: String?,
    sourceStatus: SourceStatus?,
    sourceType: SourceType?,
    skeletonStyle: CatalogSkeletonStyle,
    emptyTitle: String? = null,
    emptyDescription: String? = null,
    onAddSource: () -> Unit,
    onRetry: () -> Unit,
    onManageSources: () -> Unit,
    onEditSource: () -> Unit,
    onRefreshCatalog: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    readyContent: @Composable () -> Unit,
) {
    val resolvedEmptyTitle = emptyTitle ?: stringResource(R.string.empty_nothing_here)
    val resolvedEmptyDescription = emptyDescription ?: stringResource(R.string.empty_try_another)

    val displayState = when {
        sourceStatus == SourceStatus.EXPIRED -> CatalogDisplayState.Expired
        sourceStatus == SourceStatus.FAILED && sourceType == SourceType.M3U -> CatalogDisplayState.InvalidPlaylist
        loadState == CatalogLoadState.Loading -> CatalogDisplayState.Loading
        loadState == CatalogLoadState.Empty && message?.contains("No source", ignoreCase = true) == true ->
            CatalogDisplayState.NoSource
        loadState == CatalogLoadState.Empty -> CatalogDisplayState.Empty
        loadState == CatalogLoadState.Error -> CatalogDisplayState.Error
        else -> CatalogDisplayState.Ready
    }

    // Fade between states so the skeleton dissolves into content instead of a hard cut.
    AnimatedContent(
        targetState = displayState,
        transitionSpec = {
            fadeIn(tween(StateFadeInMs)) togetherWith fadeOut(tween(StateFadeOutMs)) using null
        },
        label = "catalogState",
    ) { state ->
        when (state) {
            CatalogDisplayState.Expired -> {
                ExpiredAccountState(
                    onReconnect = onManageSources,
                    onManageSources = onManageSources,
                    modifier = modifier,
                )
            }
            CatalogDisplayState.InvalidPlaylist -> {
                InvalidPlaylistState(
                    onEditSource = onEditSource,
                    onManageSources = onManageSources,
                    modifier = modifier,
                )
            }
            CatalogDisplayState.Loading -> {
                CatalogSkeleton(style = skeletonStyle, modifier = modifier)
            }
            CatalogDisplayState.NoSource -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = stringResource(R.string.empty_title),
                        description = stringResource(R.string.empty_description),
                        primaryAction = stringResource(R.string.btn_add_source),
                        secondaryAction = null,
                        onPrimary = onAddSource,
                        onSecondary = null,
                        footerNote = stringResource(R.string.source_footer),
                    )
                }
            }
            CatalogDisplayState.Empty -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = resolvedEmptyTitle,
                        description = message ?: resolvedEmptyDescription,
                        primaryAction = if (onRefreshCatalog != null) {
                            stringResource(R.string.btn_refresh_catalog)
                        } else {
                            stringResource(R.string.btn_manage_sources)
                        },
                        secondaryAction = if (onRefreshCatalog != null) {
                            stringResource(R.string.btn_manage_sources)
                        } else {
                            null
                        },
                        onPrimary = onRefreshCatalog ?: onManageSources,
                        onSecondary = if (onRefreshCatalog != null) onManageSources else null,
                        footerNote = null,
                    )
                }
            }
            CatalogDisplayState.Error -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CatalogErrorState(
                        message = message,
                        onRetry = onRetry,
                        onManageSources = onManageSources,
                    )
                }
            }
            CatalogDisplayState.Ready -> {
                Box(modifier = modifier.fillMaxSize()) {
                    readyContent()
                }
            }
        }
    }
}

private enum class CatalogDisplayState { Expired, InvalidPlaylist, Loading, NoSource, Empty, Error, Ready }

private const val StateFadeInMs = 220
private const val StateFadeOutMs = 160

@Composable
private fun CatalogSkeleton(
    style: CatalogSkeletonStyle,
    modifier: Modifier = Modifier,
) {
    when (style) {
        CatalogSkeletonStyle.Home -> SkeletonHomeContent(modifier = modifier.fillMaxWidth())
        CatalogSkeletonStyle.PosterGrid -> SkeletonPosterGrid(modifier = modifier.fillMaxWidth())
        CatalogSkeletonStyle.PosterGridWithFeatured -> {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                    com.iptvcinema.tv.core.design.theme.CinemaSpacing.SectionGap,
                ),
            ) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 180.dp)
                SkeletonPosterGrid(modifier = modifier.fillMaxWidth())
            }
        }
        CatalogSkeletonStyle.ChannelList -> SkeletonChannelRow(count = 8, modifier = modifier.fillMaxWidth())
        CatalogSkeletonStyle.Epg -> SkeletonEpgGrid(modifier = modifier.fillMaxWidth())
        CatalogSkeletonStyle.ProfileRow -> SkeletonProfileRow(modifier = modifier)
        CatalogSkeletonStyle.SourceCards -> SkeletonSourceCards(modifier = modifier.fillMaxWidth())
        CatalogSkeletonStyle.Simple -> SkeletonBox(modifier = modifier.fillMaxWidth(), height = 120.dp)
        CatalogSkeletonStyle.DetailHero -> SkeletonDetailHero(modifier = modifier.fillMaxWidth())
    }
}

@Composable
fun ExpiredAccountState(
    onReconnect: () -> Unit,
    onManageSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessage(
            icon = Icons.Outlined.Timer,
            tone = CinemaColors.Warning,
            title = stringResource(R.string.error_subscription_expired),
            description = stringResource(R.string.error_subscription_expired_desc),
            primaryAction = stringResource(R.string.btn_reconnect),
            onPrimary = onReconnect,
            secondaryAction = stringResource(R.string.btn_manage_sources),
            onSecondary = onManageSources,
        )
    }
}

@Composable
fun InvalidPlaylistState(
    onEditSource: () -> Unit,
    onManageSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessage(
            icon = Icons.Outlined.PlaylistRemove,
            tone = StatusDangerTone,
            title = stringResource(R.string.error_invalid_playlist),
            description = stringResource(R.string.error_invalid_playlist_desc),
            primaryAction = stringResource(R.string.btn_edit_playlist),
            onPrimary = onEditSource,
            secondaryAction = stringResource(R.string.btn_manage_sources),
            onSecondary = onManageSources,
        )
    }
}

@Composable
private fun CatalogErrorState(
    message: String?,
    onRetry: () -> Unit,
    onManageSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorState(
        title = stringResource(R.string.error_unable_load_content),
        description = message ?: stringResource(R.string.error_catalog_generic),
        errorCode = stringResource(R.string.error_catalog_code),
        onRetry = onRetry,
        onSwitchStream = onManageSources,
        onBack = onManageSources,
        modifier = modifier,
    )
}

/** Lighter red used for icons and text on dark tinted fills. */
val StatusDangerTone = Color(0xFFFF6B6B)
private val StatusIconCircleSize = 72.dp
private val StatusIconSize = 32.dp
private val StatusMessageWidth = 520.dp
private const val STATUS_TINT_ALPHA = 0.16f

/**
 * Centered full-screen message: a tinted icon circle, a title, one line of explanation and
 * up to two actions. Used for expired accounts, invalid playlists and server errors.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusMessage(
    icon: ImageVector,
    tone: Color,
    title: String,
    description: String,
    primaryAction: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.widthIn(max = StatusMessageWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .size(StatusIconCircleSize)
                .background(tone.copy(alpha = STATUS_TINT_ALPHA), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(StatusIconSize),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.White,
            ),
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CinemaButton(text = primaryAction, variant = CinemaButtonVariant.PrimaryAccent, onClick = onPrimary)
            if (secondaryAction != null && onSecondary != null) {
                CinemaButton(text = secondaryAction, variant = CinemaButtonVariant.SecondaryDark, onClick = onSecondary)
            }
        }
    }
}
