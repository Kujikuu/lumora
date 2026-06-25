package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.NavItem

private val RailCollapsedWidth = 56.dp
private val RailExpandedWidth = 220.dp

private data class RailEntry(
    val navItem: NavItem,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaNavRail(
    selected: NavItem,
    onNavigate: (NavItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(
        targetValue = if (expanded) RailExpandedWidth else RailCollapsedWidth,
        animationSpec = tween(durationMillis = 200),
        label = "railWidth",
    )

    val primaryItems = listOf(
        RailEntry(NavItem.Home, Icons.Default.Home) { onNavigate(NavItem.Home) },
        RailEntry(NavItem.Search, Icons.Default.Search, onSearchClick),
        RailEntry(NavItem.LiveTv, Icons.Default.LiveTv) { onNavigate(NavItem.LiveTv) },
        RailEntry(NavItem.Movies, Icons.Default.Movie) { onNavigate(NavItem.Movies) },
        RailEntry(NavItem.Series, Icons.Default.VideoLibrary) { onNavigate(NavItem.Series) },
        RailEntry(NavItem.MyList, Icons.Default.Favorite) { onNavigate(NavItem.MyList) },
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        CinemaColors.Background.copy(alpha = if (expanded) 0.95f else 0.85f),
                        CinemaColors.Background.copy(alpha = 0f),
                    ),
                ),
            )
            .focusGroup()
            .onFocusChanged { onExpandedChange(it.hasFocus) }
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CinemaBrandMark(size = 32.dp)

        Spacer(Modifier.size(16.dp))

        primaryItems.forEach { entry ->
            RailItemRow(
                label = stringResource(entry.navItem.labelRes),
                icon = entry.icon,
                selected = entry.navItem == selected,
                expanded = expanded,
                onClick = entry.onClick,
            )
        }

        Spacer(Modifier.weight(1f))

        RailItemRow(
            label = stringResource(R.string.nav_profile),
            icon = Icons.Default.Person,
            selected = selected == NavItem.Profile,
            expanded = expanded,
            onClick = onProfileClick,
        )
        RailItemRow(
            label = stringResource(NavItem.Settings.labelRes),
            icon = Icons.Default.Settings,
            selected = selected == NavItem.Settings,
            expanded = expanded,
            onClick = onSettingsClick,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RailItemRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    FocusableCinemaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = CinemaShapes.Small,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = 1.0f,
        contentDescription = label,
    ) { focused ->
        val contentColor = when {
            selected -> CinemaColors.White
            focused -> CinemaColors.White
            else -> CinemaColors.TextMuted
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        selected -> CinemaColors.White.copy(alpha = 0.12f)
                        focused -> CinemaColors.White.copy(alpha = 0.06f)
                        else -> CinemaColors.Background.copy(alpha = 0f)
                    },
                    CinemaShapes.Small,
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(150)) + expandHorizontally(tween(180)),
                exit = fadeOut(tween(100)) + shrinkHorizontally(tween(140)),
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                    ),
                )
            }
        }
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
    val showRail = showTopNav && selectedNavItem != null && onNavigate != null
    var railExpanded by remember { mutableStateOf(false) }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (railExpanded) 0.4f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "railScrim",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier.padding(
                        start = CinemaSpacing.ContentStart,
                        end = CinemaSpacing.ScreenPadding,
                        bottom = 8.dp,
                    ),
                )
            }
        }

        if (showRail) {
            if (scrimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .background(CinemaColors.Background.copy(alpha = scrimAlpha)),
                )
            }

            CinemaNavRail(
                selected = selectedNavItem!!,
                onNavigate = onNavigate!!,
                onSearchClick = onSearchClick ?: { onNavigate(NavItem.Search) },
                onSettingsClick = onSettingsClick ?: { onNavigate(NavItem.Settings) },
                onProfileClick = onProfileClick ?: { onNavigate(NavItem.Profile) },
                expanded = railExpanded,
                onExpandedChange = { railExpanded = it },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(2f),
            )
        }
    }
}
