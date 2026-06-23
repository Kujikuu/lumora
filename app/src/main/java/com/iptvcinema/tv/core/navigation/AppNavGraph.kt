package com.iptvcinema.tv.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
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
import com.iptvcinema.tv.features.details.MovieDetailsScreen
import com.iptvcinema.tv.features.details.SeriesDetailsScreen
import com.iptvcinema.tv.features.home.HomeScreen
import com.iptvcinema.tv.features.livetv.LiveTvScreen
import com.iptvcinema.tv.features.movies.MoviesScreen
import com.iptvcinema.tv.features.mylist.MyListScreen
import com.iptvcinema.tv.features.parental.ParentalControlsScreen
import com.iptvcinema.tv.features.player.PlayerScreen
import com.iptvcinema.tv.features.profiles.ProfileSelectionScreen
import com.iptvcinema.tv.features.profiles.ProfileViewModel
import com.iptvcinema.tv.features.search.SearchScreen
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

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val sessionViewModel: SessionViewModel = hiltViewModel(LocalContext.current as ComponentActivity)

    NavHost(
        navController = navController,
        startDestination = AppRoute.SPLASH,
    ) {
        composable(AppRoute.SPLASH) {
            val viewModel: SplashViewModel = hiltViewModel()
            val destination by viewModel.startupDestination.collectAsState()

            SplashScreen(
                startupDestination = destination,
                onNavigate = { startupDestination ->
                    navController.navigateOnboardingClearingStack(startupDestination.route())
                },
            )
        }

        composable(AppRoute.ACTIVATION) {
            val viewModel: ActivationViewModel = hiltViewModel()

            ActivationScreenWithViewModel(
                viewModel = viewModel,
                onEnterAccount = {
                    viewModel.authenticate { destination ->
                        navController.navigateOnboardingClearingStack(destination.route())
                    }
                },
                onCreateAccount = {
                    viewModel.authenticate { destination ->
                        navController.navigateOnboardingClearingStack(destination.route())
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
                    onDemoMode = {
                        viewModel.saveDemoSource {
                            navController.navigateOnboardingClearingStack(AppRoute.profileSelection())
                        }
                    },
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
            val requirement = if (mode == ProfileSelectionMode.SwitchProfile) {
                SessionRequirement.Ready
            } else {
                SessionRequirement.HasSource
            }
            val viewModel: ProfileViewModel = hiltViewModel()
            val session by viewModel.sessionState.collectAsState()
            val profilesUiState by viewModel.uiState.collectAsState()

            SessionRouteGuard(
                navController = navController,
                requirement = requirement,
                sessionViewModel = sessionViewModel,
            ) {
                ProfileSelectionScreen(
                    mode = mode,
                    currentProfileId = session.currentProfileId,
                    profilesUiState = profilesUiState,
                    onProfileSelected = { profileId ->
                        viewModel.selectProfile(profileId) {
                            when (mode) {
                                ProfileSelectionMode.Onboarding -> {
                                    navController.navigateToMainShell(AppRoute.HOME)
                                }
                                ProfileSelectionMode.SwitchProfile -> {
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    onRetry = viewModel::loadProfiles,
                    onBack = {
                        when (mode) {
                            ProfileSelectionMode.Onboarding -> {
                                if (!navController.popBackStack()) {
                                    navController.navigateOnboardingClearingStack(AppRoute.addSource())
                                }
                            }
                            ProfileSelectionMode.SwitchProfile -> {
                                navController.popBackStack()
                            }
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
            ),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                val channelId = backStackEntry.arguments?.getString("channelId").orEmpty()
                LiveTvScreen(
                    navController = navController,
                    initialChannelId = channelId.takeIf { it.isNotBlank() },
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
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                MoviesScreen(
                    navController = navController,
                    initialFilter = backStackEntry.arguments?.getString("filter").orEmpty(),
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
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                SeriesScreen(
                    navController = navController,
                    initialFilter = backStackEntry.arguments?.getString("filter").orEmpty(),
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
            route = AppRoute.PLAYER,
            arguments = listOf(
                navArgument("contentId") { type = NavType.StringType },
                navArgument("contentType") { type = NavType.StringType },
                navArgument("seriesId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            SessionRouteGuard(navController = navController, requirement = SessionRequirement.Ready) {
                PlayerScreen(
                    contentId = backStackEntry.arguments?.getString("contentId").orEmpty(),
                    contentType = backStackEntry.arguments?.getString("contentType").orEmpty(),
                    seriesId = backStackEntry.arguments?.getString("seriesId"),
                    navController = navController,
                )
            }
        }

        composable(AppRoute.PLAYLIST_MANAGEMENT) {
            val session by sessionViewModel.sessionState.collectAsState()
            val viewModel: SourceViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
            val sourcesUiState by viewModel.uiState.collectAsState()
            val syncMessage by viewModel.syncMessage.collectAsState()
            var showAddSourcePin by remember { mutableStateOf(false) }
            var addSourcePinError by remember { mutableStateOf<String?>(null) }
            val incorrectPinMessage = stringResource(R.string.error_incorrect_pin)

            fun navigateToAddSource() {
                navController.navigate(AppRoute.addSource(AddSourceMode.FromSettings))
            }

            if (showAddSourcePin) {
                com.iptvcinema.tv.features.parental.PinEntryDialog(
                    mode = com.iptvcinema.tv.features.parental.PinEntryMode.Verify,
                    title = stringResource(R.string.pin_enter),
                    errorMessage = addSourcePinError,
                    onDismiss = {
                        showAddSourcePin = false
                        addSourcePinError = null
                    },
                    onPinComplete = { pin ->
                        if (settingsViewModel.verifyParentalPin(pin)) {
                            showAddSourcePin = false
                            addSourcePinError = null
                            navigateToAddSource()
                        } else {
                            addSourcePinError = incorrectPinMessage
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
