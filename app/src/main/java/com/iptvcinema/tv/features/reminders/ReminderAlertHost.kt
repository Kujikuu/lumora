package com.iptvcinema.tv.features.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaConfirmDialog
import com.iptvcinema.tv.core.navigation.navigateToLiveChannel
import com.iptvcinema.tv.core.reminders.ProgramReminderAlerts
import com.iptvcinema.tv.core.reminders.ProgramReminderPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReminderAlertViewModel @Inject constructor(
    private val alerts: ProgramReminderAlerts,
) : ViewModel() {
    val current = alerts.current
    fun dismiss() = alerts.dismiss()
}

/** Pops the "starting now" card for a due reminder over whatever screen is open. */
@Composable
fun ReminderAlertHost(
    navController: NavController,
    viewModel: ReminderAlertViewModel = hiltViewModel(),
) {
    val reminder by viewModel.current.collectAsState()
    val due = reminder ?: return
    // Raised while the TV was on the launcher or in standby and never seen: not worth showing now.
    if (due.startEpochMs + ProgramReminderPolicy.GRACE_MS < System.currentTimeMillis()) {
        LaunchedEffect(due.id) { viewModel.dismiss() }
        return
    }
    CinemaConfirmDialog(
        title = stringResource(R.string.reminder_alert_title),
        message = stringResource(R.string.reminder_alert_body, due.title, due.channelName),
        confirmLabel = stringResource(R.string.reminder_watch),
        cancelLabel = stringResource(R.string.reminder_dismiss),
        onConfirm = {
            viewModel.dismiss()
            navController.navigateToLiveChannel(due.channelId)
        },
        onDismiss = viewModel::dismiss,
    )
}
