package com.iptvcinema.tv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity
import com.iptvcinema.tv.core.database.entity.LocalMovieEntity
import com.iptvcinema.tv.core.database.entity.LocalProgramEntity
import com.iptvcinema.tv.core.database.entity.LocalSeriesEntity
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE sourceId = :sourceId AND contentType = :contentType ORDER BY sortOrder, name")
    fun observeByType(sourceId: String, contentType: String): Flow<List<LocalCategoryEntity>>

    @Query("SELECT * FROM categories WHERE sourceId = :sourceId AND contentType = :contentType ORDER BY sortOrder, name")
    suspend fun getByType(sourceId: String, contentType: String): List<LocalCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalCategoryEntity>)

    @Query("DELETE FROM categories WHERE sourceId = :sourceId AND contentType = :contentType")
    suspend fun deleteByType(sourceId: String, contentType: String)

    @Query("DELETE FROM categories WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY sortOrder, channelNumber, name")
    fun observeAll(sourceId: String): Flow<List<LocalChannelEntity>>

    @Query(
        """
        SELECT * FROM channels
        WHERE sourceId = :sourceId AND categoryId = :categoryId
        ORDER BY sortOrder, channelNumber, name
        """,
    )
    fun observeByCategory(sourceId: String, categoryId: String): Flow<List<LocalChannelEntity>>

    @Query(
        """
        SELECT * FROM channels
        WHERE sourceId = :sourceId AND categoryName = :categoryName
        ORDER BY sortOrder, channelNumber, name
        """,
    )
    fun observeByCategoryName(sourceId: String, categoryName: String): Flow<List<LocalChannelEntity>>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY sortOrder LIMIT :limit")
    fun observeFeatured(sourceId: String, limit: Int): Flow<List<LocalChannelEntity>>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND id = :channelId LIMIT 1")
    suspend fun getById(sourceId: String, channelId: String): LocalChannelEntity?

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY sortOrder, channelNumber, name")
    suspend fun getAllOrdered(sourceId: String): List<LocalChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalChannelEntity>)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    suspend fun countBySource(sourceId: String): Int

    @Query(
        """
        SELECT * FROM channels
        WHERE sourceId = :sourceId AND name LIKE '%' || :query || '%'
        ORDER BY sortOrder, channelNumber, name
        LIMIT :limit
        """,
    )
    suspend fun searchByName(sourceId: String, query: String, limit: Int): List<LocalChannelEntity>
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE sourceId = :sourceId ORDER BY sortOrder, title")
    fun observeAll(sourceId: String): Flow<List<LocalMovieEntity>>

    @Query(
        """
        SELECT * FROM movies
        WHERE sourceId = :sourceId AND categoryId = :categoryId
        ORDER BY sortOrder, title
        """,
    )
    fun observeByCategory(sourceId: String, categoryId: String): Flow<List<LocalMovieEntity>>

    @Query(
        """
        SELECT * FROM movies
        WHERE sourceId = :sourceId AND categoryName = :categoryName
        ORDER BY sortOrder, title
        """,
    )
    fun observeByCategoryName(sourceId: String, categoryName: String): Flow<List<LocalMovieEntity>>

    @Query("SELECT * FROM movies WHERE sourceId = :sourceId ORDER BY sortOrder LIMIT :limit")
    fun observeFeatured(sourceId: String, limit: Int): Flow<List<LocalMovieEntity>>

    @Query(
        """
        SELECT * FROM movies
        WHERE sourceId = :sourceId
        ORDER BY
            CASE WHEN addedAt IS NULL THEN 0 ELSE 1 END DESC,
            addedAt DESC,
            sortOrder DESC
        LIMIT :limit
        """,
    )
    fun observeRecentlyAdded(sourceId: String, limit: Int): Flow<List<LocalMovieEntity>>

    @Query(
        """
        SELECT * FROM movies
        WHERE sourceId = :sourceId AND categoryId = :categoryId
        ORDER BY sortOrder, title
        LIMIT :limit
        """,
    )
    fun observeByCategoryLimited(
        sourceId: String,
        categoryId: String,
        limit: Int,
    ): Flow<List<LocalMovieEntity>>

    @Query("SELECT * FROM movies WHERE sourceId = :sourceId ORDER BY sortOrder, title LIMIT :limit")
    fun observeAllLimited(sourceId: String, limit: Int): Flow<List<LocalMovieEntity>>

    @Query("SELECT * FROM movies WHERE sourceId = :sourceId AND id = :movieId LIMIT 1")
    suspend fun getById(sourceId: String, movieId: String): LocalMovieEntity?

    @Query(
        """
        SELECT * FROM movies
        WHERE sourceId = :sourceId AND categoryId = :categoryId
        ORDER BY sortOrder, title
        LIMIT :limit
        """,
    )
    suspend fun getByCategory(sourceId: String, categoryId: String, limit: Int): List<LocalMovieEntity>

    @Query("SELECT * FROM movies WHERE sourceId = :sourceId ORDER BY sortOrder, title LIMIT :limit")
    suspend fun getFeatured(sourceId: String, limit: Int): List<LocalMovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalMovieEntity>)

    @Query("DELETE FROM movies WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM movies WHERE sourceId = :sourceId")
    suspend fun countBySource(sourceId: String): Int

    @Query(
        """
        SELECT * FROM movies
        WHERE sourceId = :sourceId
            AND (title LIKE '%' || :query || '%' OR "cast" LIKE '%' || :query || '%')
        ORDER BY title
        LIMIT :limit
        """,
    )
    suspend fun searchByTitle(sourceId: String, query: String, limit: Int): List<LocalMovieEntity>
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series WHERE sourceId = :sourceId ORDER BY sortOrder, title")
    fun observeAll(sourceId: String): Flow<List<LocalSeriesEntity>>

    @Query(
        """
        SELECT * FROM series
        WHERE sourceId = :sourceId AND categoryId = :categoryId
        ORDER BY sortOrder, title
        """,
    )
    fun observeByCategory(sourceId: String, categoryId: String): Flow<List<LocalSeriesEntity>>

    @Query(
        """
        SELECT * FROM series
        WHERE sourceId = :sourceId AND categoryName = :categoryName
        ORDER BY sortOrder, title
        """,
    )
    fun observeByCategoryName(sourceId: String, categoryName: String): Flow<List<LocalSeriesEntity>>

    @Query("SELECT * FROM series WHERE sourceId = :sourceId ORDER BY sortOrder LIMIT :limit")
    fun observeFeatured(sourceId: String, limit: Int): Flow<List<LocalSeriesEntity>>

    @Query("SELECT * FROM series WHERE sourceId = :sourceId AND id = :seriesId LIMIT 1")
    suspend fun getById(sourceId: String, seriesId: String): LocalSeriesEntity?

    @Query("SELECT id FROM series WHERE sourceId = :sourceId ORDER BY sortOrder, title LIMIT :limit")
    suspend fun getTopIds(sourceId: String, limit: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalSeriesEntity>)

    @Query("DELETE FROM series WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM series WHERE sourceId = :sourceId")
    suspend fun countBySource(sourceId: String): Int

    @Query(
        """
        SELECT * FROM series
        WHERE sourceId = :sourceId
            AND (title LIKE '%' || :query || '%' OR "cast" LIKE '%' || :query || '%')
        ORDER BY title
        LIMIT :limit
        """,
    )
    suspend fun searchByTitle(sourceId: String, query: String, limit: Int): List<LocalSeriesEntity>
}

@Dao
interface EpisodeDao {
    @Query(
        """
        SELECT * FROM episodes
        WHERE sourceId = :sourceId AND seriesId = :seriesId
        ORDER BY seasonNumber, episodeNumber
        """,
    )
    fun observeBySeries(sourceId: String, seriesId: String): Flow<List<LocalEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalEpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE sourceId = :sourceId AND id = :episodeId LIMIT 1")
    suspend fun getById(sourceId: String, episodeId: String): LocalEpisodeEntity?

    @Query(
        """
        SELECT * FROM episodes
        WHERE sourceId = :sourceId AND seriesId = :seriesId
        ORDER BY seasonNumber, episodeNumber
        """,
    )
    suspend fun getBySeries(sourceId: String, seriesId: String): List<LocalEpisodeEntity>

    @Query("DELETE FROM episodes WHERE sourceId = :sourceId AND seriesId = :seriesId")
    suspend fun deleteBySeries(sourceId: String, seriesId: String)

    @Query("DELETE FROM episodes WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)
}

@Dao
interface ProgramDao {
    @Query(
        """
        SELECT * FROM programs
        WHERE sourceId = :sourceId AND channelId = :channelId
        AND endEpochMs > :nowMs
        ORDER BY startEpochMs
        """,
    )
    suspend fun getUpcomingForChannel(sourceId: String, channelId: String, nowMs: Long): List<LocalProgramEntity>

    @Query(
        """
        SELECT * FROM programs
        WHERE sourceId = :sourceId AND channelId IN (:channelIds)
        AND startEpochMs <= :windowEndMs AND endEpochMs >= :windowStartMs
        ORDER BY channelId, startEpochMs
        """,
    )
    suspend fun getProgramsForChannels(
        sourceId: String,
        channelIds: List<String>,
        windowStartMs: Long,
        windowEndMs: Long,
    ): List<LocalProgramEntity>

    @Query(
        """
        SELECT * FROM programs
        WHERE sourceId = :sourceId AND channelId IN (:channelIds)
        AND startEpochMs <= :nowMs AND endEpochMs > :nowMs
        ORDER BY channelId, startEpochMs
        """,
    )
    suspend fun getCurrentProgramsForChannels(
        sourceId: String,
        channelIds: List<String>,
        nowMs: Long,
    ): List<LocalProgramEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocalProgramEntity>)

    @Query("DELETE FROM programs WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("DELETE FROM programs WHERE sourceId = :sourceId AND endEpochMs < :cutoffMs")
    suspend fun deleteOlderThan(sourceId: String, cutoffMs: Long)
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM source_sync_state WHERE sourceId = :sourceId LIMIT 1")
    fun observe(sourceId: String): Flow<LocalSourceSyncStateEntity?>

    @Query("SELECT * FROM source_sync_state WHERE sourceId = :sourceId LIMIT 1")
    suspend fun get(sourceId: String): LocalSourceSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: LocalSourceSyncStateEntity)

    @Query("DELETE FROM source_sync_state WHERE sourceId = :sourceId")
    suspend fun delete(sourceId: String)
}
