package com.iptvcinema.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptvcinema.launcher.LauncherGraph
import com.iptvcinema.launcher.R
import com.iptvcinema.launcher.content.AppLinks
import com.iptvcinema.launcher.inputs.InputTypeNames
import com.iptvcinema.launcher.system.SystemShortcuts
import com.iptvcinema.tv.core.design.theme.CinemaColors

/**
 * The home screen: a fixed header over six rows in a scrolling column. All rows stay composed
 * (about sixty small cards), so every row's focus requester is always attached and Up/Down can
 * never land nowhere.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel { HomeViewModel() }) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val labels = HomeLabels(
        openApp = stringResource(R.string.placeholder_open_app),
        installApp = stringResource(R.string.placeholder_install_app),
        emptyRow = stringResource(R.string.placeholder_empty_row),
        noInputs = stringResource(R.string.placeholder_no_inputs),
        inputTypeNames = remember(configuration) { InputTypeNames.of(context) },
    )
    LaunchedEffect(labels) { viewModel.setLabels(labels) }
    val flowRows by viewModel.rows.collectAsState()
    val rows = flowRows ?: remember { viewModel.initialRows(labels) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focus = remember { HomeFocusController(scope, scrollState, rowCount = RowKey.entries.size) }
    var menuCard by remember { mutableStateOf<HomeCard?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Focus is placed once, on the first row; Home while already home returns there.
    LaunchedEffect(Unit) { focus.toRow(0) }
    // When the focused row's cards are replaced (placeholders giving way to real rows, or a
    // refresh after watching something), the focused node may leave the composition and
    // Compose does not refocus on its own; put focus back on that row's entry card.
    val rowKeys = rows.map { row -> row.cards.map { it.key } }
    var lastRowKeys by remember { mutableStateOf(rowKeys) }
    LaunchedEffect(rowKeys) {
        val changedRow = focus.focusedRow
        val changed = changedRow >= 0 && rowKeys.getOrNull(changedRow) != lastRowKeys.getOrNull(changedRow)
        lastRowKeys = rowKeys
        if (changed) focus.restore()
    }
    LaunchedEffect(Unit) { LauncherGraph.homePresses.collect { focus.toRow(0) } }
    // A home activity must never finish on Back; Back goes to the top instead.
    BackHandler { if (focus.focusedRow != 0) focus.toRow(0) }

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume() }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val shortcuts = listOf(
        HeaderShortcut(stringResource(R.string.main_app_name)) { AppLinks.openApp(context) },
        HeaderShortcut(stringResource(R.string.shortcut_settings)) { SystemShortcuts.openSettings(context) },
        HeaderShortcut(stringResource(R.string.shortcut_network)) { SystemShortcuts.openNetwork(context) },
        HeaderShortcut(stringResource(R.string.shortcut_manage_apps)) { SystemShortcuts.openManageApps(context) },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
    ) {
        HomeHeader(
            entryRequester = focus.header,
            onHeaderFocused = focus::onHeaderFocused,
            onDown = { focus.toRow(0) },
            shortcuts = shortcuts,
        )
        // Rows are placed by HomeFocusController and HomeRail; the default focus bring-into-view
        // would fight those glides and leave rows half-aligned, so it is switched off here.
        CompositionLocalProvider(LocalBringIntoViewSpec provides NoBringIntoView) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState, enabled = false),
                verticalArrangement = Arrangement.spacedBy(HomeDimens.RowGap),
            ) {
                rows.forEachIndexed { index, row ->
                    HomeRail(
                        title = stringResource(row.key.titleRes()),
                        row = row,
                        entryRequester = focus.rows[index],
                        onRowFocused = { focus.onRowFocused(index) },
                        onUp = { if (index == 0) focus.toHeader() else focus.toRow(index - 1) },
                        onDown = { if (index < rows.lastIndex) focus.toRow(index + 1) },
                        onCardClick = { card -> runCardAction(card.action, context) },
                        onCardLongClick = { card -> if (card.action is CardAction.OpenApp) menuCard = card },
                        modifier = Modifier.onGloballyPositioned { focus.rowOffsets[index] = it.positionInParent().y.toInt() },
                    )
                }
                // Room below the last row so it can glide to the top like the others.
                Column(Modifier.padding(bottom = HomeDimens.LandscapeHeight * 3)) {}
            }
        }
    }

    menuCard?.let { card ->
        val action = card.action as CardAction.OpenApp
        AppActionsMenu(
            appTitle = card.title,
            isSystemApp = action.isSystem,
            onDismiss = {
                menuCard = null
                focus.restore()
            },
            onOpen = { LauncherGraph.apps.open(action.packageName) },
            onInfo = { LauncherGraph.apps.openInfo(action.packageName) },
            onUninstall = { LauncherGraph.apps.uninstall(action.packageName) },
        )
    }
}

private fun RowKey.titleRes(): Int = when (this) {
    RowKey.Continue -> R.string.row_continue
    RowKey.Favorites -> R.string.row_favorites
    RowKey.Live -> R.string.row_live
    RowKey.New -> R.string.row_new
    RowKey.Apps -> R.string.row_apps
    RowKey.Inputs -> R.string.row_inputs
}

private fun runCardAction(action: CardAction, context: android.content.Context) {
    when (action) {
        is CardAction.OpenLink -> AppLinks.openPlayLink(context, action.intentUri)
        is CardAction.OpenApp -> LauncherGraph.apps.open(action.packageName)
        is CardAction.OpenInput -> LauncherGraph.inputs.open(action.input)
        CardAction.OpenInputPicker -> LauncherGraph.inputs.openSystemPicker()
        CardAction.OpenMainApp -> AppLinks.openApp(context)
        CardAction.InstallMainApp -> AppLinks.openStorePage(context)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val NoBringIntoView = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}
