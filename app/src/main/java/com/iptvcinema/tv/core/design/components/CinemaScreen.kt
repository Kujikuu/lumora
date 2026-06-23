package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.NavItem

private val topNavPills = listOf(
    NavItem.Home,
    NavItem.LiveTv,
    NavItem.Movies,
    NavItem.Series,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaTopNav(
    selected: NavItem,
    onNavigate: (NavItem) -> Unit,
    onSearchClick: () -> Unit = { onNavigate(NavItem.Search) },
    onSettingsClick: () -> Unit = { onNavigate(NavItem.Settings) },
    onProfileClick: () -> Unit = { onNavigate(NavItem.Profile) },
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = CinemaSpacing.NavBottomPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CinemaLogo(navBar = true)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            items(topNavPills) { item ->
                CinemaNavPill(
                    label = stringResource(item.labelRes),
                    isSelected = item == selected,
                    onClick = { onNavigate(item) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchNavButton(
                isSelected = selected == NavItem.Search,
                onClick = onSearchClick,
            )
            NavIconButton(
                contentDescription = stringResource(R.string.nav_settings),
                isSelected = selected == NavItem.Settings,
                onClick = onSettingsClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (selected == NavItem.Settings) CinemaColors.Gold else CinemaColors.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            NavIconButton(
                contentDescription = stringResource(R.string.nav_profile),
                isSelected = selected == NavItem.Profile,
                onClick = onProfileClick,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (selected == NavItem.Profile) CinemaColors.Gold else CinemaColors.SurfaceSoft,
                            CinemaShapes.Large,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "M",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selected == NavItem.Profile) CinemaColors.Background else CinemaColors.TextPrimary,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchNavButton(
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FocusableCinemaCard(
        onClick = onClick,
        shape = CinemaShapes.Large,
    ) { focused ->
        Row(
            modifier = Modifier
                .background(
                    color = when {
                        isSelected -> CinemaColors.Gold
                        focused -> CinemaColors.Surface
                        else -> CinemaColors.SurfaceSoft
                    },
                    shape = CinemaShapes.Large,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isSelected) CinemaColors.Background else CinemaColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.nav_search),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) CinemaColors.Background else CinemaColors.TextSecondary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavIconButton(
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FocusableCinemaCard(
        modifier = modifier.size(40.dp),
        onClick = onClick,
        shape = CinemaShapes.Large,
    ) { _ ->
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) CinemaColors.Surface else CinemaColors.SurfaceSoft,
                    CinemaShapes.Large,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CinemaNavPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FocusableCinemaCard(
        onClick = onClick,
        shape = CinemaShapes.Large,
    ) { focused ->
        Box(
            modifier = Modifier
                .background(
                    color = when {
                        isSelected -> CinemaColors.Gold
                        focused -> CinemaColors.Surface
                        else -> CinemaColors.SurfaceSoft
                    },
                    shape = CinemaShapes.Large,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) CinemaColors.Background else CinemaColors.TextSecondary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusFooter(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "●●●●○  ${java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())}",
            style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaScreen(
    showTopNav: Boolean = true,
    selectedNavItem: NavItem? = null,
    onNavigate: ((NavItem) -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    showRemoteHints: Boolean = false,
    showBrowseFooter: Boolean = false,
    onFavoritesClick: (() -> Unit)? = null,
    onRecentlyAddedClick: (() -> Unit)? = null,
    onTopRatedClick: (() -> Unit)? = null,
    remoteHints: List<RemoteHint> = defaultRemoteHints(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CinemaColors.GoldDeep.copy(alpha = 0.11f),
                        CinemaColors.BackgroundSoft,
                        CinemaColors.Background,
                    ),
                ),
            )
            .padding(
                horizontal = CinemaSpacing.ScreenPadding,
                vertical = CinemaSpacing.ScreenPaddingVertical,
            ),
    ) {
        if (showTopNav && selectedNavItem != null && onNavigate != null) {
            CinemaTopNav(
                selected = selectedNavItem,
                onNavigate = onNavigate,
                onSearchClick = onSearchClick ?: { onNavigate(NavItem.Search) },
                onSettingsClick = onSettingsClick ?: { onNavigate(NavItem.Settings) },
                onProfileClick = onProfileClick ?: { onNavigate(NavItem.Profile) },
                modifier = Modifier.padding(bottom = CinemaSpacing.NavBottomPadding),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopStart,
        ) {
            content()
        }

        if (showRemoteHints) {
            RemoteHintBar(
                hints = remoteHints,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (showBrowseFooter && onFavoritesClick != null && onRecentlyAddedClick != null && onTopRatedClick != null) {
            BrowseFooter(
                onFavorites = onFavoritesClick,
                onRecentlyAdded = onRecentlyAddedClick,
                onTopRated = onTopRatedClick,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
