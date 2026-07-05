package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

private const val REFRESHING_CONTENT_ALPHA = 0.72f

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SyncStatusBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    subtitle: String? = null,
    progress: Float? = null,
    isSuccess: Boolean = false,
    isFailed: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            focusRequester.requestFocus()
        }
    }

    val contentColor = when {
        isFailed -> CinemaColors.Danger
        isSuccess -> CinemaColors.Success
        isRefreshing -> CinemaColors.GoldSoft.copy(alpha = REFRESHING_CONTENT_ALPHA)
        else -> CinemaColors.GoldSoft
    }
    val icon = when {
        isFailed -> Icons.Default.Error
        isSuccess -> Icons.Default.CheckCircle
        else -> Icons.Default.Refresh
    }

    FocusableCinemaCard(
        modifier = modifier
            .widthIn(min = 280.dp, max = 420.dp)
            .focusRequester(focusRequester),
        enabled = true,
        onClick = {
            if (!isRefreshing) {
                onClick()
            }
        },
        shape = CinemaShapes.Medium,
        contentDescription = text,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = contentColor,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let { stepText ->
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = contentColor.copy(alpha = 0.85f),
                            ),
                        )
                    }
                }
            }
            if (isRefreshing && progress != null) {
                CinemaProgressBar(fraction = progress)
            }
        }
    }
}

@Composable
fun CatalogRefreshBanner(
    syncBannerText: String?,
    refreshState: CatalogRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bannerText = when (refreshState) {
        CatalogRefreshState.Idle -> syncBannerText
        is CatalogRefreshState.Refreshing -> stringResource(R.string.refresh_in_progress)
        is CatalogRefreshState.Success -> refreshState.message
        is CatalogRefreshState.Failed -> refreshState.message
    }
    bannerText?.let { text ->
        val refreshing = refreshState as? CatalogRefreshState.Refreshing
        SyncStatusBanner(
            text = text,
            isRefreshing = refreshing != null,
            subtitle = refreshing?.stepLabel,
            progress = refreshing?.progress,
            isSuccess = refreshState is CatalogRefreshState.Success,
            isFailed = refreshState is CatalogRefreshState.Failed,
            onClick = onRefresh,
            modifier = modifier,
        )
    }
}
