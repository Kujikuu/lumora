package com.iptvcinema.tv.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.iptvcinema.tv.core.design.components.AccountSummaryCard
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
import com.iptvcinema.tv.core.util.rememberPrototypeFeedback

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    initialSection: Int = 0,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var selectedSection by remember { mutableIntStateOf(initialSection) }
    var showDevPreviews by remember { mutableStateOf(false) }
    val showFeedback = rememberPrototypeFeedback()
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
    val feedbackAccountSynced = stringResource(R.string.feedback_account_synced)
    val feedbackQualitySoon = stringResource(R.string.feedback_quality_soon)
    val feedbackAudioSoon = stringResource(R.string.feedback_audio_soon)
    val feedbackSubtitleSoon = stringResource(R.string.feedback_subtitle_soon)
    val feedbackThemeLocked = stringResource(R.string.feedback_theme_locked)
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
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = CinemaSpacing.ScreenPaddingVertical),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            CinemaSerifTitle(text = stringResource(R.string.settings_title))
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                modifier = Modifier.padding(start = CinemaSpacing.ContentStart),
            )
            Row(
                modifier = Modifier.padding(start = CinemaSpacing.ContentStart, end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                SettingsMenu(
                    items = sectionLabels,
                    selectedIndex = selectedSection,
                    onSelected = { index ->
                        selectedSection = index
                        focusState.saveFocusIndex(index)
                        if (sections[index] == SettingsSection.ParentalControls) {
                            navController.navigate(AppRoute.PARENTAL_CONTROLS)
                        }
                    },
                    firstItemFocusRequester = menuFocus,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    when (sections[selectedSection]) {
                        SettingsSection.Account -> {
                            AccountSummaryCard(
                                account = account ?: FakeDataProvider.accountSummary,
                                onManageAccount = { showFeedback(feedbackAccountSynced) },
                            )
                            if (connectedSource != null) {
                                SettingsRow(
                                    label = stringResource(R.string.settings_connected_source),
                                    isSelected = false,
                                    onClick = { runProtected { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) } },
                                    trailing = connectedSource,
                                )
                                SettingsRow(
                                    label = stringResource(R.string.btn_refresh_catalog),
                                    isSelected = false,
                                    onClick = {
                                        if (refreshState !is CatalogRefreshState.Refreshing) {
                                            runProtected { viewModel.refreshCurrentSource() }
                                        }
                                    },
                                    trailing = refreshTrailing,
                                )
                            }
                            SettingsRow(
                                label = stringResource(R.string.btn_sign_out),
                                isSelected = false,
                                onClick = {
                                    viewModel.signOut {
                                        navController.navigateOnboardingClearingStack(AppRoute.ACTIVATION)
                                    }
                                },
                            )
                        }
                        SettingsSection.Subscription -> SettingsPlaceholder(
                            title = stringResource(R.string.settings_subscription),
                            description = stringResource(R.string.settings_subscription_desc),
                        )
                        SettingsSection.Playback -> {
                            Text(
                                text = stringResource(R.string.settings_playback),
                                style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.White, fontWeight = FontWeight.Bold),
                            )
                            SettingsRow(
                                label = stringResource(R.string.settings_streaming_quality),
                                isSelected = false,
                                onClick = { showFeedback(feedbackQualitySoon) },
                                trailing = stringResource(R.string.settings_quality_auto),
                            )
                            SettingsRow(
                                label = stringResource(R.string.settings_default_audio),
                                isSelected = false,
                                onClick = { showFeedback(feedbackAudioSoon) },
                                trailing = stringResource(R.string.settings_audio_english),
                            )
                            SettingsRow(
                                label = stringResource(R.string.settings_subtitles),
                                isSelected = false,
                                onClick = { showFeedback(feedbackSubtitleSoon) },
                                trailing = stringResource(R.string.settings_subtitles_off),
                            )
                            SettingsToggle(
                                label = stringResource(R.string.settings_autoplay_next),
                                isOn = autoplayNext,
                                onToggle = { viewModel.updateAutoplayNextEpisode(!autoplayNext) },
                            )
                            SettingsToggle(
                                label = stringResource(R.string.settings_continue_watching),
                                isOn = continueWatching,
                                onToggle = { viewModel.updateContinueWatching(!continueWatching) },
                            )
                            SettingsToggle(
                                label = stringResource(R.string.settings_skip_intro),
                                isOn = skipIntro,
                                onToggle = { viewModel.updateSkipIntro(!skipIntro) },
                            )
                            SettingsRow(
                                label = stringResource(R.string.settings_theme),
                                isSelected = false,
                                onClick = { showFeedback(feedbackThemeLocked) },
                                trailing = userSettings?.theme?.replace('_', ' ')
                                    ?: stringResource(R.string.settings_theme_dark),
                            )
                        }
                        SettingsSection.Language -> SettingsPlaceholder(
                            title = stringResource(R.string.settings_language),
                            description = stringResource(R.string.settings_language_desc),
                        )
                        SettingsSection.Notifications -> SettingsPlaceholder(
                            title = stringResource(R.string.settings_notifications),
                            description = stringResource(R.string.settings_notifications_desc),
                        )
                        SettingsSection.DevicePreferences -> SettingsPlaceholder(
                            title = stringResource(R.string.settings_device_preferences),
                            description = stringResource(R.string.settings_device_desc),
                        )
                        SettingsSection.About -> SettingsPlaceholder(
                            title = stringResource(R.string.settings_about),
                            description = stringResource(R.string.settings_about_desc),
                        )
                        SettingsSection.ParentalControls -> Unit
                    }
                    SettingsRow(
                        label = stringResource(R.string.settings_playlist_sources),
                        isSelected = false,
                        onClick = { runProtected { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) } },
                    )
                    SettingsRow(
                        label = stringResource(R.string.settings_parental_controls),
                        isSelected = false,
                        onClick = { navController.navigate(AppRoute.PARENTAL_CONTROLS) },
                    )
                    SettingsRow(
                        label = stringResource(R.string.settings_developer_previews),
                        isSelected = false,
                        onClick = { showDevPreviews = !showDevPreviews },
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
                            onClick = { navController.navigate(AppRoute.EMPTY_STATE) },
                        )
                        SettingsRow(
                            label = stringResource(R.string.settings_preview_error),
                            isSelected = false,
                            onClick = { navController.navigate(AppRoute.ERROR_STATE) },
                        )
                    }
                }
            }
        }
    }
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
