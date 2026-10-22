package com.iptvcinema.launcher.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.iptvcinema.launcher.R
import com.iptvcinema.tv.core.design.components.CinemaContextMenuDialog
import com.iptvcinema.tv.core.design.components.CinemaMenuOption

/** Long-press menu for an app card: open, app info, uninstall (not for system apps). */
@Composable
fun AppActionsMenu(
    appTitle: String,
    isSystemApp: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val options = buildList {
        add(CinemaMenuOption(OPEN, stringResource(R.string.app_action_open)))
        add(CinemaMenuOption(INFO, stringResource(R.string.app_action_info)))
        if (!isSystemApp) add(CinemaMenuOption(UNINSTALL, stringResource(R.string.app_action_uninstall), destructive = true))
    }
    CinemaContextMenuDialog(
        title = appTitle,
        options = options,
        onDismiss = onDismiss,
        onOptionSelected = { option ->
            when (option.id) {
                OPEN -> onOpen()
                INFO -> onInfo()
                UNINSTALL -> onUninstall()
            }
            onDismiss()
        },
    )
}

private const val OPEN = "open"
private const val INFO = "info"
private const val UNINSTALL = "uninstall"
