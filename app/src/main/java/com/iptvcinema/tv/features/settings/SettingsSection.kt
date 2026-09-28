package com.iptvcinema.tv.features.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.graphics.vector.ImageVector
import com.iptvcinema.tv.R

enum class SettingsSection(@StringRes val labelRes: Int, val icon: ImageVector) {
    Account(R.string.settings_account, Icons.Outlined.AccountCircle),
    Profiles(R.string.settings_profiles, Icons.Outlined.People),
    Playback(R.string.settings_playback, Icons.Outlined.PlayCircle),
    Language(R.string.settings_language, Icons.Outlined.Language),
    Sync(R.string.settings_sync, Icons.Outlined.Sync),
    DevicePreferences(R.string.settings_device_preferences, Icons.Outlined.Tv),
    ParentalControls(R.string.settings_parental_controls, Icons.Outlined.Lock),
    Support(R.string.settings_support, Icons.AutoMirrored.Outlined.HelpOutline),
    About(R.string.settings_about, Icons.Outlined.Info),
    ;

    companion object {
        val menuSections: List<SettingsSection> = entries
    }
}
