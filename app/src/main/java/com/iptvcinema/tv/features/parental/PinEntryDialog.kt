package com.iptvcinema.tv.features.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

enum class PinEntryMode {
    Verify,
    SetNew,
    ConfirmNew,
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PinEntryDialog(
    mode: PinEntryMode,
    title: String,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onPinComplete: (String) -> Unit,
) {
    var digits by remember(mode) { mutableStateOf("") }
    val firstKeyFocus = remember { FocusRequester() }

    LaunchedEffect(mode) {
        digits = ""
        firstKeyFocus.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .background(CinemaColors.Surface, CinemaShapes.Large)
                .padding(CinemaSpacing.SectionGap),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = stringResource(R.string.pin_enter_4_digit),
                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(PIN_LENGTH) { index ->
                    Text(
                        text = if (index < digits.length) "●" else "○",
                        style = MaterialTheme.typography.displaySmall.copy(color = CinemaColors.Gold),
                    )
                }
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.Danger),
                )
            }
            PinKeypad(
                firstKeyFocus = firstKeyFocus,
                onDigit = { digit ->
                    if (digits.length < PIN_LENGTH) {
                        val next = digits + digit
                        digits = next
                        if (next.length == PIN_LENGTH) {
                            onPinComplete(next)
                        }
                    }
                },
                onBackspace = {
                    if (digits.isNotEmpty()) digits = digits.dropLast(1)
                },
                onClear = { digits = "" },
            )
            CinemaButton(
                text = stringResource(R.string.btn_cancel),
                variant = CinemaButtonVariant.Ghost,
                onClick = onDismiss,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PinKeypad(
    firstKeyFocus: FocusRequester,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val clearLabel = stringResource(R.string.btn_clear)
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(clearLabel, "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEachIndexed { colIndex, key ->
                    FocusableCinemaCard(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (rowIndex == 0 && colIndex == 0) {
                                    Modifier.focusRequester(firstKeyFocus)
                                } else {
                                    Modifier
                                },
                            ),
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                clearLabel -> onClear()
                                else -> onDigit(key)
                            }
                        },
                        shape = CinemaShapes.Medium,
                    ) { _ ->
                        Text(
                            text = key,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = CinemaColors.TextPrimary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private const val PIN_LENGTH = 4
