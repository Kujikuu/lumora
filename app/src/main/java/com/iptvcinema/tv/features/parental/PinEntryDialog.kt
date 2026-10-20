package com.iptvcinema.tv.features.parental

import com.iptvcinema.tv.core.navigation.requestFocusWhenReady
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.FocusableCardSurface
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.components.SettingsPalette
import com.iptvcinema.tv.core.design.theme.CinemaColors

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
        firstKeyFocus.requestFocusWhenReady()
    }

    fun addDigit(digit: String) {
        if (digits.length >= PIN_LENGTH) return
        val next = digits + digit
        if (next.length == PIN_LENGTH) {
            // Clear right away so a wrong PIN leaves empty dots and the next try can start.
            digits = ""
            onPinComplete(next)
        } else {
            digits = next
        }
    }

    fun removeDigit() {
        if (digits.isNotEmpty()) digits = digits.dropLast(1)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(PinPanelWidth)
                .background(CinemaColors.Surface, PinPanelShape)
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .onPreviewKeyEvent { event ->
                    // The remote's number keys type digits directly, without the keypad.
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val code = event.nativeKeyEvent.keyCode
                    when {
                        code in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                            addDigit((code - KeyEvent.KEYCODE_0).toString())
                            true
                        }
                        code in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                            addDigit((code - KeyEvent.KEYCODE_NUMPAD_0).toString())
                            true
                        }
                        code == KeyEvent.KEYCODE_DEL -> {
                            removeDigit()
                            true
                        }
                        else -> false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Black,
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.pin_enter_4_digit),
                    style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextMuted),
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                repeat(PIN_LENGTH) { index ->
                    PinDot(filled = index < digits.length)
                }
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = SettingsPalette.DangerText,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
            PinKeypad(
                firstKeyFocus = firstKeyFocus,
                onDigit = ::addDigit,
                onBackspace = ::removeDigit,
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
                            .height(PinKeyHeight)
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
                        shape = PinKeyShape,
                        focusedBorderWidth = 0.dp,
                        focusScale = PIN_KEY_FOCUS_SCALE,
                    ) { focused ->
                        val isDigit = key.length == 1 && key[0].isDigit()
                        FocusableCardSurface(
                            backgroundColor = if (focused) CinemaColors.White else CinemaColors.SurfaceRaised,
                            shape = PinKeyShape,
                        ) {
                            Text(
                                text = key,
                                style = (if (isDigit) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge).copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (focused) CinemaColors.Background else CinemaColors.TextPrimary,
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A filled dot per typed digit; an outlined ring for the rest. */
@Composable
private fun PinDot(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(PinDotSize)
            .then(
                if (filled) {
                    Modifier.background(CinemaColors.White, CircleShape)
                } else {
                    Modifier.border(2.dp, SettingsPalette.PinDotEmpty, CircleShape)
                },
            ),
    )
}

private const val PIN_LENGTH = 4
private const val PIN_KEY_FOCUS_SCALE = 1.04f
private val PinPanelWidth = 360.dp
private val PinPanelShape = RoundedCornerShape(22.dp)
private val PinKeyShape = RoundedCornerShape(14.dp)
private val PinKeyHeight = 48.dp
private val PinDotSize = 14.dp
