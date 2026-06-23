package com.iptvcinema.tv.features.parental

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.parental.ParentalGate

@Composable
fun ParentalLockedAction(
    controls: ParentalControls?,
    parentalGate: ParentalGate,
    requiresPin: (ParentalControls) -> Boolean,
    onAllowed: () -> Unit,
    content: @Composable (requestAccess: () -> Unit) -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val incorrectPinMessage = stringResource(R.string.error_incorrect_pin)

    if (showPinDialog && controls != null) {
        PinEntryDialog(
            mode = PinEntryMode.Verify,
            title = stringResource(R.string.pin_enter),
            errorMessage = pinError,
            onDismiss = {
                showPinDialog = false
                pinError = null
            },
            onPinComplete = { pin ->
                if (parentalGate.verifyPin(controls, pin)) {
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
        val ctrl = controls
        if (ctrl != null && requiresPin(ctrl) && !parentalGate.isPinVerified(ctrl.profileId)) {
            showPinDialog = true
        } else {
            onAllowed()
        }
    }
}
