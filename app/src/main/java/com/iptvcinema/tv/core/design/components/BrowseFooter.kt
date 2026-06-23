package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowseFooter(
    onFavorites: () -> Unit,
    onRecentlyAdded: () -> Unit,
    onTopRated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowseFooterLink(
                icon = Icons.Default.Favorite,
                label = stringResource(R.string.footer_favorites),
                onClick = onFavorites,
            )
            BrowseFooterLink(
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.footer_recently_added),
                onClick = onRecentlyAdded,
            )
            BrowseFooterLink(
                icon = Icons.Default.Star,
                label = stringResource(R.string.footer_top_rated),
                onClick = onTopRated,
            )
        }
        Text(
            text = "●●●●○  ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}",
            style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BrowseFooterLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FocusableCinemaCard(
        onClick = onClick,
        shape = CinemaShapes.Large,
    ) { focused ->
        Row(
            modifier = Modifier
                .background(
                    if (focused) CinemaColors.Surface else CinemaColors.SurfaceGlass,
                    CinemaShapes.Large,
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (focused) CinemaColors.Gold else CinemaColors.TextMuted,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (focused) CinemaColors.GoldSoft else CinemaColors.TextSecondary,
                ),
            )
        }
    }
}
