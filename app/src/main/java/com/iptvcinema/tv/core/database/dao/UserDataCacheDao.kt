package com.iptvcinema.tv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.iptvcinema.tv.core.database.entity.CachedFavoriteEntity
import com.iptvcinema.tv.core.database.entity.CachedParentalControlsEntity
import com.iptvcinema.tv.core.database.entity.CachedPlaylistSourceEntity
import com.iptvcinema.tv.core.database.entity.CachedUserSettingsEntity
import com.iptvcinema.tv.core.database.entity.CachedWatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataCacheDao {
    @Query("SELECT * FROM cached_favorites WHERE profileId = :profileId ORDER BY createdAtEpochMs DESC")
    fun observeFavorites(profileId: String): Flow<List<CachedFavoriteEntity>>

    @Query("SELECT * FROM cached_favorites WHERE profileId = :profileId ORDER BY createdAtEpochMs DESC")
    suspend fun getFavorites(profileId: String): List<CachedFavoriteEntity>

    @Transaction
    suspend fun replaceFavorites(profileId: String, favorites: List<CachedFavoriteEntity>) {
        deleteFavorites(profileId)
        if (favorites.isNotEmpty()) {
            upsertFavorites(favorites)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorites(favorites: List<CachedFavoriteEntity>)

    @Query("DELETE FROM cached_favorites WHERE profileId = :profileId")
    suspend fun deleteFavorites(profileId: String)

    @Query(
        "SELECT * FROM cached_watch_history WHERE profileId = :profileId " +
            "ORDER BY lastWatchedAtEpochMs DESC LIMIT :limit",
    )
    suspend fun getWatchHistory(profileId: String, limit: Int): List<CachedWatchHistoryEntity>

    @Transaction
    suspend fun replaceWatchHistory(profileId: String, items: List<CachedWatchHistoryEntity>) {
        deleteWatchHistory(profileId)
        if (items.isNotEmpty()) {
            upsertWatchHistory(items)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(items: List<CachedWatchHistoryEntity>)

    @Query("DELETE FROM cached_watch_history WHERE profileId = :profileId")
    suspend fun deleteWatchHistory(profileId: String)

    @Query("SELECT * FROM cached_user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getUserSettings(userId: String): CachedUserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserSettings(settings: CachedUserSettingsEntity)

    @Query("SELECT * FROM cached_parental_controls WHERE profileId = :profileId LIMIT 1")
    suspend fun getParentalControls(profileId: String): CachedParentalControlsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParentalControls(controls: CachedParentalControlsEntity)

    @Query("SELECT * FROM cached_playlist_sources WHERE userId = :userId ORDER BY cachedAtEpochMs DESC")
    suspend fun getPlaylistSources(userId: String): List<CachedPlaylistSourceEntity>

    @Transaction
    suspend fun replacePlaylistSources(userId: String, sources: List<CachedPlaylistSourceEntity>) {
        deletePlaylistSources(userId)
        if (sources.isNotEmpty()) {
            upsertPlaylistSources(sources)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistSources(sources: List<CachedPlaylistSourceEntity>)

    @Query("DELETE FROM cached_playlist_sources WHERE userId = :userId")
    suspend fun deletePlaylistSources(userId: String)
}
