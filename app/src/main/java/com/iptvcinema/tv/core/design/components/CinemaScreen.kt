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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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

private val RailCollapsedWidth = 92.dp
private val RailExpandedWidth = 340.dp

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
    onExitRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(
        targetValue = if (expanded) RailExpandedWidth else RailCollapsedWidth,
        animationSpec = tween(durationMillis = 200),
        label = "railWidth",
    )

    val primaryItems = listOf(
        RailEntry(NavItem.Search, Icons.Default.Search, onSearchClick),
        RailEntry(NavItem.Home, Icons.Default.Home) { onNavigate(NavItem.Home) },
        RailEntry(NavItem.Movies, Icons.Default.Movie) { onNavigate(NavItem.Movies) },
        RailEntry(NavItem.MyList, Icons.Default.Favorite) { onNavigate(NavItem.MyList) },
        RailEntry(NavItem.LiveTv, Icons.Default.LiveTv) { onNavigate(NavItem.LiveTv) },
        RailEntry(NavItem.Series, Icons.Default.LibraryMusic) { onNavigate(NavItem.Series) },
        RailEntry(NavItem.Settings, Icons.Default.PhoneAndroid) { onSettingsClick() },
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
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionRight &&
                    onExitRight != null
                ) {
                    onExpandedChange(false)
                    onExitRight()
                    true
                } else {
                    false
                }
            }
            .focusGroup()
            .onFocusChanged { onExpandedChange(it.hasFocus) }
            .padding(vertical = 30.dp, horizontal = 6.dp),
        horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (expanded) {
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                modifier = Modifier.padding(start = 54.dp),
                style = MaterialTheme.typography.displaySmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
                maxLines = 1,
            )
        } else {
            CinemaLogo(navBar = true)
        }

        Spacer(Modifier.size(if (expanded) 66.dp else 46.dp))

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

        FocusableCinemaCard(
            modifier = if (expanded) Modifier.fillMaxWidth().height(64.dp) else Modifier.size(42.dp),
            onClick = onProfileClick,
            shape = CinemaShapes.XLarge,
            focusedBorderWidth = 0.dp,
            contentDescription = stringResource(R.string.nav_profile),
        ) { focused ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (focused || selected == NavItem.Profile) {
                            CinemaColors.Surface.copy(alpha = 0.45f)
                        } else {
                            CinemaColors.Background.copy(alpha = 0f)
                        },
                        CinemaShapes.XLarge,
                    )
                    .padding(start = if (expanded) 54.dp else 0.dp, end = if (expanded) 12.dp else 0.dp),
                horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountAvatar(size = if (expanded) 56.dp else 32.dp)
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Text(
                            text = stringResource(R.string.nav_profile_name),
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CinemaColors.White,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.nav_switch_accounts),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                        )
                    }
                }
            }
        }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(CinemaColors.Background.copy(alpha = 0f), CinemaShapes.Small),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (expanded) Arrangement.spacedBy(12.dp) else Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier.padding(start = if (expanded) 54.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (expanded) Arrangement.spacedBy(20.dp) else Arrangement.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp),
                    )
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(tween(150)) + expandHorizontally(tween(180)),
                        exit = fadeOut(tween(100)) + shrinkHorizontally(tween(140)),
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                            ),
                        )
                    }
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(width = 34.dp, height = 3.dp)
                        .background(CinemaColors.Accent),
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
    showBrowseFooter: Boolean = false,
    onFavoritesClick: (() -> Unit)? = null,
    onRecentlyAddedClick: (() -> Unit)? = null,
    onTopRatedClick: (() -> Unit)? = null,
    onRailExitRight: (() -> Unit)? = null,
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
                onExitRight = onRailExitRight,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(2f),
            )
        }
    }
}
