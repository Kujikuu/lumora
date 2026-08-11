package com.iptvcinema.tv.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iptvcinema.tv.core.catalog.CatalogSyncResource
import com.iptvcinema.tv.core.database.entity.CatalogSyncMetadataEntity
import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity
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
