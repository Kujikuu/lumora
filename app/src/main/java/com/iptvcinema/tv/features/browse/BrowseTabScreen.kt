package com.iptvcinema.tv.features.browse

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.CatalogRefreshBanner
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.ContinueWatchingMenuDialog
import com.iptvcinema.tv.core.design.components.shellHeroContentStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.openContinueWatchingDetails
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.features.home.HomeSection

/**
 * A Home-style tab: the shared immersive layout over the rails a [BrowseTabViewModel] builds.
 * Long press opens the Continue Watching menu on those cards and toggles My List elsewhere.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowseTabScreen(
    navController: NavController,
    navItem: NavItem,
    focusKey: String,
    title: String,
    emptyTitle: String,
    emptyDescription: String,
    viewModel: BrowseTabViewModel<*, *>,
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusState = rememberScreenFocusState(focusKey)
    val browse = rememberImmersiveBrowseState(focusState)
    val catalogCallbacks = rememberCatalogStateCallbacks(navController, onRetry = viewModel::refreshCurrentSource)
    val lifecycleOwner = LocalLifecycleOwner.current
    var continueMenuCard by remember { mutableStateOf<HomeContentCard?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshContinueWatching()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MainShellBackHandler(navController = navController, isHomeTab = false)

    MainShellScaffold(
        navController = navController,
        selectedNavItem = navItem,
        // Only when the rails are shown; otherwise Right must reach the state screen's buttons.
        onRailExitRight = if (uiState.loadState == CatalogLoadState.Ready) browse::enter else null,
    ) {
        ContinueWatchingMenuDialog(
            card = continueMenuCard,
            onDismiss = { continueMenuCard = null },
            onResume = { card -> playBrowseCard(navController, card) },
            onViewDetails = { card -> openContinueWatchingDetails(navController, card) },
            onRemove = { card ->
                viewModel.removeContinueWatching(card)
                // The removed card had focus; put it back on the rails.
                browse.recoverFocus()
            },
        )
        CatalogStateContent(
            loadState = uiState.loadState,
            message = uiState.message,
            sourceStatus = uiState.sourceStatus,
            sourceType = uiState.sourceType,
            skeletonStyle = CatalogSkeletonStyle.Home,
            emptyTitle = emptyTitle,
            emptyDescription = emptyDescription,
            onAddSource = catalogCallbacks.onAddSource,
            onRetry = catalogCallbacks.onRetry,
            onManageSources = catalogCallbacks.onManageSources,
            onEditSource = catalogCallbacks.onEditSource,
            onRefreshCatalog = viewModel::refreshCurrentSource,
        ) {
            ImmersiveBrowse(
                state = browse,
                sections = uiState.sections,
                isReady = uiState.loadState == CatalogLoadState.Ready,
                onCardClick = { section, card -> openBrowseCard(navController, section, card) },
                onCardLongClick = { section, card ->
                    when (section) {
                        is HomeSection.ContinueWatching -> continueMenuCard = card
                        is HomeSection.Categories -> openBrowseCardDetails(navController, card)
                        else -> viewModel.toggleFavorite(card)
                    }
                },
                overlay = {
                    Text(
                        text = title,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = shellHeroContentStart(), top = CinemaSpacing.ScreenPaddingVertical),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CinemaColors.TextSecondary,
                        ),
                    )
                    CatalogRefreshBanner(
                        syncBannerText = uiState.syncBannerText,
                        refreshState = uiState.refreshState,
                        onRefresh = viewModel::refreshCurrentSource,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = CinemaSpacing.ScreenPaddingVertical, end = CinemaSpacing.ScreenPadding),
                    )
                },
            )
        }
    }
}
