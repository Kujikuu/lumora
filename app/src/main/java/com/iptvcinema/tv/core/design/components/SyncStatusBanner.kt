package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SyncStatusBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
) {
    FocusableCinemaCard(
        modifier = modifier
            .fillMaxWidth(),
        enabled = !isRefreshing,
        onClick = onClick,
        shape = CinemaShapes.Medium,
        contentDescription = text,
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CinemaColors.GoldSoft,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CinemaColors.GoldSoft,
                    fontWeight = FontWeight.Medium,
                ),
            )
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
        CatalogRefreshState.Refreshing -> stringResource(R.string.refresh_in_progress)
        is CatalogRefreshState.Success -> refreshState.message
        is CatalogRefreshState.Failed -> refreshState.message
    }
    bannerText?.let { text ->
        SyncStatusBanner(
            text = text,
            isRefreshing = refreshState == CatalogRefreshState.Refreshing,
            onClick = onRefresh,
            modifier = modifier,
        )
    }
}
