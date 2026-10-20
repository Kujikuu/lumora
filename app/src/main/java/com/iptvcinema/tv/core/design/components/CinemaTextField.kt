package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

private val FieldShape = CinemaShapes.XLarge
private val FieldFocusBorder = 2.dp

/**
 * Single-line text input: a Surface fill with a 12 dp radius that gains a 2 dp white ring when
 * focused. With [showLabelAbove] the label sits above the field as a caption instead of
 * appearing inside it as a placeholder.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    showLabelAbove: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.hasFocus },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (showLabelAbove) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isFocused) CinemaColors.TextSecondary else CinemaColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinemaColors.Surface, FieldShape)
                .then(
                    if (isFocused) {
                        Modifier.border(FieldFocusBorder, CinemaColors.FocusBorder, FieldShape)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = CinemaColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(CinemaColors.White),
                singleLine = true,
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty() && !showLabelAbove) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextMuted),
                            maxLines = 1,
                        )
                    }
                    inner()
                },
                visualTransformation = if (isPassword) {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
            )
        }
    }
}
