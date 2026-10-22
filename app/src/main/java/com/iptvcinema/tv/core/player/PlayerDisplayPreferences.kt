package com.iptvcinema.tv.core.player

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.iptvcinema.tv.core.datastore.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Device-level picture settings; they describe the TV, so they are not synced per account. */
@Singleton
class PlayerDisplayPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun observeAspectMode(): Flow<VideoAspectMode> = dataStore.data
        .map { VideoAspectMode.fromStorage(it[AppPreferences.VIDEO_ASPECT_MODE]) }
        .distinctUntilChanged()

    suspend fun setAspectMode(mode: VideoAspectMode) {
        dataStore.edit { it[AppPreferences.VIDEO_ASPECT_MODE] = mode.storageValue }
    }
}
