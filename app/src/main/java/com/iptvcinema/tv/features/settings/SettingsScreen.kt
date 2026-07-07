package com.iptvcinema.tv.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.AccountAvatar
import com.iptvcinema.tv.core.design.components.AccountSummaryCard
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.SettingsMenu
import com.iptvcinema.tv.core.design.components.SettingsRow
import com.iptvcinema.tv.core.design.components.SettingsToggle
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.datastore.connectedSourceLabel
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.navigateOnboardingClearingStack
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.platform.AppLocaleHelper

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    initialSection: Int = 0,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var selectedSection by remember { mutableIntStateOf(initialSection) }
    var showDevPreviews by remember { mutableStateOf(false) }
    val menuFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("settings")
    val session by viewModel.sessionState.collectAsState()
    val account by viewModel.accountSummary.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()
    val connectedSource = session.connectedSourceLabel()
    val autoplayNext = userSettings?.autoplayNextEpisode ?: true
    val continueWatching = userSettings?.continueWatchingEnabled ?: true
    val skipIntro = userSettings?.skipIntroEnabled ?: false
    var pendingProtectedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPlaylistPin by remember { mutableStateOf(false) }
    var playlistPinError by remember { mutableStateOf<String?>(null) }
    val sections = SettingsSection.menuSections
    val sectionLabels = sections.map { stringResource(it.labelRes) }
    val inPlayerLabel = stringResource(R.string.settings_in_player)
    val incorrectPinMessage = stringResource(R.string.error_incorrect_pin)
    val refreshTrailing = when (val state = refreshState) {
        CatalogRefreshState.Idle -> null
        is CatalogRefreshState.Refreshing -> {
            if (state.stepLabel.isNotBlank()) {
                "${stringResource(R.string.refresh_in_progress)} ${state.stepLabel}"
            } else {
                stringResource(R.string.refresh_in_progress)
            }
        }
        is CatalogRefreshState.Success -> state.message
        is CatalogRefreshState.Failed -> state.message
    }

    fun runProtected(action: () -> Unit) {
        if (viewModel.requiresPlaylistPin()) {
            pendingProtectedAction = action
            showPlaylistPin = true
        } else {
            action()
        }
    }

    if (showPlaylistPin) {
        com.iptvcinema.tv.features.parental.PinEntryDialog(
            mode = com.iptvcinema.tv.features.parental.PinEntryMode.Verify,
            title = stringResource(R.string.pin_enter),
            errorMessage = playlistPinError,
            onDismiss = {
                showPlaylistPin = false
                playlistPinError = null
                pendingProtectedAction = null
            },
            onPinComplete = { pin ->
                if (viewModel.verifyParentalPin(pin)) {
                    showPlaylistPin = false
                    playlistPinError = null
                    pendingProtectedAction?.invoke()
                    pendingProtectedAction = null
                } else {
                    playlistPinError = incorrectPinMessage
                }
            },
        )
    }

    MainShellBackHandler(navController = navController, isHomeTab = false)

    LaunchedEffect(focusState.hasSavedFocus) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(menuFocus)
        } else {
            focusState.requestInitialFocus(menuFocus)
            focusState.saveFocusIndex(0)
        }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Settings,
    ) {
        val displayAccount = account ?: FakeDataProvider.accountSummary
        val currentSection = sections[selectedSection]

        if (currentSection == SettingsSection.Account) {
            AccountOverviewSettingsContent(
                accountName = displayAccount.name,
                firstItemFocusRequester = menuFocus,
                onOpenSettings = {
                    selectedSection = sections.indexOf(SettingsSection.Playback).coerceAtLeast(0)
                    focusState.saveFocusIndex(selectedSection)
                },
                onSupport = {
                    selectedSection = sections.indexOf(SettingsSection.About).coerceAtLeast(0)
                    focusState.saveFocusIndex(selectedSection)
                },
                onAgreement = {
                    selectedSection = sections.indexOf(SettingsSection.Subscription).coerceAtLeast(0)
                    focusState.saveFocusIndex(selectedSection)
                },
                onSignOut = {
                    viewModel.signOut {
                        navController.navigateOnboardingClearingStack(AppRoute.WELCOME)
                    }
                },
            )
        } else {
            DetailSettingsContent(
                currentSection = currentSection,
                firstItemFocusRequester = menuFocus,
                connectedSource = connectedSource,
                refreshTrailing = refreshTrailing,
                refreshEnabled = refreshState !is CatalogRefreshState.Refreshing,
                showDevPreviews = showDevPreviews,
                autoplayNext = autoplayNext,
                continueWatching = continueWatching,
                skipIntro = skipIntro,
                onOpenLanguage = {
                    selectedSection = sections.indexOf(SettingsSection.Language).coerceAtLeast(0)
                    focusState.saveFocusIndex(selectedSection)
                },
                onRefreshCatalog = {
                    if (refreshState !is CatalogRefreshState.Refreshing) {
                        runProtected { viewModel.refreshCurrentSource() }
                    }
                },
                onOpenPlaylistSources = { runProtected { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) } },
                onOpenTvGuide = { navController.navigate(AppRoute.liveTv(openGuide = true)) },
                onOpenParentalControls = { navController.navigate(AppRoute.PARENTAL_CONTROLS) },
                onToggleDevPreviews = { showDevPreviews = !showDevPreviews },
                onOpenEmptyPreview = { navController.navigate(AppRoute.EMPTY_STATE) },
                onOpenErrorPreview = { navController.navigate(AppRoute.ERROR_STATE) },
                onToggleAutoplay = { viewModel.updateAutoplayNextEpisode(!autoplayNext) },
                onToggleContinueWatching = { viewModel.updateContinueWatching(!continueWatching) },
                onToggleSkipIntro = { viewModel.updateSkipIntro(!skipIntro) },
                onSignOut = {
                    viewModel.signOut {
                        navController.navigateOnboardingClearingStack(AppRoute.WELCOME)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccountOverviewSettingsContent(
    accountName: String,
    firstItemFocusRequester: FocusRequester,
    onOpenSettings: () -> Unit,
    onSupport: () -> Unit,
    onAgreement: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = CinemaSpacing.ContentStart, end = 90.dp, top = 128.dp, bottom = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        AccountAvatar(size = 240.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = accountName,
                style = MaterialTheme.typography.displaySmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = stringResource(R.string.settings_active),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = CinemaColors.Success,
                    fontWeight = FontWeight.Black,
                ),
            )
        }

        Column(
            modifier = Modifier
                .width(930.dp)
                .padding(top = 34.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsRow(
                label = stringResource(R.string.settings_title),
                isSelected = true,
                onClick = onOpenSettings,
                modifier = Modifier.focusRequester(firstItemFocusRequester),
            )
            SettingsRow(
                label = stringResource(R.string.settings_support),
                isSelected = false,
                onClick = onSupport,
            )
            SettingsRow(
                label = stringResource(R.string.settings_user_agreement),
                isSelected = false,
                onClick = onAgreement,
            )
        }

        CinemaButton(
            text = stringResource(R.string.btn_sign_out),
            variant = CinemaButtonVariant.SecondaryDark,
            onClick = onSignOut,
            modifier = Modifier.padding(top = 30.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailSettingsContent(
    currentSection: SettingsSection,
    firstItemFocusRequester: FocusRequester,
    connectedSource: String?,
    refreshTrailing: String?,
    refreshEnabled: Boolean,
    showDevPreviews: Boolean,
    autoplayNext: Boolean,
    continueWatching: Boolean,
    skipIntro: Boolean,
    onOpenLanguage: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onOpenPlaylistSources: () -> Unit,
    onOpenTvGuide: () -> Unit,
    onOpenParentalControls: () -> Unit,
    onToggleDevPreviews: () -> Unit,
    onOpenEmptyPreview: () -> Unit,
    onOpenErrorPreview: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleContinueWatching: () -> Unit,
    onToggleSkipIntro: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = CinemaSpacing.ContentStart, end = 90.dp, top = 92.dp, bottom = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(42.dp),
    ) {
        Text(
            text = if (currentSection == SettingsSection.Language) {
                stringResource(R.string.settings_audio_language)
            } else {
                stringResource(R.string.settings_title)
            },
            style = MaterialTheme.typography.displaySmall.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Black,
            ),
        )

        Column(
            modifier = Modifier.width(930.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            if (currentSection == SettingsSection.Language) {
                LanguageSettingsSection()
            } else {
                SettingsGroupTitle(text = stringResource(R.string.settings_devices))
                SettingsRow(
                    label = stringResource(R.string.settings_my_devices),
                    isSelected = true,
                    onClick = onOpenPlaylistSources,
                    modifier = Modifier.focusRequester(firstItemFocusRequester),
                    trailing = stringResource(R.string.settings_devices_count, 2, 5),
                )

                SettingsGroupTitle(text = stringResource(R.string.settings_watching))
                SettingsRow(
                    label = stringResource(R.string.settings_audio_language),
                    isSelected = false,
                    onClick = onOpenLanguage,
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_autoplay_next),
                    isOn = autoplayNext,
                    onToggle = onToggleAutoplay,
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_continue_watching),
                    isOn = continueWatching,
                    onToggle = onToggleContinueWatching,
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_skip_intro),
                    isOn = skipIntro,
                    onToggle = onToggleSkipIntro,
                )

                SettingsGroupTitle(text = stringResource(R.string.settings_general))
                if (connectedSource != null) {
                    SettingsRow(
                        label = stringResource(R.string.settings_connected_source),
                        isSelected = false,
                        onClick = onOpenPlaylistSources,
                        trailing = connectedSource,
                    )
                    SettingsRow(
                        label = stringResource(R.string.btn_refresh_catalog),
                        isSelected = false,
                        enabled = refreshEnabled,
                        onClick = onRefreshCatalog,
                        trailing = refreshTrailing,
                    )
                }
                SettingsRow(
                    label = stringResource(R.string.settings_manage_data),
                    isSelected = false,
                    onClick = onOpenPlaylistSources,
                )
                SettingsRow(
                    label = stringResource(R.string.settings_playlist_sources),
                    isSelected = false,
                    onClick = onOpenPlaylistSources,
                )
                SettingsRow(
                    label = stringResource(R.string.livetv_tv_guide),
                    isSelected = false,
                    onClick = onOpenTvGuide,
                )
                SettingsRow(
                    label = stringResource(R.string.settings_parental_controls),
                    isSelected = false,
                    onClick = onOpenParentalControls,
                )
                SettingsRow(
                    label = stringResource(R.string.settings_developer_previews),
                    isSelected = false,
                    onClick = onToggleDevPreviews,
                    trailingIcon = if (showDevPreviews) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                )
                if (showDevPreviews) {
                    SettingsRow(
                        label = stringResource(R.string.settings_preview_empty),
                        isSelected = false,
                        onClick = onOpenEmptyPreview,
                    )
                    SettingsRow(
                        label = stringResource(R.string.settings_preview_error),
                        isSelected = false,
                        onClick = onOpenErrorPreview,
                    )
                }
                SettingsRow(
                    label = stringResource(R.string.settings_delete_account),
                    isSelected = false,
                    onClick = onSignOut,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineMedium.copy(
            color = CinemaColors.White,
            fontWeight = FontWeight.Black,
        ),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsPlaceholder(
    title: String,
    description: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.White, fontWeight = FontWeight.Bold),
    )
    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LanguageSettingsSection() {
    var selectedLanguage by remember { mutableStateOf(AppLocaleHelper.currentLanguageTag()) }
    val englishLabel = stringResource(R.string.settings_language_english)
    val arabicLabel = stringResource(R.string.settings_language_arabic)

    Text(
        text = stringResource(R.string.settings_language),
        style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.White, fontWeight = FontWeight.Bold),
    )
    Text(
        text = stringResource(R.string.settings_language_app_desc),
        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
    )
    SettingsRow(
        label = englishLabel,
        isSelected = selectedLanguage == AppLocaleHelper.LANGUAGE_EN,
        onClick = {
            selectedLanguage = AppLocaleHelper.LANGUAGE_EN
            AppLocaleHelper.applyLanguage(AppLocaleHelper.LANGUAGE_EN)
        },
        trailing = if (selectedLanguage == AppLocaleHelper.LANGUAGE_EN) {
            stringResource(R.string.settings_language_selected)
        } else {
            null
        },
    )
    SettingsRow(
        label = arabicLabel,
        isSelected = selectedLanguage == AppLocaleHelper.LANGUAGE_AR,
        onClick = {
            selectedLanguage = AppLocaleHelper.LANGUAGE_AR
            AppLocaleHelper.applyLanguage(AppLocaleHelper.LANGUAGE_AR)
        },
        trailing = if (selectedLanguage == AppLocaleHelper.LANGUAGE_AR) {
            stringResource(R.string.settings_language_selected)
        } else {
            null
        },
    )
}
