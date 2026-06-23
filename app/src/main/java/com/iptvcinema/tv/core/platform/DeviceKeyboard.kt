package com.iptvcinema.tv.core.platform

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

fun hasMultipleInputLanguages(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val locales = imm.enabledInputMethodList
        .flatMap { ime -> imm.getEnabledInputMethodSubtypeList(ime, true) }
        .mapNotNull { subtype -> subtype.languageTag.takeIf { it.isNotBlank() } }
        .distinct()
    return locales.size > 1
}

@Composable
fun rememberHasMultipleInputLanguages(): Boolean {
    val context = LocalContext.current
    return remember(context) { hasMultipleInputLanguages(context) }
}

fun showDeviceKeyboard(
    focusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
) {
    focusRequester.requestFocus()
    keyboardController?.show()
}

@Composable
fun rememberSoftwareKeyboardController() = LocalSoftwareKeyboardController.current
