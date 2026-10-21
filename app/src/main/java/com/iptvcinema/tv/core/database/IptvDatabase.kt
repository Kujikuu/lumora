package com.iptvcinema.tv.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.iptvcinema.tv.core.catalog.CatalogChangeSummary
import com.iptvcinema.tv.core.catalog.CatalogSyncPolicy
import com.iptvcinema.tv.core.database.dao.CategoryDao
import com.iptvcinema.tv.core.database.dao.ChannelDao
import com.iptvcinema.tv.core.database.dao.EpisodeDao
import com.iptvcinema.tv.core.database.dao.MovieDao
import com.iptvcinema.tv.core.database.dao.ProgramDao
import com.iptvcinema.tv.core.database.dao.SeriesDao
import com.iptvcinema.tv.core.database.dao.SyncStateDao
import com.iptvcinema.tv.core.database.dao.CatalogSyncMetadataDao
import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity
import com.iptvcinema.tv.core.database.entity.LocalMovieEntity
import com.iptvcinema.tv.core.database.entity.LocalProgramEntity
import com.iptvcinema.tv.core.database.entity.LocalSeriesEntity
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import com.iptvcinema.tv.core.database.entity.CatalogSyncMetadataEntity
import com.iptvcinema.tv.core.database.dao.UserDataCacheDao
import com.iptvcinema.tv.core.database.entity.CachedFavoriteEntity
import com.iptvcinema.tv.core.database.entity.CachedParentalControlsEntity
import com.iptvcinema.tv.core.database.entity.CachedPlaylistSourceEntity
import com.iptvcinema.tv.core.database.entity.CachedUserSettingsEntity
import com.iptvcinema.tv.core.database.entity.CachedWatchHistoryEntity
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
        CatalogSyncMetadataEntity::class,
        CachedFavoriteEntity::class,
        CachedWatchHistoryEntity::class,
        CachedUserSettingsEntity::class,
        CachedParentalControlsEntity::class,
        CachedPlaylistSourceEntity::class,
    ],
    version = 11,
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
    abstract fun catalogSyncMetadataDao(): CatalogSyncMetadataDao
    abstract fun userDataCacheDao(): UserDataCacheDao
}

data class CatalogReconcileResult(
    val changes: CatalogChangeSummary,
    val liveChannelCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val epgAvailable: Boolean,
)

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
    val syncMetadata get() = database.catalogSyncMetadataDao()

    suspend fun replaceLiveCatalog(
        sourceId: String,
        categories: List<LocalCategoryEntity>,
        channels: List<LocalChannelEntity>,
    ) = database.withTransaction {
        database.categoryDao().deleteByType(sourceId, LIVE)
        database.channelDao().deleteBySource(sourceId)
        if (categories.isNotEmpty()) database.categoryDao().upsertAll(categories)
        if (channels.isNotEmpty()) database.channelDao().upsertAll(channels)
    }

    suspend fun replaceVodCatalog(
        sourceId: String,
        categories: List<LocalCategoryEntity>,
        movies: List<LocalMovieEntity>,
    ) = database.withTransaction {
        database.categoryDao().deleteByType(sourceId, VOD)
        database.movieDao().deleteBySource(sourceId)
        if (categories.isNotEmpty()) database.categoryDao().upsertAll(categories)
        if (movies.isNotEmpty()) database.movieDao().upsertAll(movies)
    }

    suspend fun replaceSeriesCatalog(
        sourceId: String,
        categories: List<LocalCategoryEntity>,
        series: List<LocalSeriesEntity>,
    ) = database.withTransaction {
        database.categoryDao().deleteByType(sourceId, SERIES)
        database.seriesDao().deleteBySource(sourceId)
        if (categories.isNotEmpty()) database.categoryDao().upsertAll(categories)
        if (series.isNotEmpty()) database.seriesDao().upsertAll(series)
    }

    suspend fun replacePrograms(sourceId: String, programs: List<LocalProgramEntity>) {
        database.withTransaction {
            database.programDao().deleteBySource(sourceId)
            if (programs.isNotEmpty()) database.programDao().upsertAll(programs)
            val cutoffMs = System.currentTimeMillis() - com.iptvcinema.tv.core.epg.EpgSyncRepository.TRIM_PAST_MS
            database.programDao().deleteOlderThan(sourceId, cutoffMs)
        }
    }

    /** Swaps one series' episodes in a single transaction, so a cancel never leaves it empty. */
    suspend fun replaceSeriesEpisodes(
        sourceId: String,
        seriesId: String,
        episodes: List<LocalEpisodeEntity>,
    ) = database.withTransaction {
        database.episodeDao().deleteBySeries(sourceId, seriesId)
        if (episodes.isNotEmpty()) database.episodeDao().upsertAll(episodes)
    }

    suspend fun purgeSource(sourceId: String) = database.withTransaction {
        database.categoryDao().deleteBySource(sourceId)
        database.channelDao().deleteBySource(sourceId)
        database.movieDao().deleteBySource(sourceId)
        database.seriesDao().deleteBySource(sourceId)
        database.episodeDao().deleteBySource(sourceId)
        database.programDao().deleteBySource(sourceId)
        database.syncStateDao().delete(sourceId)
        database.catalogSyncMetadataDao().deleteBySource(sourceId)
    }

    /**
     * Applies one sync's catalog to the database.
     *
     * Each resource (live, VOD, series) commits on its own, and very large ones in groups (see
     * [CatalogItemReconciler]), instead of one transaction for the whole sync. With WAL, readers
     * never wait for these writes; this keeps other writers from waiting for the whole sync too.
     * The sync metadata and counts are saved last, so a sync that stops half way is redone in
     * full next time and ends in the same state.
     */
    suspend fun reconcileCatalog(
        sourceId: String,
        liveCategories: List<LocalCategoryEntity>?,
        channels: List<LocalChannelEntity>?,
        vodCategories: List<LocalCategoryEntity>?,
        movies: List<LocalMovieEntity>?,
        seriesCategories: List<LocalCategoryEntity>?,
        seriesItems: List<LocalSeriesEntity>?,
        metadata: List<CatalogSyncMetadataEntity>,
        syncedAtEpochMs: Long,
    ): CatalogReconcileResult {
        val liveChanges = reconcileResource(
            sourceId = sourceId,
            contentType = LIVE,
            categories = liveCategories,
            items = channels,
            reconcileItems = ::reconcileChannels,
            syncCategoryNames = { database.channelDao().syncCategoryNames(sourceId) },
        )
        val vodChanges = reconcileResource(
            sourceId = sourceId,
            contentType = VOD,
            categories = vodCategories,
            items = movies,
            reconcileItems = ::reconcileMovies,
            syncCategoryNames = { database.movieDao().syncCategoryNames(sourceId) },
        )
        val seriesChanges = reconcileResource(
            sourceId = sourceId,
            contentType = SERIES,
            categories = seriesCategories,
            items = seriesItems,
            reconcileItems = ::reconcileSeries,
            syncCategoryNames = { database.seriesDao().syncCategoryNames(sourceId) },
        )
        return database.withTransaction {
            if (metadata.isNotEmpty()) database.catalogSyncMetadataDao().upsertAll(metadata)
            val liveChannelCount = database.channelDao().countBySource(sourceId)
            val movieCount = database.movieDao().countBySource(sourceId)
            val seriesCount = database.seriesDao().countBySource(sourceId)
            val epgAvailable = database.syncStateDao().get(sourceId)?.epgAvailable ?: false
            database.syncStateDao().upsert(
                LocalSourceSyncStateEntity(
                    sourceId = sourceId,
                    lastSyncedAtEpochMs = syncedAtEpochMs,
                    liveChannelCount = liveChannelCount,
                    movieCount = movieCount,
                    seriesCount = seriesCount,
                    epgAvailable = epgAvailable,
                    lastError = null,
                ),
            )
            CatalogReconcileResult(
                changes = liveChanges + vodChanges + seriesChanges,
                liveChannelCount = liveChannelCount,
                movieCount = movieCount,
                seriesCount = seriesCount,
                epgAvailable = epgAvailable,
            )
        }
    }

    /**
     * Categories first, in their own short transaction; then the items. When only the categories
     * came in, existing items take the new category names in that same transaction.
     */
    private suspend fun <T> reconcileResource(
        sourceId: String,
        contentType: String,
        categories: List<LocalCategoryEntity>?,
        items: List<T>?,
        reconcileItems: suspend (String, List<T>) -> CatalogChangeSummary,
        syncCategoryNames: suspend () -> Int,
    ): CatalogChangeSummary {
        val renamed = database.withTransaction {
            val categoriesChanged = categories?.let { reconcileCategories(sourceId, contentType, it) } == true
            if (items == null && categoriesChanged) syncCategoryNames() else 0
        }
        val itemChanges = items?.let { reconcileItems(sourceId, it) } ?: CatalogChangeSummary()
        return itemChanges + CatalogChangeSummary(updated = renamed)
    }

    private suspend fun reconcileCategories(
        sourceId: String,
        contentType: String,
        incoming: List<LocalCategoryEntity>,
    ): Boolean {
        val uniqueIncoming = incoming.associateBy { it.id }.values.toList()
        val existing = database.categoryDao().getByType(sourceId, contentType).associateBy { it.id }
        val incomingIds = uniqueIncoming.mapTo(hashSetOf()) { it.id }
        val changed = uniqueIncoming.filter { existing[it.id] != it }
        val removed = existing.keys - incomingIds
        CatalogSyncPolicy.databaseBatches(removed).forEach { chunk ->
            if (chunk.isNotEmpty()) database.categoryDao().deleteByIds(sourceId, contentType, chunk)
        }
        CatalogSyncPolicy.databaseBatches(changed).forEach { chunk ->
            database.categoryDao().upsertAll(chunk)
        }
        return changed.isNotEmpty() || removed.isNotEmpty()
    }

    private suspend fun reconcileChannels(
        sourceId: String,
        incoming: List<LocalChannelEntity>,
    ): CatalogChangeSummary = CatalogItemReconciler.reconcile(
        incoming = incoming,
        idOf = LocalChannelEntity::id,
        existingIds = { database.channelDao().getIdsBySource(sourceId) },
        existingByIds = { database.channelDao().getByIds(sourceId, it) },
        upsert = database.channelDao()::upsertAll,
        delete = { database.channelDao().deleteByIds(sourceId, it) },
        inTransaction = ::inTransaction,
    )

    private suspend fun reconcileMovies(
        sourceId: String,
        incoming: List<LocalMovieEntity>,
    ): CatalogChangeSummary = CatalogItemReconciler.reconcile(
        incoming = incoming,
        idOf = LocalMovieEntity::id,
        existingIds = { database.movieDao().getIdsBySource(sourceId) },
        existingByIds = { database.movieDao().getByIds(sourceId, it) },
        upsert = database.movieDao()::upsertAll,
        delete = { database.movieDao().deleteByIds(sourceId, it) },
        inTransaction = ::inTransaction,
    )

    private suspend fun reconcileSeries(
        sourceId: String,
        incoming: List<LocalSeriesEntity>,
    ): CatalogChangeSummary = CatalogItemReconciler.reconcile(
        incoming = incoming,
        idOf = LocalSeriesEntity::id,
        existingIds = { database.seriesDao().getIdsBySource(sourceId) },
        existingByIds = { database.seriesDao().getByIds(sourceId, it) },
        upsert = database.seriesDao()::upsertAll,
        delete = { ids ->
            database.seriesDao().deleteByIds(sourceId, ids)
            database.episodeDao().deleteBySeriesIds(sourceId, ids)
        },
        inTransaction = ::inTransaction,
    )

    private suspend fun inTransaction(block: suspend () -> Unit) = database.withTransaction { block() }

    companion object {
        const val LIVE = "LIVE"
        const val VOD = "VOD"
        const val SERIES = "SERIES"
    }
}
