package com.iptvcinema.tv.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.iptvcinema.tv.core.database.dao.CategoryDao
import com.iptvcinema.tv.core.database.dao.ChannelDao
import com.iptvcinema.tv.core.database.dao.EpisodeDao
import com.iptvcinema.tv.core.database.dao.MovieDao
import com.iptvcinema.tv.core.database.dao.ProgramDao
import com.iptvcinema.tv.core.database.dao.SeriesDao
import com.iptvcinema.tv.core.database.dao.SyncStateDao
import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity
import com.iptvcinema.tv.core.database.entity.LocalMovieEntity
import com.iptvcinema.tv.core.database.entity.LocalProgramEntity
import com.iptvcinema.tv.core.database.entity.LocalSeriesEntity
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [
        LocalCategoryEntity::class,
        LocalChannelEntity::class,
        LocalMovieEntity::class,
        LocalSeriesEntity::class,
        LocalEpisodeEntity::class,
        LocalProgramEntity::class,
        LocalSourceSyncStateEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun programDao(): ProgramDao
    abstract fun syncStateDao(): SyncStateDao
}

@Singleton
class CatalogDaoFacade @Inject constructor(
    private val database: IptvDatabase,
) {
    val categories get() = database.categoryDao()
    val channels get() = database.channelDao()
    val movies get() = database.movieDao()
    val series get() = database.seriesDao()
    val episodes get() = database.episodeDao()
    val programs get() = database.programDao()
    val syncState get() = database.syncStateDao()

    @Transaction
    suspend fun replaceLiveCatalog(
        sourceId: String,
        categories: List<LocalCategoryEntity>,
        channels: List<LocalChannelEntity>,
    ) {
        database.categoryDao().deleteByType(sourceId, LIVE)
        database.channelDao().deleteBySource(sourceId)
        if (categories.isNotEmpty()) database.categoryDao().upsertAll(categories)
        if (channels.isNotEmpty()) database.channelDao().upsertAll(channels)
    }

    @Transaction
    suspend fun replaceVodCatalog(
        sourceId: String,
        categories: List<LocalCategoryEntity>,
        movies: List<LocalMovieEntity>,
    ) {
        database.categoryDao().deleteByType(sourceId, VOD)
        database.movieDao().deleteBySource(sourceId)
        if (categories.isNotEmpty()) database.categoryDao().upsertAll(categories)
        if (movies.isNotEmpty()) database.movieDao().upsertAll(movies)
    }

    @Transaction
    suspend fun replaceSeriesCatalog(
        sourceId: String,
        categories: List<LocalCategoryEntity>,
        series: List<LocalSeriesEntity>,
    ) {
        database.categoryDao().deleteByType(sourceId, SERIES)
        database.seriesDao().deleteBySource(sourceId)
        if (categories.isNotEmpty()) database.categoryDao().upsertAll(categories)
        if (series.isNotEmpty()) database.seriesDao().upsertAll(series)
    }

    @Transaction
    suspend fun replacePrograms(sourceId: String, programs: List<LocalProgramEntity>) {
        database.programDao().deleteBySource(sourceId)
        if (programs.isNotEmpty()) database.programDao().upsertAll(programs)
        val cutoffMs = System.currentTimeMillis() - com.iptvcinema.tv.core.epg.EpgSyncRepository.TRIM_PAST_MS
        database.programDao().deleteOlderThan(sourceId, cutoffMs)
    }

    @Transaction
    suspend fun purgeSource(sourceId: String) {
        database.categoryDao().deleteBySource(sourceId)
        database.channelDao().deleteBySource(sourceId)
        database.movieDao().deleteBySource(sourceId)
        database.seriesDao().deleteBySource(sourceId)
        database.episodeDao().deleteBySource(sourceId)
        database.programDao().deleteBySource(sourceId)
        database.syncStateDao().delete(sourceId)
    }

    companion object {
        const val LIVE = "LIVE"
        const val VOD = "VOD"
        const val SERIES = "SERIES"
    }
}
