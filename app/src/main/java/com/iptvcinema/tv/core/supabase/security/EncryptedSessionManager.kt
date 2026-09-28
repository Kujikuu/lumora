package com.iptvcinema.tv.core.supabase.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Stores the Supabase session (access + refresh token) in EncryptedSharedPreferences
 * instead of the library default, which is plain SharedPreferences.
 *
 * On first load it moves a session saved by the old default manager, so existing users
 * stay signed in. If the Keystore is broken (seen on some low-end TVs) it falls back to
 * the default manager rather than logging the user out.
 */
class EncryptedSessionManager(
    private val context: Context,
    private val legacy: SessionManager,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionManager {
    private val mutex = Mutex()
    private var preferences: SharedPreferences? = null
    private var encryptionUnavailable = false

    override suspend fun saveSession(session: UserSession) {
        val prefs = encryptedPreferences() ?: return legacy.saveSession(session)
        withContext(Dispatchers.IO) {
            prefs.edit().putString(KEY_SESSION, json.encodeToString(UserSession.serializer(), session)).apply()
        }
    }

    override suspend fun loadSession(): UserSession? {
        val prefs = encryptedPreferences() ?: return legacy.loadSession()
        val stored = withContext(Dispatchers.IO) { prefs.getString(KEY_SESSION, null) }
        if (stored != null) {
            return runCatching { json.decodeFromString(UserSession.serializer(), stored) }
                .onFailure { Log.w(TAG, "Stored session unreadable", it) }
                .getOrNull()
        }
        return migrateLegacySession()
    }

    override suspend fun deleteSession() {
        encryptedPreferences()?.let { prefs ->
            withContext(Dispatchers.IO) { prefs.edit().remove(KEY_SESSION).apply() }
        }
        runCatching { legacy.deleteSession() }
    }

    private suspend fun migrateLegacySession(): UserSession? {
        val session = runCatching { legacy.loadSession() }.getOrNull() ?: return null
        saveSession(session)
        runCatching { legacy.deleteSession() }
        return session
    }

    private suspend fun encryptedPreferences(): SharedPreferences? = mutex.withLock {
        if (encryptionUnavailable) return null
        preferences?.let { return it }
        withContext(Dispatchers.IO) {
            runCatching {
                EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }.onFailure {
                Log.w(TAG, "Encrypted storage unavailable, using default session storage", it)
                encryptionUnavailable = true
            }.getOrNull()
        }?.also { preferences = it }
    }

    companion object {
        /**
         * The library's own default storage (plain SharedPreferences). Its key is derived from
         * the project URL, so the old session is only found with exactly this key.
         */
        fun legacyFor(supabaseUrl: String): SessionManager =
            SettingsSessionManager(key = legacySessionKey(supabaseUrl))

        /** Same rule as supabase-kt 3.x createDefaultSessionManager (internal there). */
        fun legacySessionKey(supabaseUrl: String): String =
            "sb-" + supabaseUrl.removeSuffix("/").replace('/', '-').replace('.', '-') + "-session"

        private const val TAG = "EncryptedSessionManager"
        private const val FILE_NAME = "supabase_session_secure"
        private const val KEY_SESSION = "session"
    }
}
