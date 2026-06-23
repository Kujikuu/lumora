package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType

data class CatalogStateCallbacks(
    val onAddSource: () -> Unit,
    val onTryDemo: () -> Unit,
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
    onTryDemo: () -> Unit,
    onRetry: () -> Unit,
    onManageSources: () -> Unit,
    onEditSource: () -> Unit,
    modifier: Modifier = Modifier,
    readyContent: @Composable () -> Unit,
) {
    val resolvedEmptyTitle = emptyTitle ?: stringResource(R.string.empty_nothing_here)
    val resolvedEmptyDescription = emptyDescription ?: stringResource(R.string.empty_try_another)

    when {
        sourceStatus == SourceStatus.EXPIRED -> {
            ExpiredAccountState(
                onReconnect = onManageSources,
                onManageSources = onManageSources,
                modifier = modifier,
            )
        }
        sourceStatus == SourceStatus.FAILED && sourceType == SourceType.M3U -> {
            InvalidPlaylistState(
                onEditSource = onEditSource,
                onManageSources = onManageSources,
                modifier = modifier,
            )
        }
        loadState == CatalogLoadState.Loading -> {
            CatalogSkeleton(style = skeletonStyle, modifier = modifier)
        }
        loadState == CatalogLoadState.Empty && message?.contains("No source", ignoreCase = true) == true -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = stringResource(R.string.empty_title),
                    description = stringResource(R.string.empty_description),
                    primaryAction = stringResource(R.string.btn_add_source),
                    secondaryAction = stringResource(R.string.btn_try_demo),
                    onPrimary = onAddSource,
                    onSecondary = onTryDemo,
                    footerNote = stringResource(R.string.source_footer),
                )
            }
        }
        loadState == CatalogLoadState.Empty -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = resolvedEmptyTitle,
                    description = message ?: resolvedEmptyDescription,
                    primaryAction = stringResource(R.string.btn_manage_sources),
                    secondaryAction = stringResource(R.string.btn_try_demo),
                    onPrimary = onManageSources,
                    onSecondary = onTryDemo,
                    footerNote = null,
                )
            }
        }
        loadState == CatalogLoadState.Error -> {
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
        loadState == CatalogLoadState.Ready -> {
            readyContent()
        }
    }
}

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
        EmptyState(
            title = stringResource(R.string.error_subscription_expired),
            description = stringResource(R.string.error_subscription_expired_desc),
            primaryAction = stringResource(R.string.btn_reconnect),
            secondaryAction = stringResource(R.string.btn_manage_sources),
            onPrimary = onReconnect,
            onSecondary = onManageSources,
            footerNote = null,
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
        EmptyState(
            title = stringResource(R.string.error_invalid_playlist),
            description = stringResource(R.string.error_invalid_playlist_desc),
            primaryAction = stringResource(R.string.btn_edit_playlist),
            secondaryAction = stringResource(R.string.btn_manage_sources),
            onPrimary = onEditSource,
            onSecondary = onManageSources,
            footerNote = null,
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
