package com.iptvcinema.tv.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferences {
    val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
    val USER_ID = stringPreferencesKey("user_id")
    val HAS_SOURCE = booleanPreferencesKey("has_source")
    val CURRENT_PROFILE_ID = stringPreferencesKey("current_profile_id")
    val CURRENT_SOURCE_ID = stringPreferencesKey("current_source_id")
    val SOURCE_TYPE = stringPreferencesKey("source_type")
    val IS_DEMO_MODE = booleanPreferencesKey("is_demo_mode")
    val SEARCH_KEYBOARD_LAYOUT = stringPreferencesKey("search_keyboard_layout")
}
