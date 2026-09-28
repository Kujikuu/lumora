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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

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
                .fillMaxWidth(0.5f)
                .background(CinemaColors.Surface, CinemaShapes.Large)
                .padding(CinemaSpacing.SectionGap),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
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
                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                CinemaButton(
                    text = cancelLabel,
                    variant = CinemaButtonVariant.SecondaryDark,
                    onClick = onDismiss,
                    enabled = !isWorking,
                    modifier = Modifier.focusRequester(cancelFocus),
                )
                CinemaButton(
                    text = confirmLabel,
                    variant = if (destructive) CinemaButtonVariant.Danger else CinemaButtonVariant.PrimaryAccent,
                    onClick = onConfirm,
                    enabled = !isWorking,
                )
            }
        }
    }
}
