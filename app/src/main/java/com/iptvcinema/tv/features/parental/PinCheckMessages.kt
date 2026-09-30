package com.iptvcinema.tv.features.parental

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.parental.PinCheck

/** Text to show under the PIN dots, or null when the PIN was accepted. */
@Composable
fun PinCheck.messageOrNull(): String? = when (this) {
    PinCheck.Accepted -> null
    is PinCheck.Wrong -> stringResource(R.string.pin_error_wrong, triesLeft)
    is PinCheck.LockedOut -> stringResource(R.string.pin_error_locked, secondsLeft)
    PinCheck.Unavailable -> stringResource(R.string.pin_error_unavailable)
}

/** Like [messageOrNull], for code outside Compose. */
fun PinCheck.messageOrNull(appStrings: AppStrings): String? = when (this) {
    PinCheck.Accepted -> null
    is PinCheck.Wrong -> appStrings.get(R.string.pin_error_wrong, triesLeft)
    is PinCheck.LockedOut -> appStrings.get(R.string.pin_error_locked, secondsLeft)
    PinCheck.Unavailable -> appStrings.get(R.string.pin_error_unavailable)
}
