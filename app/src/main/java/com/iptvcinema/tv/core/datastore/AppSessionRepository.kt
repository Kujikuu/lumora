package com.iptvcinema.tv.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.iptvcinema.tv.core.model.SourceType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AppSessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val sessionState: Flow<AppSessionState> = dataStore.data.map { preferences ->
        AppSessionState(
            isAuthenticated = preferences[AppPreferences.IS_AUTHENTICATED] ?: false,
            userId = preferences[AppPreferences.USER_ID],
            hasSource = preferences[AppPreferences.HAS_SOURCE] ?: false,
            currentProfileId = preferences[AppPreferences.CURRENT_PROFILE_ID],
            currentSourceId = preferences[AppPreferences.CURRENT_SOURCE_ID],
            sourceType = preferences[AppPreferences.SOURCE_TYPE]?.let { runCatching { SourceType.valueOf(it) }.getOrNull() },
            isDemoMode = preferences[AppPreferences.IS_DEMO_MODE] ?: false,
        )
    }

    suspend fun setAuthenticated(authenticated: Boolean, userId: String? = null) {
        dataStore.edit { preferences ->
            preferences[AppPreferences.IS_AUTHENTICATED] = authenticated
            if (authenticated && userId != null) {
                preferences[AppPreferences.USER_ID] = userId
            } else if (!authenticated) {
                preferences.remove(AppPreferences.USER_ID)
            }
        }
    }

    suspend fun setSource(
        sourceId: String,
        sourceType: SourceType,
        isDemoMode: Boolean = sourceType == SourceType.DEMO,
    ) {
        dataStore.edit { preferences ->
            preferences[AppPreferences.HAS_SOURCE] = true
            preferences[AppPreferences.CURRENT_SOURCE_ID] = sourceId
            preferences[AppPreferences.SOURCE_TYPE] = sourceType.name
            preferences[AppPreferences.IS_DEMO_MODE] = isDemoMode
        }
    }

    suspend fun selectProfile(profileId: String) {
        dataStore.edit { preferences ->
            preferences[AppPreferences.CURRENT_PROFILE_ID] = profileId
        }
    }

    suspend fun clearSource() {
        dataStore.edit { preferences ->
            preferences[AppPreferences.HAS_SOURCE] = false
            preferences.remove(AppPreferences.CURRENT_SOURCE_ID)
            preferences.remove(AppPreferences.SOURCE_TYPE)
            preferences[AppPreferences.IS_DEMO_MODE] = false
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
