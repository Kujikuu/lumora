package com.iptvcinema.tv.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.iptvcinema.tv.R
import com.iptvcinema.tv.features.parental.PinEntryDialog
import com.iptvcinema.tv.features.parental.PinEntryMode

@Composable
fun ParentalProtectedNavigation(
    requiresPin: Boolean,
    onVerifyPin: (String) -> Boolean,
    onAllowed: () -> Unit,
    content: @Composable (requestProtectedNavigation: () -> Unit) -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val incorrectPinMessage = stringResource(R.string.error_incorrect_pin)

    if (showPinDialog) {
        PinEntryDialog(
            mode = PinEntryMode.Verify,
            title = stringResource(R.string.pin_enter),
            errorMessage = pinError,
            onDismiss = {
                showPinDialog = false
                pinError = null
            },
            onPinComplete = { pin ->
                if (onVerifyPin(pin)) {
                    showPinDialog = false
                    pinError = null
                    onAllowed()
                } else {
                    pinError = incorrectPinMessage
                }
            },
        )
    }

    content {
        if (requiresPin) {
            showPinDialog = true
        } else {
            onAllowed()
        }
    }
}
