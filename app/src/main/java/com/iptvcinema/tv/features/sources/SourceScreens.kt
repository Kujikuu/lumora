package com.iptvcinema.tv.features.sources

import com.iptvcinema.tv.core.navigation.OnboardingBackHandler
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.iptvcinema.tv.R
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaLogo
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.CinemaTextField
import com.iptvcinema.tv.core.design.components.FooterNote
import com.iptvcinema.tv.core.design.components.ImportPreviewPanel
import com.iptvcinema.tv.core.design.components.SettingsToggle
import com.iptvcinema.tv.core.design.components.SourceTypeCard
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.m3u.M3uDownloader
import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.XtreamCredentials
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.components.SkeletonSourceCards
import com.iptvcinema.tv.core.model.PlaylistSourceItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddSourceScreen(
    allowBack: Boolean = false,
    onBack: () -> Unit = {},
    onXtream: () -> Unit,
    onM3u: () -> Unit,
) {
    val firstCardFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("add_source")

    LaunchedEffect(focusState.hasSavedFocus) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(firstCardFocus)
        } else {
            focusState.requestInitialFocus(firstCardFocus)
            focusState.saveFocusIndex(0)
        }
    }

    OnboardingBackHandler(allowBack = allowBack, onBack = onBack)

    CinemaScreen(showTopNav = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            CinemaLogo()
            Text(
                text = stringResource(R.string.source_title),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = stringResource(R.string.source_subtitle),
                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
            )
            SourceTypeCard(
                title = stringResource(R.string.source_xtream_title),
                description = stringResource(R.string.source_xtream_desc),
                onClick = onXtream,
                modifier = Modifier.focusRequester(firstCardFocus),
            )
            SourceTypeCard(
                title = stringResource(R.string.source_m3u_title),
                description = stringResource(R.string.source_m3u_desc),
                onClick = onM3u,
            )
            FooterNote(stringResource(R.string.source_footer))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XtreamFormScreen(
    connectState: XtreamConnectUiState,
    onConnect: (XtreamCredentials) -> Unit,
    onBack: () -> Unit,
) {
    val defaultServer = if (BuildConfig.DEBUG && BuildConfig.XTREAM_TEST_SERVER.isNotBlank()) {
        BuildConfig.XTREAM_TEST_SERVER
    } else {
        "http://example.com:8080"
    }
    val defaultUsername = if (BuildConfig.DEBUG && BuildConfig.XTREAM_TEST_USERNAME.isNotBlank()) {
        BuildConfig.XTREAM_TEST_USERNAME
    } else {
        "demo_user"
    }
    val defaultPassword = if (BuildConfig.DEBUG && BuildConfig.XTREAM_TEST_PASSWORD.isNotBlank()) {
        BuildConfig.XTREAM_TEST_PASSWORD
    } else {
        "••••••••"
    }
    var serverUrl by remember { mutableStateOf(defaultServer) }
    var username by remember { mutableStateOf(defaultUsername) }
    var password by remember { mutableStateOf(defaultPassword) }
    var accountName by remember { mutableStateOf("My Xtream Account") }
    var rememberSource by remember { mutableStateOf(true) }
    val firstFieldFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("xtream_form")
    val checklist = connectState.checklist.ifEmpty {
        listOf(
            "Server reachable" to false,
            "Authentication" to false,
            "Live channels" to false,
            "Movies" to false,
            "Series" to false,
        )
    }

    LaunchedEffect(focusState.hasSavedFocus) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(firstFieldFocus)
        } else {
            focusState.requestInitialFocus(firstFieldFocus)
            focusState.saveFocusIndex(0)
        }
    }

    PopBackHandler(onBack = onBack)

    CinemaScreen(showTopNav = false) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                Text(
                    text = stringResource(R.string.source_connect_xtream),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.source_connect_xtream_desc),
                    style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                )
                CinemaTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = stringResource(R.string.field_server_url),
                    modifier = Modifier.focusRequester(firstFieldFocus),
                )
                CinemaTextField(value = username, onValueChange = { username = it }, label = stringResource(R.string.field_username))
                CinemaTextField(value = password, onValueChange = { password = it }, label = stringResource(R.string.field_password), isPassword = true)
                CinemaTextField(value = accountName, onValueChange = { accountName = it }, label = stringResource(R.string.field_account_name))
                SettingsToggle(
                    label = stringResource(R.string.field_remember_source),
                    isOn = rememberSource,
                    onToggle = { rememberSource = !rememberSource },
                )
                connectState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.Danger),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                    CinemaButton(
                        text = if (connectState.isConnecting) "Connecting…" else stringResource(R.string.btn_connect),
                        variant = CinemaButtonVariant.PrimaryAccent,
                        enabled = !connectState.isConnecting,
                        onClick = {
                            onConnect(
                                XtreamCredentials(
                                    serverUrl = serverUrl.trim(),
                                    username = username.trim(),
                                    password = password,
                                    accountName = accountName.trim(),
                                ),
                            )
                        },
                    )
                    CinemaButton(
                        text = stringResource(R.string.btn_back),
                        variant = CinemaButtonVariant.SecondaryDark,
                        enabled = !connectState.isConnecting,
                        onClick = onBack,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                ImportPreviewPanel(
                    title = stringResource(R.string.source_connection_status),
                    items = checklist,
                )
                Text(
                    text = stringResource(R.string.source_credentials_note),
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun M3uFormScreen(
    connectState: M3uConnectUiState,
    onImport: (M3uCredentials) -> Unit,
    onBack: () -> Unit,
) {
    var playlistName by remember { mutableStateOf("My Playlist") }
    var m3uUrl by remember { mutableStateOf("http://example.com/playlist.m3u") }
    var epgUrl by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var userAgent by remember { mutableStateOf("") }
    var referer by remember { mutableStateOf("") }
    var headers by remember { mutableStateOf("") }
    val firstFieldFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("m3u_form")
    val checklist = connectState.checklist.ifEmpty {
        listOf(
            "Playlist URL valid" to false,
            "Playlist downloaded" to false,
            "Channels parsed" to false,
            "EPG" to false,
            "Sync complete" to false,
        )
    }

    LaunchedEffect(focusState.hasSavedFocus) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(firstFieldFocus)
        } else {
            focusState.requestInitialFocus(firstFieldFocus)
            focusState.saveFocusIndex(0)
        }
    }

    PopBackHandler(onBack = onBack)

    CinemaScreen(showTopNav = false) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                Text(
                    text = stringResource(R.string.source_add_m3u),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.source_add_m3u_desc),
                    style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                )
                CinemaTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = stringResource(R.string.field_playlist_name),
                    modifier = Modifier.focusRequester(firstFieldFocus),
                )
                CinemaTextField(value = m3uUrl, onValueChange = { m3uUrl = it }, label = stringResource(R.string.field_m3u_url))
                CinemaTextField(value = epgUrl, onValueChange = { epgUrl = it }, label = stringResource(R.string.field_epg_url))
                CinemaButton(
                    text = if (showAdvanced) "Hide Advanced Options" else "Advanced Options",
                    variant = CinemaButtonVariant.Ghost,
                    onClick = { showAdvanced = !showAdvanced },
                )
                if (showAdvanced) {
                    CinemaTextField(value = userAgent, onValueChange = { userAgent = it }, label = stringResource(R.string.field_user_agent))
                    CinemaTextField(value = referer, onValueChange = { referer = it }, label = stringResource(R.string.field_referer))
                    CinemaTextField(value = headers, onValueChange = { headers = it }, label = stringResource(R.string.field_headers))
                }
                connectState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.Danger),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                    CinemaButton(
                        text = if (connectState.isConnecting) "Importing…" else "Import Playlist",
                        variant = CinemaButtonVariant.PrimaryAccent,
                        enabled = !connectState.isConnecting,
                        onClick = {
                            onImport(
                                M3uCredentials(
                                    playlistUrl = m3uUrl.trim(),
                                    epgUrl = epgUrl.trim().ifBlank { null },
                                    playlistName = playlistName.trim(),
                                    userAgent = userAgent.trim().ifBlank { null },
                                    referer = referer.trim().ifBlank { null },
                                    customHeaders = M3uDownloader.parseCustomHeaders(headers),
                                ),
                            )
                        },
                    )
                    CinemaButton(
                        text = stringResource(R.string.btn_back),
                        variant = CinemaButtonVariant.SecondaryDark,
                        enabled = !connectState.isConnecting,
                        onClick = onBack,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                ImportPreviewPanel(
                    title = stringResource(R.string.source_import_status),
                    items = checklist,
                )
                Text(
                    text = stringResource(R.string.source_playlist_note),
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistManagementScreen(
    currentSourceId: String? = null,
    sourceType: SourceType? = null,
    isDemoMode: Boolean = false,
    sourcesUiState: SourcesUiState = SourcesUiState.Loading,
    syncMessage: String? = null,
    onLoadSources: () -> Unit = {},
    onAddSource: () -> Unit,
    onSetActive: (String) -> Unit = {},
    onResyncSource: (String) -> Unit = {},
    onDeleteSource: (String) -> Unit = {},
    onEditSource: (PlaylistSourceItem) -> Unit = {},
    onExpiredAccount: () -> Unit = {},
    onInvalidPlaylist: () -> Unit = {},
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onLoadSources() }

    val sources = when (sourcesUiState) {
        is SourcesUiState.Ready -> sourcesUiState.sources
        else -> emptyList()
    }
    val activeSourceIndex = remember(currentSourceId, sourceType, isDemoMode) {
        when {
            isDemoMode -> -1
            currentSourceId != null -> {
                sources.indexOfFirst { it.id == currentSourceId }.takeIf { it >= 0 }
                    ?: sourceType?.let { type -> sources.indexOfFirst { it.type == type } }?.takeIf { it >= 0 }
                    ?: -1
            }
            sourceType != null -> sources.indexOfFirst { it.type == sourceType }.takeIf { it >= 0 } ?: -1
            else -> -1
        }
    }
    val focusIndex = activeSourceIndex.coerceAtLeast(0)
    val sourceFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("playlist_management")

    LaunchedEffect(focusState.hasSavedFocus, focusIndex) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(sourceFocus)
        } else {
            focusState.requestInitialFocus(sourceFocus)
            focusState.saveFocusIndex(focusIndex)
        }
    }

    PopBackHandler(onBack = onBack)

    CinemaScreen(showTopNav = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.source_playlist_sources),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = when {
                            isDemoMode -> "Active source: Demo Mode"
                            sourceType != null -> "Active source: ${sourceType.name.replace('_', ' ')}"
                            else -> "Manage your Xtream Codes accounts and M3U playlists."
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                    )
                }
                CinemaButton(text = stringResource(R.string.btn_add_source), variant = CinemaButtonVariant.PrimaryAccent, onClick = onAddSource)
            }
            when {
                sourcesUiState is SourcesUiState.Loading -> {
                    SkeletonSourceCards(count = 2)
                }
                sourcesUiState is SourcesUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ErrorState(
                            title = stringResource(R.string.source_could_not_load),
                            description = sourcesUiState.message,
                            errorCode = null,
                            onRetry = onLoadSources,
                            onSwitchStream = onAddSource,
                            onBack = onBack,
                            showSwitchStream = true,
                            switchStreamLabel = stringResource(R.string.btn_add_source),
                            backLabel = stringResource(R.string.btn_back),
                        )
                    }
                }
                sources.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.source_no_playlists),
                        description = stringResource(R.string.source_no_playlists_desc),
                        primaryAction = stringResource(R.string.btn_add_source),
                        secondaryAction = stringResource(R.string.btn_back),
                        onPrimary = onAddSource,
                        onSecondary = onBack,
                    )
                }
                else -> {
                    if (syncMessage != null) {
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                        )
                    }
                    sources.forEachIndexed { index, source ->
                        val isActive = index == activeSourceIndex || source.id == currentSourceId
                        val needsRecovery = source.status == SourceStatus.EXPIRED ||
                            (source.status == SourceStatus.FAILED && source.type == SourceType.M3U)
                        com.iptvcinema.tv.core.design.components.SourceCard(
                            name = source.name,
                            type = source.type,
                            status = source.status,
                            channelCount = source.channelCount,
                            lastSynced = source.lastSynced,
                            onSync = { onResyncSource(source.id) },
                            onEdit = {
                                if (needsRecovery) {
                                    when {
                                        source.status == SourceStatus.EXPIRED -> onExpiredAccount()
                                        else -> onInvalidPlaylist()
                                    }
                                } else {
                                    onEditSource(source)
                                }
                            },
                            onRemove = { onDeleteSource(source.id) },
                            modifier = Modifier
                                .then(
                                    if (index == focusIndex) {
                                        Modifier.focusRequester(sourceFocus)
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    if (isActive) {
                                        Modifier.background(CinemaColors.GoldDeep.copy(alpha = 0.08f))
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                        if (isActive && needsRecovery) {
                            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                                CinemaButton(
                                    text = if (source.status == SourceStatus.EXPIRED) {
                                        stringResource(R.string.btn_reconnect)
                                    } else {
                                        stringResource(R.string.btn_edit_playlist)
                                    },
                                    variant = CinemaButtonVariant.PrimaryAccent,
                                    onClick = {
                                        when {
                                            source.status == SourceStatus.EXPIRED -> onExpiredAccount()
                                            else -> onInvalidPlaylist()
                                        }
                                    },
                                )
                            }
                        }
                    }
                    val activeSource = sources.getOrNull(activeSourceIndex)
                    com.iptvcinema.tv.core.design.components.SyncStatusPanel(
                        channels = activeSource?.channelCount ?: 0,
                        movies = activeSource?.movieCount ?: 0,
                        series = activeSource?.seriesCount ?: 0,
                        epgAvailable = activeSource?.epgAvailable ?: false,
                        lastUpdate = activeSource?.lastSynced ?: "Never synced",
                    )
                }
            }
            CinemaButton(text = stringResource(R.string.btn_back), variant = CinemaButtonVariant.Ghost, onClick = onBack)
        }
    }
}
