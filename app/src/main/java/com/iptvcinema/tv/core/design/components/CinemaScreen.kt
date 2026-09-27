package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
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
private val RailHorizontalPadding = 8.dp
private val RailIconSlotWidth = RailCollapsedWidth - RailHorizontalPadding * 2
private const val ScrimMaxAlpha = 0.6f

private data class RailEntry(
    val navItem: NavItem,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * YouTube-style side navigation. Collapsed it is an icon column; focusing it expands a
 * panel with labels over the content, which dims behind it.
 *
 * Performance: the layout width changes only once per expand or collapse. Everything that
 * moves each frame (panel reveal, pill growth, label fade) reads [expansion] inside draw or
 * graphicsLayer blocks, so the animation never recomposes or relayouts anything.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    expansion: State<Float>,
    onExpandedChange: (Boolean) -> Unit,
    onNavHandoffStart: () -> Unit = {},
    onRailFocusExit: () -> Unit = {},
    onExitRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val layoutExpanded by remember(expanded) {
        derivedStateOf { expanded || expansion.value > 0f }
    }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The rail sits on the right in RTL, so leaving it toward the content is Left there.
    val exitKey = if (isRtl) Key.DirectionLeft else Key.DirectionRight
    val selectedRequester = remember { FocusRequester() }

    val primaryItems = listOf(
        RailEntry(NavItem.Search, Icons.Default.Search, onSearchClick),
        RailEntry(NavItem.Home, Icons.Default.Home) { onNavigate(NavItem.Home) },
        RailEntry(NavItem.Movies, Icons.Default.Movie) { onNavigate(NavItem.Movies) },
        RailEntry(NavItem.Series, Icons.Default.VideoLibrary) { onNavigate(NavItem.Series) },
        RailEntry(NavItem.LiveTv, Icons.Default.LiveTv) { onNavigate(NavItem.LiveTv) },
        RailEntry(NavItem.MyList, Icons.Default.Bookmarks) { onNavigate(NavItem.MyList) },
    )
    val selectedInRail = primaryItems.any { it.navItem == selected }

    fun performNavAction(action: () -> Unit) {
        onNavHandoffStart()
        onExpandedChange(false)
        action()
        onExitRight?.invoke()
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (layoutExpanded) RailExpandedWidth else RailCollapsedWidth)
            .drawBehind { drawRailPanel(expansion.value, RailCollapsedWidth.toPx()) }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == exitKey && onExitRight != null) {
                    onExpandedChange(false)
                    onExitRight()
                    true
                } else {
                    false
                }
            }
            // Entering the rail always lands on the current tab, not the nearest icon.
            .focusProperties {
                enter = { if (selectedInRail) selectedRequester else FocusRequester.Default }
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
            .padding(vertical = 30.dp, horizontal = RailHorizontalPadding),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RailLogo(showFull = layoutExpanded, expansion = expansion)

        Spacer(Modifier.height(36.dp))

        primaryItems.forEach { entry ->
            val isSelected = entry.navItem == selected
            RailItemRow(
                label = stringResource(entry.navItem.labelRes),
                icon = entry.icon,
                selected = isSelected,
                showLabel = layoutExpanded,
                expansion = expansion,
                onClick = { performNavAction(entry.onClick) },
                modifier = if (isSelected) Modifier.focusRequester(selectedRequester) else Modifier,
            )
        }

        Spacer(Modifier.weight(1f))

        RailProfileRow(
            title = navProfileTitle,
            subtitle = navProfileSubtitle,
            selected = selected == NavItem.Profile,
            showLabel = layoutExpanded,
            expansion = expansion,
            onClick = { performNavAction(onProfileClick) },
        )
    }
}

/** Panel background that grows from the collapsed width to the full rail as [progress] goes 0 to 1. */
private fun DrawScope.drawRailPanel(progress: Float, collapsedPx: Float) {
    val panelWidth = collapsedPx + (size.width - collapsedPx).coerceAtLeast(0f) * progress
    if (panelWidth <= 0f) return
    val rtl = layoutDirection == LayoutDirection.Rtl
    val edgeX = if (rtl) size.width else 0f
    val farX = if (rtl) size.width - panelWidth else panelWidth
    drawRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to CinemaColors.Background.copy(alpha = 0.85f + 0.13f * progress),
                0.7f to CinemaColors.Background.copy(alpha = 0.55f + 0.4f * progress),
                1f to CinemaColors.Background.copy(alpha = 0f),
            ),
            startX = edgeX,
            endX = farX,
        ),
        topLeft = Offset(minOf(edgeX, farX), 0f),
        size = Size(panelWidth, size.height),
    )
}

/** Focus pill that grows with the panel so it never pokes out past the background. */
private fun DrawScope.drawFocusPill(progress: Float, collapsedPx: Float, color: Color) {
    val pillWidth = collapsedPx + (size.width - collapsedPx).coerceAtLeast(0f) * progress
    val left = if (layoutDirection == LayoutDirection.Rtl) size.width - pillWidth else 0f
    drawRoundRect(
        color = color,
        topLeft = Offset(left, 0f),
        size = Size(pillWidth, size.height),
        cornerRadius = CornerRadius(size.height / 2f),
    )
}

/** Labels fade in over the second part of the expansion and slide a few dp toward place. */
private fun GraphicsLayerScope.applyLabelReveal(progress: Float, isRtl: Boolean) {
    val labelProgress = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f)
    alpha = labelProgress
    val slide = (1f - labelProgress) * 12.dp.toPx()
    translationX = if (isRtl) slide else -slide
}

@Composable
private fun RailLogo(showFull: Boolean, expansion: State<Float>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .width(RailIconSlotWidth)
                .graphicsLayer { alpha = 1f - expansion.value },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.nav_sidebar_min),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit,
            )
        }
        if (showFull) {
            Image(
                painter = painterResource(R.drawable.nav_sidebar_full),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = expansion.value },
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RailItemRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    showLabel: Boolean,
    expansion: State<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // YouTube pattern: focused item is dark on a white pill; the current tab stays bright.
    val contentColor = when {
        focused -> CinemaColors.Background
        selected -> CinemaColors.White
        else -> CinemaColors.TextMuted
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CinemaSpacing.NavRailItemMinHeight)
            .drawBehind {
                if (focused) drawFocusPill(expansion.value, RailIconSlotWidth.toPx(), CinemaColors.White)
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(RailIconSlotWidth)
                .heightIn(min = CinemaSpacing.NavRailItemMinHeight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(CinemaSpacing.NavRailIconSize),
            )
            if (selected && !focused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .size(width = CinemaSpacing.NavRailActiveIndicatorWidth, height = 3.dp)
                        .background(CinemaColors.Accent, CinemaShapes.Small),
                )
            }
        }
        if (showLabel) {
            Text(
                text = label,
                maxLines = 1,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .graphicsLayer { applyLabelReveal(expansion.value, isRtl) },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RailProfileRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    showLabel: Boolean,
    expansion: State<Float>,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val profileLabel = stringResource(R.string.nav_profile)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CinemaSpacing.NavRailItemMinHeight)
            .drawBehind {
                if (focused || selected) {
                    val color = if (focused) CinemaColors.White else CinemaColors.Surface.copy(alpha = 0.6f)
                    drawFocusPill(expansion.value, RailIconSlotWidth.toPx(), color)
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = profileLabel
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(RailIconSlotWidth),
            contentAlignment = Alignment.Center,
        ) {
            AccountAvatar(size = CinemaSpacing.NavRailProfileAvatarSize)
        }
        if (showLabel) {
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .graphicsLayer { applyLabelReveal(expansion.value, isRtl) },
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (focused) CinemaColors.Background else CinemaColors.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = subtitle,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (focused) CinemaColors.Background.copy(alpha = 0.7f) else CinemaColors.TextMuted,
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
    var railExpanded by remember { mutableStateOf(false) }
    var suppressRailExpansion by remember { mutableStateOf(false) }
    val expansion = animateFloatAsState(
        targetValue = if (railExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = TvScrollMotion.SHELL_MS, easing = FastOutSlowInEasing),
        label = "railExpansion",
    )

    fun setRailExpanded(expanded: Boolean) {
        if (expanded && suppressRailExpansion) return
        railExpanded = expanded
    }

    LaunchedEffect(selectedNavItem) {
        suppressRailExpansion = true
        railExpanded = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
        ) {
            content()
        }

        if (showRail) {
            // Dims the content while the rail is open. Drawn, not composed, per frame.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .drawBehind {
                        val alpha = ScrimMaxAlpha * expansion.value
                        if (alpha > 0f) drawRect(CinemaColors.Background.copy(alpha = alpha))
                    },
            )

            CinemaNavRail(
                selected = selectedNavItem!!,
                onNavigate = onNavigate!!,
                onSearchClick = onSearchClick ?: { onNavigate(NavItem.Search) },
                onSettingsClick = onSettingsClick ?: { onNavigate(NavItem.Settings) },
                onProfileClick = onProfileClick ?: { onNavigate(NavItem.Profile) },
                navProfileTitle = navProfileTitle.orEmpty(),
                navProfileSubtitle = navProfileSubtitle.orEmpty(),
                expanded = railExpanded,
                expansion = expansion,
                onExpandedChange = ::setRailExpanded,
                onNavHandoffStart = { suppressRailExpansion = true },
                onRailFocusExit = { suppressRailExpansion = false },
                onExitRight = onRailExitRight,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(2f),
            )
        }
    }
}
