package com.iptvcinema.tv.core.util

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberPrototypeFeedback(): (String) -> Unit {
    val context = LocalContext.current
    return { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
