package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors

/**
 * Yes/no dialog for actions that are hard to undo. Focus starts on Cancel so an accidental
 * OK press on the remote never triggers the action.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    isWorking: Boolean = false,
) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { cancelFocus.requestFocus() } }

    Dialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(CONFIRM_DIALOG_WIDTH_FRACTION)
                .background(CinemaColors.Surface, DialogPanelShape)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogPillAction(
                    label = cancelLabel,
                    onClick = onDismiss,
                    enabled = !isWorking,
                    restBackground = CinemaColors.OverlayFill,
                    modifier = Modifier.focusRequester(cancelFocus),
                )
                DialogPillAction(
                    label = confirmLabel,
                    onClick = onConfirm,
                    enabled = !isWorking,
                    restBackground = if (destructive) SettingsPalette.DangerTint else CinemaColors.Accent,
                    restContent = if (destructive) SettingsPalette.DangerText else CinemaColors.White,
                    focusedContent = if (destructive) CinemaColors.Danger else CinemaColors.Background,
                )
            }
        }
    }
}

private const val CONFIRM_DIALOG_WIDTH_FRACTION = 0.42f
