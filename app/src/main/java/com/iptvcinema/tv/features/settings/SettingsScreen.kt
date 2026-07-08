package com.iptvcinema.tv.features.settings

import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.design.components.AccountAvatar
import com.iptvcinema.tv.core.design.components.AccountDegradedBanner
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.SettingsHintText
import com.iptvcinema.tv.core.design.components.SettingsMenu
import com.iptvcinema.tv.core.design.components.SettingsMenuItem
import com.iptvcinema.tv.core.design.components.SettingsPanelHeader
import com.iptvcinema.tv.core.design.components.SettingsRow
import com.iptvcinema.tv.core.design.components.SettingsToggle
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.datastore.connectedSourceLabel
import com.iptvcinema.tv.core.model.AccountSummary
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.SessionViewModel
import com.iptvcinema.tv.core.navigation.navigateMainShellHome
import com.iptvcinema.tv.core.navigation.navigateOnboardingClearingStack
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.platform.AppLocaleHelper
import com.iptvcinema.tv.core.player.StreamingQualityOption

private enum class PlaybackSubPanel {
    None,
    Audio,
    Subtitles,
    Quality,
}

private data class PlaybackLanguageOption(val code: String, val labelRes: Int)

private val playbackLanguageOptions = listOf(
    PlaybackLanguageOption("en", R.string.settings_playback_audio_en),
    PlaybackLanguageOption("ar", R.string.settings_playback_audio_ar),
    PlaybackLanguageOption("fr", R.string.settings_playback_audio_fr),
    PlaybackLanguageOption("es", R.string.settings_playback_audio_es),
)

private data class StreamingQualityUiOption(val value: String, val labelRes: Int)

private val streamingQualityOptions = listOf(
    StreamingQualityUiOption(StreamingQualityOption.AUTO.storageValue, R.string.settings_quality_auto),
    StreamingQualityUiOption(StreamingQualityOption.P1080.storageValue, R.string.settings_quality_1080),
    StreamingQualityUiOption(StreamingQualityOption.P720.storageValue, R.string.settings_quality_720),
    StreamingQualityUiOption(StreamingQualityOption.P480.storageValue, R.string.settings_quality_480),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    initialSection: Int = 0,
    viewModel: SettingsViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
) {
    val sections = SettingsSection.menuSections
    var selectedSection by remember {
        mutableStateOf(sections.getOrElse(initialSection) { SettingsSection.Account })
    }
    var playbackSubPanel by remember { mutableStateOf(PlaybackSubPanel.None) }
    var showDevPreviews by remember { mutableStateOf(false) }
    val menuFocus = remember { FocusRequester() }
    val detailFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("settings")
    val session by viewModel.sessionState.collectAsState()
    val account by viewModel.accountSummary.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()
    val isCloudDegraded by sessionViewModel.isCloudDegraded.collectAsState()
    val connectedSource = session.connectedSourceLabel()
    var pendingProtectedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPlaylistPin by remember { mutableStateOf(false) }
    var playlistPinError by remember { mutableStateOf<String?>(null) }
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

    val displayAccount = account ?: AccountSummary(
        name = stringResource(R.string.nav_profile_guest),
        email = "—",
        plan = "Lumora Play",
        renewalDate = "—",
    )

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

    BackHandler {
        when {
            playbackSubPanel != PlaybackSubPanel.None -> playbackSubPanel = PlaybackSubPanel.None
            else -> navController.navigateMainShellHome()
        }
    }

    LaunchedEffect(focusState.hasSavedFocus, playbackSubPanel) {
        if (playbackSubPanel != PlaybackSubPanel.None) {
            detailFocus.requestFocus()
            return@LaunchedEffect
        }
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(menuFocus)
        } else {
            focusState.requestInitialFocus(menuFocus)
            focusState.saveFocusIndex(sections.indexOf(selectedSection).coerceAtLeast(0))
        }
    }

    CinemaScreen(showTopNav = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
        ) {
            if (isCloudDegraded) {
                AccountDegradedBanner(onRetry = sessionViewModel::retryCloudSync)
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CinemaSpacing.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap * 2),
            ) {
                Column(
                    modifier = Modifier.width(300.dp),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    SettingsPanelHeader(
                        title = stringResource(R.string.settings_title),
                        subtitle = stringResource(R.string.settings_subtitle),
                    )
                    SettingsMenu(
                        items = buildSettingsMenuItems(
                            sections = sections,
                            userSettings = userSettings,
                            connectedSource = connectedSource,
                        ),
                        selectedIndex = sections.indexOf(selectedSection).coerceAtLeast(0),
                        onSelected = { index ->
                            val section = sections[index]
                            if (section == SettingsSection.ParentalControls) {
                                navController.navigate(AppRoute.PARENTAL_CONTROLS)
                            } else {
                                playbackSubPanel = PlaybackSubPanel.None
                                selectedSection = section
                                focusState.saveFocusIndex(index)
                            }
                        },
                        firstItemFocusRequester = menuFocus,
                        focusedItemIndex = sections.indexOf(selectedSection).coerceAtLeast(0),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    when (playbackSubPanel) {
                        PlaybackSubPanel.Audio -> PlaybackAudioSubPanel(
                            userSettings = userSettings,
                            firstItemFocusRequester = detailFocus,
                            onSelectAudioLanguage = viewModel::updateDefaultAudioLanguage,
                        )
                        PlaybackSubPanel.Subtitles -> PlaybackSubtitlesSubPanel(
                            userSettings = userSettings,
                            firstItemFocusRequester = detailFocus,
                            onToggleSubtitles = viewModel::updateSubtitlesEnabled,
                            onSelectSubtitleLanguage = viewModel::updateDefaultSubtitleLanguage,
                        )
                        PlaybackSubPanel.Quality -> PlaybackQualitySubPanel(
                            userSettings = userSettings,
                            firstItemFocusRequester = detailFocus,
                            onSelectStreamingQuality = viewModel::updateStreamingQuality,
                        )
                        PlaybackSubPanel.None -> when (selectedSection) {
                            SettingsSection.Account -> AccountSettingsPanel(
                                account = displayAccount,
                                firstItemFocusRequester = detailFocus,
                                onSignOut = {
                                    viewModel.signOut {
                                        navController.navigateOnboardingClearingStack(AppRoute.WELCOME)
                                    }
                                },
                            )
                            SettingsSection.Playback -> PlaybackSettingsPanel(
                                userSettings = userSettings,
                                firstItemFocusRequester = detailFocus,
                                onToggleAutoplay = viewModel::updateAutoplayNextEpisode,
                                onToggleContinueWatching = viewModel::updateContinueWatching,
                                onOpenAudio = { playbackSubPanel = PlaybackSubPanel.Audio },
                                onOpenSubtitles = { playbackSubPanel = PlaybackSubPanel.Subtitles },
                                onOpenQuality = { playbackSubPanel = PlaybackSubPanel.Quality },
                            )
                            SettingsSection.Language -> LanguageSettingsPanel(
                                firstItemFocusRequester = detailFocus,
                            )
                            SettingsSection.DevicePreferences -> DeviceSettingsPanel(
                                connectedSource = connectedSource,
                                refreshTrailing = refreshTrailing,
                                refreshEnabled = refreshState !is CatalogRefreshState.Refreshing,
                                showDevPreviews = showDevPreviews,
                                firstItemFocusRequester = detailFocus,
                                onRefreshCatalog = {
                                    if (refreshState !is CatalogRefreshState.Refreshing) {
                                        runProtected { viewModel.refreshCurrentSource() }
                                    }
                                },
                                onOpenPlaylistSources = {
                                    runProtected { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) }
                                },
                                onOpenTvGuide = { navController.navigate(AppRoute.liveTv(openGuide = true)) },
                                onOpenParentalControls = { navController.navigate(AppRoute.PARENTAL_CONTROLS) },
                                onToggleDevPreviews = { showDevPreviews = !showDevPreviews },
                                onOpenEmptyPreview = { navController.navigate(AppRoute.EMPTY_STATE) },
                                onOpenErrorPreview = { navController.navigate(AppRoute.ERROR_STATE) },
                            )
                            SettingsSection.Subscription -> SubscriptionSettingsPanel(account = displayAccount)
                            SettingsSection.Support -> SupportSettingsPanel()
                            SettingsSection.About -> AboutSettingsPanel()
                            SettingsSection.ParentalControls -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun buildSettingsMenuItems(
    sections: List<SettingsSection>,
    userSettings: UserSettings?,
    connectedSource: String?,
): List<SettingsMenuItem> {
    val context = LocalContext.current
    val deviceName = remember {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: Build.MODEL
    }
    val appLanguage = when (AppLocaleHelper.currentLanguageTag()) {
        AppLocaleHelper.LANGUAGE_AR -> stringResource(R.string.settings_language_arabic)
        else -> stringResource(R.string.settings_language_english)
    }
    return sections.map { section ->
        SettingsMenuItem(
            label = stringResource(section.labelRes),
            summary = when (section) {
                SettingsSection.Playback -> streamingQualitySummary(userSettings?.streamingQuality)
                SettingsSection.Language -> appLanguage
                SettingsSection.DevicePreferences -> connectedSource ?: deviceName
                SettingsSection.Account -> null
                else -> null
            },
        )
    }
}

@Composable
private fun playbackAudioSummary(languageCode: String?): String {
    val code = languageCode ?: "en"
    return playbackLanguageOptions.firstOrNull { it.code == code }?.labelRes?.let { stringResource(it) }
        ?: code.uppercase()
}

@Composable
private fun streamingQualitySummary(quality: String?): String {
    val normalized = StreamingQualityOption.normalize(quality)
    return streamingQualityOptions.firstOrNull { it.value == normalized }?.labelRes?.let { stringResource(it) }
        ?: stringResource(R.string.settings_quality_auto)
}

@Composable
private fun subtitlesSummary(userSettings: UserSettings?): String {
    if (userSettings?.subtitlesEnabled != true) {
        return stringResource(R.string.settings_subtitles_off)
    }
    return playbackAudioSummary(userSettings.defaultSubtitleLanguage ?: userSettings.defaultAudioLanguage)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccountSettingsPanel(
    account: AccountSummary,
    firstItemFocusRequester: FocusRequester,
    onSignOut: () -> Unit,
) {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_account),
        subtitle = stringResource(R.string.settings_account_desc),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(size = 120.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = account.email,
                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
            )
            Text(
                text = stringResource(R.string.settings_active),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = CinemaColors.Success,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
    CinemaButton(
        text = stringResource(R.string.btn_sign_out),
        variant = CinemaButtonVariant.SecondaryDark,
        onClick = onSignOut,
        modifier = Modifier
            .focusRequester(firstItemFocusRequester)
            .padding(top = 12.dp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackSettingsPanel(
    userSettings: UserSettings?,
    firstItemFocusRequester: FocusRequester,
    onToggleAutoplay: (Boolean) -> Unit,
    onToggleContinueWatching: (Boolean) -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenQuality: () -> Unit,
) {
    val autoplayNext = userSettings?.autoplayNextEpisode ?: true
    val continueWatching = userSettings?.continueWatchingEnabled ?: true

    SettingsPanelHeader(
        title = stringResource(R.string.settings_playback),
        subtitle = stringResource(R.string.settings_playback_desc),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsToggle(
            label = stringResource(R.string.settings_autoplay_next),
            isOn = autoplayNext,
            onToggle = { onToggleAutoplay(!autoplayNext) },
            modifier = Modifier.focusRequester(firstItemFocusRequester),
        )
        SettingsHintText(text = stringResource(R.string.settings_autoplay_desc))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsToggle(
            label = stringResource(R.string.settings_continue_watching),
            isOn = continueWatching,
            onToggle = { onToggleContinueWatching(!continueWatching) },
        )
        SettingsHintText(text = stringResource(R.string.settings_continue_watching_desc))
    }

    SettingsGroupTitle(text = stringResource(R.string.settings_in_player))
    SettingsRow(
        label = stringResource(R.string.settings_default_audio),
        isSelected = false,
        onClick = onOpenAudio,
        trailing = playbackAudioSummary(userSettings?.defaultAudioLanguage),
    )
    SettingsRow(
        label = stringResource(R.string.settings_subtitles),
        isSelected = false,
        onClick = onOpenSubtitles,
        trailing = subtitlesSummary(userSettings),
    )
    SettingsRow(
        label = stringResource(R.string.settings_streaming_quality),
        isSelected = false,
        onClick = onOpenQuality,
        trailing = streamingQualitySummary(userSettings?.streamingQuality),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackAudioSubPanel(
    userSettings: UserSettings?,
    firstItemFocusRequester: FocusRequester,
    onSelectAudioLanguage: (String) -> Unit,
) {
    val selectedCode = userSettings?.defaultAudioLanguage ?: "en"
    val labels = playbackLanguageOptions.map { stringResource(it.labelRes) }
    val selectedIndex = playbackLanguageOptions.indexOfFirst { it.code == selectedCode }.coerceAtLeast(0)

    SettingsPanelHeader(
        title = stringResource(R.string.settings_select_audio),
        subtitle = stringResource(R.string.settings_audio_desc),
    )
    FilterChipRow(
        items = labels,
        selectedIndex = selectedIndex,
        onSelected = { index -> onSelectAudioLanguage(playbackLanguageOptions[index].code) },
        chipFocusRequester = firstItemFocusRequester,
        focusedChipIndex = selectedIndex,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackSubtitlesSubPanel(
    userSettings: UserSettings?,
    firstItemFocusRequester: FocusRequester,
    onToggleSubtitles: (Boolean) -> Unit,
    onSelectSubtitleLanguage: (String) -> Unit,
) {
    val subtitlesEnabled = userSettings?.subtitlesEnabled ?: false
    val selectedCode = userSettings?.defaultSubtitleLanguage ?: userSettings?.defaultAudioLanguage ?: "en"

    SettingsPanelHeader(
        title = stringResource(R.string.settings_select_subtitles),
        subtitle = stringResource(R.string.settings_subtitles_desc),
    )
    SettingsToggle(
        label = stringResource(R.string.settings_subtitles),
        isOn = subtitlesEnabled,
        onToggle = { onToggleSubtitles(!subtitlesEnabled) },
        modifier = Modifier.focusRequester(firstItemFocusRequester),
    )
    if (subtitlesEnabled) {
        val labels = playbackLanguageOptions.map { stringResource(it.labelRes) }
        val selectedIndex = playbackLanguageOptions.indexOfFirst { it.code == selectedCode }.coerceAtLeast(0)
        FilterChipRow(
            items = labels,
            selectedIndex = selectedIndex,
            onSelected = { index -> onSelectSubtitleLanguage(playbackLanguageOptions[index].code) },
            focusedChipIndex = selectedIndex,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackQualitySubPanel(
    userSettings: UserSettings?,
    firstItemFocusRequester: FocusRequester,
    onSelectStreamingQuality: (String) -> Unit,
) {
    val selectedQuality = StreamingQualityOption.normalize(userSettings?.streamingQuality)
    val labels = streamingQualityOptions.map { stringResource(it.labelRes) }
    val selectedIndex = streamingQualityOptions.indexOfFirst { it.value == selectedQuality }.coerceAtLeast(0)

    SettingsPanelHeader(
        title = stringResource(R.string.settings_select_quality),
        subtitle = stringResource(R.string.settings_quality_desc),
    )
    FilterChipRow(
        items = labels,
        selectedIndex = selectedIndex,
        onSelected = { index -> onSelectStreamingQuality(streamingQualityOptions[index].value) },
        chipFocusRequester = firstItemFocusRequester,
        focusedChipIndex = selectedIndex,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LanguageSettingsPanel(firstItemFocusRequester: FocusRequester) {
    var selectedLanguage by remember { mutableStateOf(AppLocaleHelper.currentLanguageTag()) }
    val labels = listOf(
        stringResource(R.string.settings_language_english),
        stringResource(R.string.settings_language_arabic),
    )

    SettingsPanelHeader(
        title = stringResource(R.string.settings_app_language),
        subtitle = stringResource(R.string.settings_language_app_desc),
    )
    FilterChipRow(
        items = labels,
        selectedIndex = if (selectedLanguage == AppLocaleHelper.LANGUAGE_AR) 1 else 0,
        onSelected = { index ->
            val tag = if (index == 1) AppLocaleHelper.LANGUAGE_AR else AppLocaleHelper.LANGUAGE_EN
            selectedLanguage = tag
            AppLocaleHelper.applyLanguage(tag)
        },
        chipFocusRequester = firstItemFocusRequester,
        focusedChipIndex = if (selectedLanguage == AppLocaleHelper.LANGUAGE_AR) 1 else 0,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeviceSettingsPanel(
    connectedSource: String?,
    refreshTrailing: String?,
    refreshEnabled: Boolean,
    showDevPreviews: Boolean,
    firstItemFocusRequester: FocusRequester,
    onRefreshCatalog: () -> Unit,
    onOpenPlaylistSources: () -> Unit,
    onOpenTvGuide: () -> Unit,
    onOpenParentalControls: () -> Unit,
    onToggleDevPreviews: () -> Unit,
    onOpenEmptyPreview: () -> Unit,
    onOpenErrorPreview: () -> Unit,
) {
    val context = LocalContext.current
    val deviceName = remember {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: Build.MODEL
    }

    SettingsPanelHeader(
        title = stringResource(R.string.settings_device_preferences),
        subtitle = stringResource(R.string.settings_device_desc),
    )

    SettingsRow(
        label = stringResource(R.string.settings_device_name),
        isSelected = false,
        onClick = {},
        enabled = false,
        trailing = deviceName,
        modifier = Modifier.focusRequester(firstItemFocusRequester),
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
    if (BuildConfig.DEBUG) {
        SettingsRow(
            label = stringResource(R.string.settings_developer_previews),
            isSelected = false,
            onClick = onToggleDevPreviews,
            trailingIcon = if (showDevPreviews) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
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
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SubscriptionSettingsPanel(account: AccountSummary) {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_subscription),
        subtitle = stringResource(R.string.settings_subscription_desc),
    )
    SettingsRow(
        label = stringResource(R.string.settings_account),
        isSelected = false,
        onClick = {},
        enabled = false,
        trailing = account.email,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AboutSettingsPanel() {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_about),
        subtitle = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
    )
    Text(
        text = stringResource(R.string.settings_user_agreement),
        style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.White, fontWeight = FontWeight.Bold),
    )
    Text(
        text = stringResource(R.string.settings_about_desc),
        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SupportSettingsPanel() {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_support),
        subtitle = stringResource(R.string.settings_support_contact),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        style = MaterialTheme.typography.titleLarge.copy(
            color = CinemaColors.White,
            fontWeight = FontWeight.Black,
        ),
    )
}
