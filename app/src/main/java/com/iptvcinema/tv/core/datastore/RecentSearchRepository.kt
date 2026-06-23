package com.iptvcinema.tv.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.recentSearchDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "recent_searches",
)

@Singleton
class RecentSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getRecentSearches(profileId: String, limit: Int = 5): List<String> {
        val key = recentKey(profileId)
        return context.recentSearchDataStore.data.map { prefs ->
            prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty().take(limit)
        }.first()
    }

    suspend fun addRecentSearch(profileId: String, query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val key = recentKey(profileId)
        context.recentSearchDataStore.edit { prefs ->
            val existing = prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
            val updated = listOf(trimmed) + existing.filter { !it.equals(trimmed, ignoreCase = true) }
            prefs[key] = updated.take(MAX_STORED).joinToString(SEPARATOR)
        }
    }

    private fun recentKey(profileId: String) = stringPreferencesKey("recent_$profileId")

    companion object {
        private const val SEPARATOR = "\u001F"
        private const val MAX_STORED = 8
    }
}
