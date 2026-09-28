package com.iptvcinema.tv.features.settings

/** What the Account panel shows. Null fields are unknown, never faked. */
data class SettingsAccountUi(
    val deviceName: String,
    val displayName: String? = null,
    val email: String? = null,
    val isCloudAccount: Boolean = true,
)
