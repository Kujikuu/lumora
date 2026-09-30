package com.iptvcinema.tv.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.datastore.SessionRequirement
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.datastore.route
import com.iptvcinema.tv.features.activation.ActivationScreenWithViewModel
import com.iptvcinema.tv.features.activation.ActivationViewModel
import com.iptvcinema.tv.features.details.ChannelDetailsScreen
import com.iptvcinema.tv.features.details.MovieDetailsScreen
import com.iptvcinema.tv.features.details.RelatedKind
import com.iptvcinema.tv.features.details.RelatedScreen
import com.iptvcinema.tv.features.details.SeriesDetailsScreen
import com.iptvcinema.tv.features.details.SeriesEpisodesScreen
import com.iptvcinema.tv.features.home.HomeScreen
import com.iptvcinema.tv.features.livetv.LiveTvScreen
import com.iptvcinema.tv.features.movies.MoviesBrowseScreen
import com.iptvcinema.tv.features.movies.MoviesScreen
import com.iptvcinema.tv.features.mylist.MyListScreen
import com.iptvcinema.tv.features.parental.ParentalControlsScreen
import com.iptvcinema.tv.features.player.PlayerScreen
import com.iptvcinema.tv.core.parental.PinCheck
import com.iptvcinema.tv.features.parental.messageOrNull
import com.iptvcinema.tv.features.profiles.ProfileEditorActions
import com.iptvcinema.tv.features.profiles.ProfileEditorDialog
import com.iptvcinema.tv.features.profiles.ProfileScreenActions
import com.iptvcinema.tv.features.profiles.ProfileSelectionScreen
import com.iptvcinema.tv.features.profiles.ProfileViewModel
import com.iptvcinema.tv.features.search.SearchScreen
import com.iptvcinema.tv.features.series.SeriesBrowseScreen
import com.iptvcinema.tv.features.series.SeriesScreen
import com.iptvcinema.tv.features.settings.SettingsScreen
import com.iptvcinema.tv.features.settings.SettingsViewModel
import com.iptvcinema.tv.features.sources.AddSourceScreen
import com.iptvcinema.tv.features.sources.M3uFormScreen
import com.iptvcinema.tv.features.sources.PlaylistManagementScreen
import com.iptvcinema.tv.features.sources.SourceViewModel
import com.iptvcinema.tv.features.sources.XtreamFormScreen
import com.iptvcinema.tv.features.splash.SplashScreen
import com.iptvcinema.tv.features.splash.SplashViewModel
import com.iptvcinema.tv.features.states.EmptyStateScreen
import com.iptvcinema.tv.features.states.ErrorStateScreen
import com.iptvcinema.tv.features.states.ExpiredAccountScreen
import com.iptvcinema.tv.features.states.InvalidPlaylistScreen
import com.iptvcinema.tv.features.welcome.WelcomeScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val activity = LocalActivity.current as ComponentActivity
    val sessionViewModel: SessionViewModel = hiltViewModel(activity)

    PendingDeepLinkHandler(navController = navController, sessionViewModel = sessionViewModel)

    // The default NavHost crossfade is 700 ms, which feels sluggish on a remote.
    NavHost(
        navController = navController,
        startDestination = AppRoute.SPLASH,
        enterTransition = { fadeIn(tween(NAV_ENTER_MS)) + scaleIn(tween(NAV_ENTER_MS), initialScale = 0.98f) },
        exitTransition = { fadeOut(tween(NAV_EXIT_MS)) },
        popEnterTransition = { fadeIn(tween(NAV_ENTER_MS)) },
        popExitTransition = { fadeOut(tween(NAV_EXIT_MS)) + scaleOut(tween(NAV_EXIT_MS), targetScale = 0.98f) },
    ) {
        composable(AppRoute.SPLASH) {
            val viewModel: SplashViewModel = hiltViewModel()
            val destination by viewModel.startupDestination.collectAsState()

            BlockBackHandler()

            SplashScreen(
                isReady = destination != null,
                onFinished = {
                    destination?.let { startupDestination ->
                        navController.navigateOnboardingClearingStack(startupDestination.route())
                    }
                },
            )
        }

        composable(AppRoute.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(AppRoute.ACTIVATION)
                },
            )
        }

        composable(AppRoute.ACTIVATION) {
            val viewModel: ActivationViewModel = hiltViewModel()

            ActivationScreenWithViewModel(
                viewModel = viewModel,
                onSignedIn = { destination ->
                    navController.navigateOnboardingClearingStack(destination.route())
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigateOnboardingClearingStack(AppRoute.WELCOME)
                    }
                },
            )
        }

        composable(
            route = AppRoute.ADD_SOURCE,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = AddSourceMode.Onboarding.name
                },
            ),
        ) { backStackEntry ->
            val mode = runCatching {
                AddSourceMode.valueOf(
                    backStackEntry.arguments?.getString("mode") ?: AddSourceMode.Onboarding.name,
                )
            }.getOrDefault(AddSourceMode.Onboarding)
            val requirement = if (mode == AddSourceMode.FromSettings) {
                SessionRequirement.Ready
            } else {
                SessionRequirement.Authenticated
            }
            val viewModel: SourceViewModel = hiltViewModel()

            SessionRouteGuard(
                navController = navController,
                requirement = requirement,
                sessionViewModel = sessionViewModel,
            ) {
                AddSourceScreen(
                    allowBack = mode == AddSourceMode.FromSettings,
                    onBack = { navController.popBackStack() },
                    onXtream = { navController.navigate(AppRoute.XTREAM_FORM) },
                    onM3u = { navController.navigate(AppRoute.M3U_FORM) },
                )
            }
        }

        composable(AppRoute.XTREAM_FORM) {
            val viewModel: SourceViewModel = hiltViewModel()
            val connectState by viewModel.xtreamConnectState.collectAsState()

            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Authenticated) {
                XtreamFormScreen(
                    connectState = connectState,
                    onConnect = { credentials ->
                        viewModel.connectXtreamSource(credentials) {
                            navController.navigateOnboardingClearingStack(AppRoute.profileSelection())
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(AppRoute.M3U_FORM) {
            val viewModel: SourceViewModel = hiltViewModel()
            val connectState by viewModel.m3uConnectState.collectAsState()

            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Authenticated) {
                M3uFormScreen(
                    connectState = connectState,
                    onImport = { credentials ->
                        viewModel.saveM3uSource(credentials) {
                            navController.navigateOnboardingClearingStack(AppRoute.profileSelection())
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = AppRoute.PROFILE_SELECTION,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = ProfileSelectionMode.Onboarding.name
                },
            ),
        ) { backStackEntry ->
            val mode = runCatching {
                ProfileSelectionMode.valueOf(
                    backStackEntry.arguments?.getString("mode") ?: ProfileSelectionMode.Onboarding.name,
                )
            }.getOrDefault(ProfileSelectionMode.Onboarding)
            val requirement = if (mode == ProfileSelectionMode.Onboarding) {
                SessionRequirement.HasSource
            } else {
                SessionRequirement.Ready
            }
            val viewModel: ProfileViewModel = hiltViewModel()
            val session by viewModel.sessionState.collectAsState()
            val profilesUiState by viewModel.uiState.collectAsState()

            SessionRouteGuard(
                navController = navController,
                requirement = requirement,
                sessionViewModel = sessionViewModel,
            ) {
                val editor by viewModel.editor.collectAsState()
                ProfileSelectionScreen(
                    mode = mode,
                    currentProfileId = session.currentProfileId,
                    profilesUiState = profilesUiState,
                    canAddProfile = viewModel.canAddProfile(),
                    actions = ProfileScreenActions(
                        onProfileSelected = { profileId ->
                            viewModel.selectProfile(profileId) {
                                when (mode) {
                                    ProfileSelectionMode.Onboarding -> navController.navigateToMainShell(AppRoute.HOME)
                                    ProfileSelectionMode.SwitchProfile,
                                    ProfileSelectionMode.Manage,
                                    -> navController.popBackStack()
                                }
                            }
                        },
                        onEditProfile = viewModel::startEdit,
                        onAddProfile = viewModel::startCreate,
                        onRetry = viewModel::loadProfiles,
                        onBack = {
                            if (!navController.popBackStack() && mode == ProfileSelectionMode.Onboarding) {
                                navController.navigateOnboardingClearingStack(AppRoute.addSource())
                            }
                        },
                    ),
                    editorContent = {
                        editor?.let { state ->
                            ProfileEditorDialog(
                                state = state,
                                actions = ProfileEditorActions(
                                    onNameChange = viewModel::updateEditorName,
                                    onTypeChange = viewModel::updateEditorType,
                                    onSave = viewModel::saveEditor,
                                    onDelete = viewModel::deleteEditingProfile,
                                    onDismiss = viewModel::dismissEditor,
                                ),
                            )
                        }
                    },
                )
            }
        }

        composable(AppRoute.HOME) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                HomeScreen(navController = navController)
            }
        }

        composable(
            route = AppRoute.LIVE_TV,
            arguments = listOf(
                navArgument("channelId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("openGuide") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                val channelId = backStackEntry.arguments?.getString("channelId").orEmpty()
                val openGuide = backStackEntry.arguments?.getBoolean("openGuide") ?: false
                LiveTvScreen(
                    navController = navController,
                    initialChannelId = channelId.takeIf { it.isNotBlank() },
                    initialOpenGuide = openGuide,
                )
            }
        }

        composable(
            route = AppRoute.MOVIES,
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                MoviesBrowseScreen(navController = navController)
            }
        }

        composable(
            route = AppRoute.MOVIE_CATALOG,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                MoviesScreen(
                    navController = navController,
                    initialFilter = backStackEntry.arguments?.getString("category").orEmpty(),
                )
            }
        }

        composable(
            route = AppRoute.SERIES,
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                SeriesBrowseScreen(navController = navController)
            }
        }

        composable(
            route = AppRoute.SERIES_CATALOG,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                SeriesScreen(
                    navController = navController,
                    initialFilter = backStackEntry.arguments?.getString("category").orEmpty(),
                )
            }
        }

        composable(AppRoute.SEARCH) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                SearchScreen(navController = navController)
            }
        }

        composable(AppRoute.MY_LIST) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                MyListScreen(navController = navController)
            }
        }

        composable(AppRoute.SETTINGS) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                SettingsScreen(navController = navController)
            }
        }

        composable(
            route = AppRoute.MOVIE_DETAILS,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                MovieDetailsScreen(
                    movieId = backStackEntry.arguments?.getString("movieId").orEmpty(),
                    navController = navController,
                )
            }
        }

        composable(
            route = AppRoute.MOVIE_RELATED,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                RelatedScreen(
                    kind = RelatedKind.Movie,
                    contentId = backStackEntry.arguments?.getString("movieId").orEmpty(),
                    navController = navController,
                )
            }
        }

        composable(
            route = AppRoute.SERIES_RELATED,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType }),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                RelatedScreen(
                    kind = RelatedKind.Series,
                    contentId = backStackEntry.arguments?.getString("seriesId").orEmpty(),
                    navController = navController,
                )
            }
        }

        composable(
            route = AppRoute.SERIES_DETAILS,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType }),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                SeriesDetailsScreen(
                    seriesId = backStackEntry.arguments?.getString("seriesId").orEmpty(),
                    navController = navController,
                )
            }
        }

        composable(
            route = AppRoute.SERIES_EPISODES,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType }),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                SeriesEpisodesScreen(
                    seriesId = backStackEntry.arguments?.getString("seriesId").orEmpty(),
                    navController = navController,
                )
            }
        }

        composable(
            route = AppRoute.CHANNEL_DETAILS,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                ChannelDetailsScreen(
                    channelId = backStackEntry.arguments?.getString("channelId").orEmpty(),
                    navController = navController,
                )
            }
        }

        composable(
            route = AppRoute.PLAYER,
            arguments = listOf(
                navArgument("contentId") { type = NavType.StringType },
                navArgument("contentType") { type = NavType.StringType },
                navArgument("seriesId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("resumePositionMs") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("startEpochMs") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("endEpochMs") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                PlayerScreen(
                    contentId = backStackEntry.arguments?.getString("contentId").orEmpty(),
                    contentType = backStackEntry.arguments?.getString("contentType").orEmpty(),
                    seriesId = backStackEntry.arguments?.getString("seriesId"),
                    resumePositionMs = backStackEntry.arguments?.getLong("resumePositionMs")?.takeIf { it >= 0L },
                    navController = navController,
                )
            }
        }

        composable(AppRoute.PLAYLIST_MANAGEMENT) {
            val session by sessionViewModel.sessionState.collectAsState()
            val viewModel: SourceViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
            val sourcesUiState by viewModel.uiState.collectAsState()
            val syncMessage by viewModel.syncMessage.collectAsState()
            var showAddSourcePin by remember { mutableStateOf(false) }
            var addSourcePinCheck by remember { mutableStateOf<PinCheck?>(null) }

            fun navigateToAddSource() {
                navController.navigate(AppRoute.addSource(AddSourceMode.FromSettings))
            }

            if (showAddSourcePin) {
                com.iptvcinema.tv.features.parental.PinEntryDialog(
                    mode = com.iptvcinema.tv.features.parental.PinEntryMode.Verify,
                    title = stringResource(R.string.pin_enter),
                    errorMessage = addSourcePinCheck?.messageOrNull(),
                    onDismiss = {
                        showAddSourcePin = false
                        addSourcePinCheck = null
                    },
                    onPinComplete = { pin ->
                        val check = settingsViewModel.checkParentalPin(pin)
                        if (check == PinCheck.Accepted) {
                            showAddSourcePin = false
                            addSourcePinCheck = null
                            navigateToAddSource()
                        } else {
                            addSourcePinCheck = check
                        }
                    },
                )
            }

            SessionRouteGuard(
                navController = navController,
                requirement = SessionRequirement.Ready,
                sessionViewModel = sessionViewModel,
            ) {
                PlaylistManagementScreen(
                    currentSourceId = session.currentSourceId,
                    sourceType = session.sourceType,
                    isDemoMode = session.isDemoMode,
                    sourcesUiState = sourcesUiState,
                    syncMessage = syncMessage,
                    onLoadSources = viewModel::loadSources,
                    onAddSource = {
                        if (settingsViewModel.requiresPlaylistPin()) {
                            showAddSourcePin = true
                        } else {
                            navigateToAddSource()
                        }
                    },
                    onSetActive = viewModel::setActiveSource,
                    onResyncSource = viewModel::resyncSource,
                    onDeleteSource = viewModel::deleteSource,
                    onEditSource = { source ->
                        when (source.type) {
                            com.iptvcinema.tv.core.model.SourceType.XTREAM_CODES ->
                                navController.navigate(AppRoute.XTREAM_FORM)
                            com.iptvcinema.tv.core.model.SourceType.M3U ->
                                navController.navigate(AppRoute.M3U_FORM)
                            else -> Unit
                        }
                    },
                    onExpiredAccount = { navController.navigate(AppRoute.EXPIRED_ACCOUNT) },
                    onInvalidPlaylist = { navController.navigate(AppRoute.INVALID_PLAYLIST) },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(AppRoute.PARENTAL_CONTROLS) {
            val viewModel: com.iptvcinema.tv.features.parental.ParentalControlsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                ParentalControlsScreen(
                    navController = navController,
                    uiState = uiState,
                    onSelectProfile = viewModel::selectProfile,
                    onUpdateControls = viewModel::updateControls,
                    onRetry = viewModel::loadProfiles,
                    onBeginSetPin = viewModel::beginSetPin,
                    onPinEntered = viewModel::onPinEntered,
                    onClearPin = viewModel::clearPin,
                )
            }
        }

        composable(AppRoute.EMPTY_STATE) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                EmptyStateScreen(navController = navController)
            }
        }

        composable(AppRoute.ERROR_STATE) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                ErrorStateScreen(navController = navController)
            }
        }

        composable(AppRoute.EXPIRED_ACCOUNT) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                ExpiredAccountScreen(navController = navController)
            }
        }

        composable(AppRoute.INVALID_PLAYLIST) {
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                InvalidPlaylistScreen(navController = navController)
            }
        }
    }
}

private const val NAV_ENTER_MS = 180
private const val NAV_EXIT_MS = 120
