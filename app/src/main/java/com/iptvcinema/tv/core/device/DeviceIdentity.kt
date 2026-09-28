package com.iptvcinema.tv.core.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun interface DeviceIdentity {
    fun deviceName(): String
}

/** The name the user gave this TV (e.g. "Living room TV"), falling back to the model. */
class AndroidDeviceIdentity @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceIdentity {
    override fun deviceName(): String {
        val userName = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()
        if (!userName.isNullOrBlank()) return userName.trim()
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        val name = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        return name.trim().ifBlank { "Android TV" }
    }
}
