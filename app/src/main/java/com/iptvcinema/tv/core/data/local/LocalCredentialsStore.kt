package com.iptvcinema.tv.core.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.XtreamCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StoredXtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val accountName: String,
)

@Serializable
private data class StoredM3uCredentials(
    val playlistUrl: String,
    val epgUrl: String?,
    val playlistName: String,
    val userAgent: String?,
    val referer: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
)

@Singleton
class LocalCredentialsStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {
    // Keystore and EncryptedSharedPreferences setup takes 100-300 ms on a weak TV, so it is not
    // done in the constructor (which ran on the main thread at startup). The app warms it on IO
    // with [warmUp]; the lazy lock makes any earlier caller wait for that same instance.
    private val preferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Opens the encrypted store off the main thread, ahead of the first real read. */
    suspend fun warmUp() {
        withContext(Dispatchers.IO) { preferences }
    }

    fun saveXtreamCredentials(sourceId: String, credentials: XtreamCredentials) {
        preferences.edit()
            .putString(xtreamKey(sourceId), json.encodeToString(credentials.toStored()))
            .apply()
    }

    fun saveM3uCredentials(sourceId: String, credentials: M3uCredentials) {
        preferences.edit()
            .putString(m3uKey(sourceId), json.encodeToString(credentials.toStored()))
            .apply()
    }

    fun getXtreamCredentials(sourceId: String): XtreamCredentials? =
        preferences.getString(xtreamKey(sourceId), null)?.let {
            json.decodeFromString<StoredXtreamCredentials>(it).toDomain()
        }

    fun getM3uCredentials(sourceId: String): M3uCredentials? =
        preferences.getString(m3uKey(sourceId), null)?.let {
            json.decodeFromString<StoredM3uCredentials>(it).toDomain()
        }

    fun removeCredentials(sourceId: String) {
        preferences.edit()
            .remove(xtreamKey(sourceId))
            .remove(m3uKey(sourceId))
            .apply()
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    private fun xtreamKey(sourceId: String) = "xtream_$sourceId"
    private fun m3uKey(sourceId: String) = "m3u_$sourceId"

    private fun XtreamCredentials.toStored() = StoredXtreamCredentials(
        serverUrl = serverUrl,
        username = username,
        password = password,
        accountName = accountName,
    )

    private fun StoredXtreamCredentials.toDomain() = XtreamCredentials(
        serverUrl = serverUrl,
        username = username,
        password = password,
        accountName = accountName,
    )

    private fun M3uCredentials.toStored() = StoredM3uCredentials(
        playlistUrl = playlistUrl,
        epgUrl = epgUrl,
        playlistName = playlistName,
        userAgent = userAgent,
        referer = referer,
        customHeaders = customHeaders,
    )

    private fun StoredM3uCredentials.toDomain() = M3uCredentials(
        playlistUrl = playlistUrl,
        epgUrl = epgUrl,
        playlistName = playlistName,
        userAgent = userAgent,
        referer = referer,
        customHeaders = customHeaders,
    )

    companion object {
        private const val FILE_NAME = "iptv_source_credentials"
    }
}
