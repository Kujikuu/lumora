package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

private val RailCollapsedWidth = CinemaSpacing.NavRailWidth
private val RailExpandedWidth = CinemaSpacing.NavRailExpandedWidth
private val RailIconSlotWidth = CinemaSpacing.NavRailIconSlotWidth

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
    navProfileTitle: String,
    navProfileSubtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNavHandoffStart: () -> Unit = {},
    onRailFocusExit: () -> Unit = {},
    onExitRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(
        targetValue = if (expanded) RailExpandedWidth else RailCollapsedWidth,
        animationSpec = tween(durationMillis = TvScrollMotion.SHELL_MS),
        label = "railWidth",
    )

    val primaryItems = listOf(
        RailEntry(NavItem.Search, Icons.Default.Search, onSearchClick),
        RailEntry(NavItem.Home, Icons.Default.Home) { onNavigate(NavItem.Home) },
        RailEntry(NavItem.Movies, Icons.Default.Movie) { onNavigate(NavItem.Movies) },
        RailEntry(NavItem.Series, Icons.Default.VideoLibrary) { onNavigate(NavItem.Series) },
        RailEntry(NavItem.LiveTv, Icons.Default.LiveTv) { onNavigate(NavItem.LiveTv) },
        RailEntry(NavItem.MyList, Icons.Default.Bookmarks) { onNavigate(NavItem.MyList) },
    )

    fun performNavAction(action: () -> Unit) {
        onNavHandoffStart()
        onExpandedChange(false)
        action()
        onExitRight?.invoke()
    }

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
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus) {
                    onRailFocusExit()
                    onExpandedChange(false)
                } else {
                    onExpandedChange(true)
                }
            }
            .padding(vertical = 30.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (expanded) {
                Image(
                    painter = painterResource(R.drawable.nav_sidebar_full),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.nav_sidebar_min),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        primaryItems.forEach { entry ->
            RailItemRow(
                label = stringResource(entry.navItem.labelRes),
                icon = entry.icon,
                selected = entry.navItem == selected,
                expanded = expanded,
                onClick = { performNavAction(entry.onClick) },
            )
        }

        Spacer(Modifier.weight(1f))

        FocusableCinemaCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CinemaSpacing.NavRailItemMinHeight),
            onClick = { performNavAction(onProfileClick) },
            shape = CinemaShapes.XLarge,
            focusedBorderWidth = 0.dp,
            contentDescription = stringResource(R.string.nav_profile),
        ) { focused ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (focused || selected == NavItem.Profile) {
                            CinemaColors.Surface.copy(alpha = 0.45f)
                        } else {
                            CinemaColors.Background.copy(alpha = 0f)
                        },
                        CinemaShapes.XLarge,
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.width(RailIconSlotWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    AccountAvatar(size = CinemaSpacing.NavRailProfileAvatarSize)
                }
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(end = 12.dp)) {
                        Text(
                            text = navProfileTitle,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CinemaColors.White,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Text(
                            text = navProfileSubtitle,
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
        val isActive = selected || focused
        val contentColor = if (isActive) CinemaColors.White else CinemaColors.TextMuted
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CinemaSpacing.NavRailItemMinHeight)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(RailIconSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(CinemaSpacing.NavRailIconSize),
                    )
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(
                                    width = CinemaSpacing.NavRailActiveIndicatorWidth,
                                    height = 3.dp,
                                )
                                .background(CinemaColors.Accent),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(150)) + expandHorizontally(tween(180)),
                exit = fadeOut(tween(100)) + shrinkHorizontally(tween(140)),
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
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
    navProfileTitle: String? = null,
    navProfileSubtitle: String? = null,
    showBrowseFooter: Boolean = false,
    onFavoritesClick: (() -> Unit)? = null,
    onRecentlyAddedClick: (() -> Unit)? = null,
    onTopRatedClick: (() -> Unit)? = null,
    onRailExitRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val showRail = showTopNav && selectedNavItem != null && onNavigate != null
    val shellImmersion = remember { ShellImmersionState() }
    var railExpanded by remember { mutableStateOf(false) }
    var suppressRailExpansion by remember { mutableStateOf(false) }
    val hideNavRail = shellImmersion.hideNavRail
    val scrimAlpha by animateFloatAsState(
        targetValue = if (railExpanded && !hideNavRail) 0.4f else 0f,
        animationSpec = tween(durationMillis = TvScrollMotion.SHELL_MS),
        label = "railScrim",
    )

    fun setRailExpanded(expanded: Boolean) {
        if (expanded && suppressRailExpansion) return
        railExpanded = expanded
        if (expanded) shellImmersion.showNavRail()
    }

    LaunchedEffect(selectedNavItem) {
        suppressRailExpansion = true
        railExpanded = false
        shellImmersion.showNavRail()
    }

    CompositionLocalProvider(LocalShellImmersion provides shellImmersion) {
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

                AnimatedVisibility(
                    visible = !hideNavRail,
                    enter = fadeIn(tween(TvScrollMotion.SHELL_MS)) + expandHorizontally(tween(TvScrollMotion.SHELL_MS), expandFrom = Alignment.Start),
                    exit = fadeOut(tween(TvScrollMotion.SHELL_MS - 40)) + shrinkHorizontally(tween(TvScrollMotion.SHELL_MS - 20), shrinkTowards = Alignment.Start),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .zIndex(2f),
                ) {
                    CinemaNavRail(
                        selected = selectedNavItem!!,
                        onNavigate = onNavigate!!,
                        onSearchClick = onSearchClick ?: { onNavigate(NavItem.Search) },
                        onSettingsClick = onSettingsClick ?: { onNavigate(NavItem.Settings) },
                        onProfileClick = onProfileClick ?: { onNavigate(NavItem.Profile) },
                        navProfileTitle = navProfileTitle.orEmpty(),
                        navProfileSubtitle = navProfileSubtitle.orEmpty(),
                        expanded = railExpanded,
                        onExpandedChange = ::setRailExpanded,
                        onNavHandoffStart = { suppressRailExpansion = true },
                        onRailFocusExit = { suppressRailExpansion = false },
                        onExitRight = onRailExitRight,
                    )
                }
            }
        }
    }
}
