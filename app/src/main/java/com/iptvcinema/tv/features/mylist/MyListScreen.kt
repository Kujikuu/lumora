package com.iptvcinema.tv.features.mylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaContextMenuDialog
import com.iptvcinema.tv.core.design.components.CinemaMenuOption
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.ContinueWatchingMenuDialog
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.components.SkeletonHomeContent
import com.iptvcinema.tv.core.design.components.shellHeroContentStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.navigateMainShellTab
import com.iptvcinema.tv.core.navigation.openContinueWatchingDetails
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.features.browse.ImmersiveBrowse
import com.iptvcinema.tv.features.browse.openBrowseCard
import com.iptvcinema.tv.features.browse.openBrowseCardDetails
import com.iptvcinema.tv.features.browse.playBrowseCard
import com.iptvcinema.tv.features.browse.rememberImmersiveBrowseState
import com.iptvcinema.tv.features.home.HomeSection

/**
 * My List in the Home layout: saved movies, series, episodes and channels, then Continue
 * Watching, with the spotlight and backdrop following the focused card. Long press opens a menu
 * (play, details, remove) instead of removing straight away.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyListScreen(
    navController: NavController,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusState = rememberScreenFocusState("my_list")
    val browse = rememberImmersiveBrowseState(focusState)
    val lifecycleOwner = LocalLifecycleOwner.current
    var menuCard by remember { mutableStateOf<HomeContentCard?>(null) }
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
        selectedNavItem = NavItem.MyList,
        onRailExitRight = if (uiState.loadState == MyListLoadState.Ready) browse::enter else null,
    ) {
        MyListMenuDialog(
            card = menuCard,
            onDismiss = { menuCard = null },
            onPlay = { card -> playBrowseCard(navController, card) },
            onDetails = { card -> openBrowseCardDetails(navController, card) },
            onRemove = { card ->
                viewModel.removeFavorite(card)
                // The removed card had focus; put it back on the rails.
                browse.recoverFocus()
            },
        )
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
        when (uiState.loadState) {
            MyListLoadState.Loading -> Column(
                modifier = Modifier.padding(top = CinemaSpacing.ScreenPaddingVertical),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                CinemaSerifTitle(text = stringResource(R.string.mylist_title))
                SkeletonHomeContent()
            }
            MyListLoadState.Error -> ErrorState(
                title = stringResource(R.string.error_load_list),
                description = uiState.errorMessage ?: stringResource(R.string.mylist_error_load),
                errorCode = null,
                onRetry = viewModel::retry,
                onSwitchStream = {},
                onBack = { navController.navigateMainShellTab(AppRoute.HOME) },
                showSwitchStream = false,
                backLabel = stringResource(R.string.btn_back),
            )
            MyListLoadState.Empty -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = CinemaSpacing.ScreenPaddingVertical),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                CinemaSerifTitle(text = stringResource(R.string.mylist_title))
                EmptyState(
                    title = stringResource(R.string.mylist_nothing_saved),
                    description = stringResource(R.string.mylist_nothing_saved_desc),
                    primaryAction = stringResource(R.string.nav_movies),
                    secondaryAction = stringResource(R.string.nav_live_tv),
                    onPrimary = { navController.navigateMainShellTab(AppRoute.movies()) },
                    onSecondary = { navController.navigateMainShellTab(AppRoute.liveTv()) },
                    footerNote = stringResource(R.string.mylist_remove_tip),
                )
            }
            MyListLoadState.Ready -> ImmersiveBrowse(
                state = browse,
                sections = uiState.sections,
                isReady = true,
                onCardClick = { section, card -> openBrowseCard(navController, section, card) },
                onCardLongClick = { section, card ->
                    if (section is HomeSection.ContinueWatching) continueMenuCard = card else menuCard = card
                },
                overlay = {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = shellHeroContentStart(), top = CinemaSpacing.ScreenPaddingVertical),
                    ) {
                        Text(
                            text = stringResource(R.string.mylist_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = CinemaColors.TextSecondary,
                            ),
                        )
                    }
                },
            )
        }
    }
}

private object MyListMenuOptionId {
    const val PLAY = "play"
    const val DETAILS = "details"
    const val REMOVE = "remove"
}

@Composable
private fun MyListMenuDialog(
    card: HomeContentCard?,
    onDismiss: () -> Unit,
    onPlay: (HomeContentCard) -> Unit,
    onDetails: (HomeContentCard) -> Unit,
    onRemove: (HomeContentCard) -> Unit,
) {
    if (card == null) return
    val options = buildList {
        when (card.contentType) {
            "movie", "episode" -> add(CinemaMenuOption(MyListMenuOptionId.PLAY, stringResource(R.string.mylist_menu_play)))
            "channel" -> add(CinemaMenuOption(MyListMenuOptionId.PLAY, stringResource(R.string.mylist_menu_watch_live)))
        }
        if (card.contentType != "channel") {
            add(CinemaMenuOption(MyListMenuOptionId.DETAILS, stringResource(R.string.mylist_menu_details)))
        }
        add(CinemaMenuOption(MyListMenuOptionId.REMOVE, stringResource(R.string.mylist_menu_remove), destructive = true))
    }
    CinemaContextMenuDialog(
        title = card.title,
        options = options,
        onDismiss = onDismiss,
        onOptionSelected = { option ->
            when (option.id) {
                MyListMenuOptionId.PLAY -> if (card.contentType == "channel") onDetails(card) else onPlay(card)
                MyListMenuOptionId.DETAILS -> onDetails(card)
                MyListMenuOptionId.REMOVE -> onRemove(card)
            }
            onDismiss()
        },
    )
}
