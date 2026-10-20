package com.iptvcinema.tv.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iptvcinema.tv.core.catalog.CatalogSyncResource
import com.iptvcinema.tv.core.database.entity.CatalogSyncMetadataEntity
import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity
import com.iptvcinema.tv.core.database.entity.LocalMovieEntity
import com.iptvcinema.tv.core.database.entity.LocalSeriesEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogDaoFacadeIntegrationTest {
    private lateinit var database: IptvDatabase
    private lateinit var facade: CatalogDaoFacade

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            IptvDatabase::class.java,
        ).allowMainThreadQueries().build()
        facade = CatalogDaoFacade(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reconciliationUpdatesNamesDeletesOrphanEpisodesAndPreservesStateOnEpgUpdate() = runBlocking {
        val sourceId = "source-1"
        val liveCategory = category(sourceId, "live-1", "Old name", CatalogDaoFacade.LIVE)
        val seriesCategory = category(sourceId, "series-1", "Drama", CatalogDaoFacade.SERIES)
        val channel = channel(sourceId, liveCategory.id, liveCategory.name)
        val series = series(sourceId, seriesCategory.id, seriesCategory.name)

        val initial = facade.reconcileCatalog(
            sourceId = sourceId,
            liveCategories = listOf(liveCategory),
            channels = listOf(channel),
            vodCategories = emptyList(),
            movies = emptyList(),
            seriesCategories = listOf(seriesCategory),
            seriesItems = listOf(series),
            metadata = listOf(metadata(sourceId, checkedAt = 100L)),
            syncedAtEpochMs = 100L,
        )
        assertEquals(2, initial.changes.added)

        facade.episodes.upsertAll(
            listOf(
                LocalEpisodeEntity(
                    id = "episode-1",
                    sourceId = sourceId,
                    seriesId = series.id,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    title = "Pilot",
                    streamUrl = "https://stream.example/episode-1",
                    durationMinutes = 45,
                    plot = null,
                    thumbnailUrl = null,
                ),
            ),
        )

        val reconciled = facade.reconcileCatalog(
            sourceId = sourceId,
            liveCategories = listOf(liveCategory.copy(name = "New name")),
            channels = null,
            vodCategories = null,
            movies = null,
            seriesCategories = null,
            seriesItems = emptyList(),
            metadata = listOf(metadata(sourceId, checkedAt = 200L)),
            syncedAtEpochMs = 200L,
        )

        assertEquals(1, reconciled.changes.updated)
        assertEquals(1, reconciled.changes.removed)
        assertEquals("New name", facade.channels.getById(sourceId, channel.id)?.categoryName)
        assertNull(facade.episodes.getById(sourceId, "episode-1"))

        val metadataOnly = facade.reconcileCatalog(
            sourceId = sourceId,
            liveCategories = null,
            channels = null,
            vodCategories = null,
            movies = null,
            seriesCategories = null,
            seriesItems = null,
            metadata = listOf(metadata(sourceId, checkedAt = 300L)),
            syncedAtEpochMs = 300L,
        )
        assertFalse(metadataOnly.changes.hasChanges)

        facade.syncState.updateEpgAvailable(sourceId, true)
        val state = facade.syncState.get(sourceId)
        assertEquals(300L, state?.lastSyncedAtEpochMs)
        assertEquals(1, state?.liveChannelCount)
        assertEquals(0, state?.movieCount)
        assertEquals(0, state?.seriesCount)
        assertTrue(state?.epgAvailable == true)
    }

    @Test
    fun largeSyncCommittedInGroupsEndsWithTheProviderList() = runBlocking {
        val sourceId = "source-large"
        val size = CatalogItemReconciler.ITEMS_PER_TRANSACTION + 10
        val first = (0 until size).map { movie(sourceId, id = "m$it", sortOrder = it) }

        val initial = reconcileMovies(sourceId, first)
        assertEquals(size, initial.changes.added)
        assertEquals(size, initial.movieCount)

        // Drop the first 100, retitle the next 100, keep the rest.
        val second = first.drop(100).mapIndexed { index, movie ->
            if (index < 100) movie.copy(title = "Retitled ${movie.id}") else movie
        }
        val reconciled = reconcileMovies(sourceId, second)

        assertEquals(0, reconciled.changes.added)
        assertEquals(100, reconciled.changes.updated)
        assertEquals(100, reconciled.changes.removed)
        assertEquals(size - 100, reconciled.movieCount)
        assertEquals(second.map { it.id }.toSet(), facade.movies.getIdsBySource(sourceId).toSet())
        assertEquals("Retitled m100", facade.movies.getById(sourceId, "m100")?.title)
        assertNull(facade.movies.getById(sourceId, "m0"))
    }

    @Test
    fun indexedRankingQueriesKeepTheirOrder() = runBlocking {
        val sourceId = "source-rank"
        facade.movies.upsertAll(
            listOf(
                movie(sourceId, id = "undated-low", sortOrder = 1, addedAt = null, rating = null),
                movie(sourceId, id = "undated-high", sortOrder = 5, addedAt = null, rating = "N/A"),
                movie(sourceId, id = "old", sortOrder = 2, addedAt = 100L, rating = "7.5/10", year = 2001),
                movie(sourceId, id = "new", sortOrder = 3, addedAt = 300L, rating = "9", year = 1999),
                movie(sourceId, id = "tie-low", sortOrder = 4, addedAt = 200L, rating = "7.5", year = 2005),
                movie(sourceId, id = "tie-high", sortOrder = 6, addedAt = 200L, rating = "6.9", year = 2010),
            ),
        )
        // Dated movies newest first (sortOrder breaks ties), then undated ones.
        assertEquals(
            listOf("new", "tie-high", "tie-low", "old", "undated-high", "undated-low"),
            facade.movies.getNewest(sourceId, limit = 10).map { it.id },
        )
        // Rating parsed like CAST(rating AS REAL); year breaks ties; unrated and below-min left out.
        assertEquals(
            listOf("new", "tie-low", "old"),
            facade.movies.getTopRated(sourceId, minRating = 7.0, limit = 10).map { it.id },
        )

        facade.series.upsertAll(
            listOf(
                series(sourceId, "c", "Drama").copy(id = "no-year", year = null, sortOrder = 9),
                series(sourceId, "c", "Drama").copy(id = "zero", year = 0, sortOrder = 1),
                series(sourceId, "c", "Drama").copy(id = "negative", year = -1, sortOrder = 8),
                series(sourceId, "c", "Drama").copy(id = "y2020", year = 2020, sortOrder = 2),
                series(sourceId, "c", "Drama").copy(id = "y2020b", year = 2020, sortOrder = 3),
                series(sourceId, "c", "Drama").copy(id = "y1990", year = 1990, sortOrder = 4),
            ),
        )
        // Real years first, newest first; then 0, negative, and missing years, as before.
        val expectedLatest = listOf("y2020b", "y2020", "y1990", "zero", "negative", "no-year")
        assertEquals(expectedLatest, facade.series.getLatest(sourceId, limit = 10).map { it.id })
        assertEquals(
            expectedLatest,
            facade.series.getByCategoryNameLimited(sourceId, "Drama", limit = 10).map { it.id },
        )
    }

    private suspend fun reconcileMovies(sourceId: String, movies: List<LocalMovieEntity>) =
        facade.reconcileCatalog(
            sourceId = sourceId,
            liveCategories = null,
            channels = null,
            vodCategories = null,
            movies = movies,
            seriesCategories = null,
            seriesItems = null,
            metadata = emptyList(),
            syncedAtEpochMs = 1L,
        )

    private fun movie(
        sourceId: String,
        id: String,
        sortOrder: Int,
        addedAt: Long? = null,
        rating: String? = null,
        year: Int? = null,
    ) = LocalMovieEntity(
        id = id,
        sourceId = sourceId,
        title = "Movie $id",
        streamUrl = "https://stream.example/$id",
        posterUrl = null,
        backdropUrl = null,
        categoryId = null,
        categoryName = null,
        year = year,
        durationMinutes = null,
        rating = rating,
        plot = null,
        genres = null,
        sortOrder = sortOrder,
        addedAt = addedAt,
    )

    private fun category(sourceId: String, id: String, name: String, type: String) = LocalCategoryEntity(
        id = id,
        sourceId = sourceId,
        name = name,
        contentType = type,
    )

    private fun channel(sourceId: String, categoryId: String, categoryName: String) = LocalChannelEntity(
        id = "channel-1",
        sourceId = sourceId,
        name = "Channel",
        streamUrl = "https://stream.example/channel-1",
        logoUrl = null,
        categoryId = categoryId,
        categoryName = categoryName,
        tvgId = null,
        channelNumber = 1,
    )

    private fun series(sourceId: String, categoryId: String, categoryName: String) = LocalSeriesEntity(
        id = "series-item-1",
        sourceId = sourceId,
        title = "Series",
        posterUrl = null,
        backdropUrl = null,
        categoryId = categoryId,
        categoryName = categoryName,
        plot = null,
        rating = null,
        year = null,
    )

    private fun metadata(sourceId: String, checkedAt: Long) = CatalogSyncMetadataEntity(
        sourceId = sourceId,
        resourceKey = CatalogSyncResource.XTREAM_LIVE_STREAMS,
        contentFingerprint = "fingerprint-$checkedAt",
        lastCheckedAtEpochMs = checkedAt,
    )
}
