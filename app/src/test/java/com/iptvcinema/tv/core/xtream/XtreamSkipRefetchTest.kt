package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository
import com.iptvcinema.tv.core.model.XtreamCredentials
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamSkipRefetchTest {
    @Test
    fun hasCachedCatalogData_trueWhenSyncStateHasChannels() {
        val syncState = LocalSourceSyncStateEntity(
            sourceId = "source-1",
            lastSyncedAtEpochMs = 1L,
            liveChannelCount = 10,
            movieCount = 0,
            seriesCount = 0,
            epgAvailable = false,
            lastError = null,
        )

        assertTrue(XtreamSyncRepository.hasCachedCatalogData(syncState, channelRowCount = 0))
    }

    @Test
    fun hasCachedCatalogData_trueWhenRoomHasChannelsWithoutSyncState() {
        assertTrue(
            XtreamSyncRepository.hasCachedCatalogData(
                syncState = null,
                channelRowCount = 3,
            ),
        )
    }

    @Test
    fun hasCachedCatalogData_falseWhenEmpty() {
        assertFalse(
            XtreamSyncRepository.hasCachedCatalogData(
                syncState = null,
                channelRowCount = 0,
            ),
        )
    }

    @Test
    fun matchesXtreamSource_matchesNormalizedServerAndUsername() {
        val credentials = XtreamCredentials(
            serverUrl = "http://example.com:8080",
            username = "demo",
            password = "secret",
            accountName = "Main",
        )
        val stored = credentials.copy(serverUrl = "example.com:8080")

        assertTrue(
            SupabasePlaylistSourcesRepository.matchesXtreamSource(
                serverUrl = "http://example.com:8080/",
                storedCredentials = stored,
                credentials = credentials,
            ),
        )
    }

    @Test
    fun matchesXtreamSource_rejectsDifferentUsername() {
        val credentials = XtreamCredentials(
            serverUrl = "http://example.com",
            username = "demo",
            password = "secret",
            accountName = "Main",
        )

        assertFalse(
            SupabasePlaylistSourcesRepository.matchesXtreamSource(
                serverUrl = "http://example.com",
                storedCredentials = credentials.copy(username = "other"),
                credentials = credentials,
            ),
        )
    }

    @Test
    fun matchesXtreamSource_rejectsMissingStoredCredentials() {
        val credentials = XtreamCredentials(
            serverUrl = "http://example.com",
            username = "demo",
            password = "secret",
            accountName = "Main",
        )

        assertFalse(
            SupabasePlaylistSourcesRepository.matchesXtreamSource(
                serverUrl = "http://example.com",
                storedCredentials = null,
                credentials = credentials,
            ),
        )
    }
}
