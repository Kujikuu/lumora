package com.iptvcinema.tv.features.settings

import androidx.annotation.StringRes
import com.iptvcinema.tv.R

enum class SettingsSection(@StringRes val labelRes: Int) {
    Account(R.string.settings_account),
    Subscription(R.string.settings_subscription),
    Playback(R.string.settings_playback),
    Language(R.string.settings_language),
    ParentalControls(R.string.settings_parental_controls),
    Notifications(R.string.settings_notifications),
    DevicePreferences(R.string.settings_device_preferences),
    About(R.string.settings_about),
    ;

    companion object {
        val menuSections: List<SettingsSection> = entries.filter { it != ParentalControls }
    }
}
