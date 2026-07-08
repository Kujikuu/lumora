package com.iptvcinema.tv.features.settings

import androidx.annotation.StringRes
import com.iptvcinema.tv.R

enum class SettingsSection(@StringRes val labelRes: Int) {
    Account(R.string.settings_account),
    Playback(R.string.settings_playback),
    Language(R.string.settings_language),
    DevicePreferences(R.string.settings_device_preferences),
    ParentalControls(R.string.settings_parental_controls),
    Subscription(R.string.settings_subscription),
    Support(R.string.settings_support),
    About(R.string.settings_about),
    ;

    companion object {
        val menuSections: List<SettingsSection> = entries
    }
}
