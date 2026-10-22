package com.iptvcinema.tv.features.search

import android.app.Activity
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.design.components.CatalogSkeletonStyle
import com.iptvcinema.tv.core.design.components.CatalogStateContent
import com.iptvcinema.tv.core.design.components.CinemaSpinner
import com.iptvcinema.tv.core.design.components.SearchKeyboard
import com.iptvcinema.tv.core.design.components.SearchKeyboardLayout
import com.iptvcinema.tv.core.design.components.shellContentStart
import com.iptvcinema.tv.core.design.components.shellHeroContentStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberCatalogStateCallbacks
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.util.isolateDirection
import com.iptvcinema.tv.features.browse.ImmersiveBrowse
import com.iptvcinema.tv.features.browse.openBrowseCard
import com.iptvcinema.tv.features.browse.rememberImmersiveBrowseState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The keyboard and query take more of the top area than a spotlight does. */
private const val SEARCH_TOP_WEIGHT = 0.62f
private const val RESTORE_FALLBACK_MS = 1_000L

/**
 * Search in the Home layout. While the viewer types, the top area shows the query and keyboard
 * and the rails below show results (or, before typing, recent searches and top rated ideas).
 * Moving down onto a result swaps the keyboard for the spotlight and backdrop of the focused
 * title; Up from the first rail brings the keyboard back.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("search")
    val browse = rememberImmersiveBrowseState(focusState)
    val catalogCallbacks = rememberCatalogStateCallbacks(navController, onRetry = viewModel::retry)
    var keyboardLayout by remember { mutableStateOf(SearchKeyboardLayout.English) }
    val context = LocalContext.current
    val voiceAvailable = remember(context) { VoiceSearch.isAvailable(context) }
    val voicePrompt = stringResource(R.string.search_voice_prompt)
    val voiceUnavailable = stringResource(R.string.search_voice_unavailable)
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = VoiceSearch.bestTranscript(
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS),
        ) ?: return@rememberLauncherForActivityResult
        viewModel.updateQuery(spoken)
    }
    fun startVoiceSearch() {
        val intent = VoiceSearch.recognizeIntent(VoiceSearch.languageTag(keyboardLayout), voicePrompt)
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { Toast.makeText(context, voiceUnavailable, Toast.LENGTH_SHORT).show() }
    }
    // Coming back from a result: focus returns to it. Typing first cancels that.
    val restoreIntoRails = remember { focusState.sectionId.isNotBlank() }
    var keyboardUsed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun focusKeyboard() {
        scope.launch { focusState.restoreFocus(keyboardFocus) }
    }

    fun type(action: () -> Unit) {
        keyboardUsed = true
        action()
    }

    MainShellBackHandler(navController = navController, isHomeTab = false)

    LaunchedEffect(Unit) {
        if (restoreIntoRails) {
            delay(RESTORE_FALLBACK_MS)
            if (!browse.railsHaveFocus) focusState.restoreFocus(keyboardFocus)
        } else {
            focusState.requestInitialFocus(keyboardFocus)
        }
    }

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.Search,
        onRailExitRight = if (uiState.loadState == CatalogLoadState.Ready) ::focusKeyboard else null,
    ) {
        if (uiState.loadState != CatalogLoadState.Ready) {
            CatalogStateContent(
                loadState = uiState.loadState,
                message = uiState.message,
                sourceStatus = uiState.sourceStatus,
                sourceType = uiState.sourceType,
                skeletonStyle = CatalogSkeletonStyle.PosterGrid,
                emptyTitle = stringResource(R.string.search_no_results),
                emptyDescription = stringResource(R.string.search_no_results_desc),
                onAddSource = catalogCallbacks.onAddSource,
                onRetry = catalogCallbacks.onRetry,
                onManageSources = catalogCallbacks.onManageSources,
                onEditSource = catalogCallbacks.onEditSource,
            ) {}
            return@MainShellScaffold
        }
        ImmersiveBrowse(
            state = browse,
            sections = uiState.sections,
            isReady = restoreIntoRails && !keyboardUsed && uiState.sections.any { it.id == focusState.sectionId },
            spotlightWeight = SEARCH_TOP_WEIGHT,
            onUpFromFirstRail = ::focusKeyboard,
            onCardClick = { section, card ->
                if (card.contentType == BrowseCardTypes.SEARCH_TERM) {
                    type { viewModel.applyRecentSearch(card.title) }
                    focusKeyboard()
                } else {
                    viewModel.onResultOpened()
                    openBrowseCard(navController, section, card)
                }
            },
            onCardLongClick = { _, card ->
                if (card.contentType != BrowseCardTypes.SEARCH_TERM) viewModel.toggleFavorite(card)
            },
            topContent = {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = shellHeroContentStart(), bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.62f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchQueryField(
                            query = uiState.query,
                            hint = stringResource(R.string.search_hint),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (uiState.isSearching) CinemaSpinner(size = 24.dp, strokeWidth = 3.dp)
                    }
                    SearchKeyboard(
                        layout = keyboardLayout,
                        onLayoutToggle = {
                            keyboardLayout = when (keyboardLayout) {
                                SearchKeyboardLayout.English -> SearchKeyboardLayout.Arabic
                                SearchKeyboardLayout.Arabic -> SearchKeyboardLayout.English
                            }
                        },
                        onDeviceKeyboard = {},
                        showDeviceKeyboard = false,
                        onKeyPress = { key -> type { viewModel.appendToQuery(key) } },
                        onBackspace = { type(viewModel::deleteLastCharacter) },
                        onClear = { type(viewModel::clearSearch) },
                        firstKeyFocusRequester = keyboardFocus,
                        onVoiceSearch = if (voiceAvailable) ::startVoiceSearch else null,
                    )
                }
            },
            overlay = {
                uiState.emptyMessage?.takeIf { uiState.hasQuery && !uiState.isSearching }?.let { message ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = shellContentStart(), bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CinemaColors.TextPrimary,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.search_no_results_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
                        )
                    }
                }
            },
        )
    }
}

/** The typed query in a rounded field with a static accent caret (no blink, to save redraws). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchQueryField(
    query: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .widthIn(min = SearchFieldMinWidth)
            .background(CinemaColors.Surface, CinemaShapes.Pill)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = query.ifBlank { hint }.isolateDirection(),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = if (query.isBlank()) CinemaColors.TextMuted else CinemaColors.White,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 28.dp)
                    .background(CinemaColors.Accent),
            )
        }
    }
}

private val SearchFieldMinWidth = 360.dp
