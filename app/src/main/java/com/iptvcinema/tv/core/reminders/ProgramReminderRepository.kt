package com.iptvcinema.tv.core.reminders

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iptvcinema.tv.core.util.safeSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "program_reminders",
)

/** Reminders live on the device only; they are tied to this TV's guide and clock. */
@Singleton
class ProgramReminderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(ProgramReminder.serializer())

    fun observe(profileId: String): Flow<List<ProgramReminder>> =
        context.reminderDataStore.data
            .map { prefs -> decode(prefs[key(profileId)]) }
            .distinctUntilChanged()

    /** Adds the reminder, or removes it if it was already set. Returns true when added. */
    suspend fun toggle(reminder: ProgramReminder): Boolean {
        var added = false
        context.reminderDataStore.edit { prefs ->
            val current = decode(prefs[key(reminder.profileId)])
            val updated = if (current.any { it.id == reminder.id }) {
                current.filterNot { it.id == reminder.id }
            } else {
                added = true
                current + reminder
            }
            prefs[key(reminder.profileId)] = json.encodeToString(serializer, updated)
        }
        return added
    }

    suspend fun remove(profileId: String, reminderId: String) {
        context.reminderDataStore.edit { prefs ->
            val updated = decode(prefs[key(profileId)]).filterNot { it.id == reminderId }
            prefs[key(profileId)] = json.encodeToString(serializer, updated)
        }
    }

    /** Drops reminders whose programme is long over, so the list never grows for ever. */
    suspend fun prune(profileId: String, nowMs: Long) {
        context.reminderDataStore.edit { prefs ->
            val current = decode(prefs[key(profileId)])
            val kept = ProgramReminderPolicy.prune(current, nowMs)
            if (kept.size != current.size) {
                prefs[key(profileId)] = json.encodeToString(serializer, kept)
            }
        }
    }

    private fun decode(raw: String?): List<ProgramReminder> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (error: Exception) {
            Log.w(TAG, "Dropping unreadable reminders: ${error.safeSummary()}")
            emptyList()
        }
    }

    private fun key(profileId: String) = stringPreferencesKey("reminders_$profileId")

    private companion object {
        const val TAG = "ProgramReminders"
    }
}
