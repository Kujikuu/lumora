package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceMetaFlowTest {
    private val active = source(SourceStatus.ACTIVE)
    private val expired = source(SourceStatus.EXPIRED)

    @Test
    fun cachedFirst_thenServer() = runTest {
        val result = sourceMetaFlow("s1", { active }, { listOf(expired) }).toList()

        assertEquals(
            listOf(SourceStatus.ACTIVE to SourceType.XTREAM_CODES, SourceStatus.EXPIRED to SourceType.XTREAM_CODES),
            result,
        )
    }

    @Test
    fun offline_keepsTheCachedValue() = runTest {
        val result = sourceMetaFlow("s1", { active }, { null }).toList()

        assertEquals(listOf(SourceStatus.ACTIVE to SourceType.XTREAM_CODES), result)
    }

    @Test
    fun offlineWithoutCache_isUnknown() = runTest {
        val result = sourceMetaFlow("s1", { null }, { null }).toList()

        assertEquals(listOf<Pair<SourceStatus?, SourceType?>>(null to null), result)
    }

    @Test
    fun sourceGoneFromServer_isUnknown() = runTest {
        val result = sourceMetaFlow("s1", { active }, { emptyList() }).toList()

        assertEquals(
            listOf(SourceStatus.ACTIVE to SourceType.XTREAM_CODES, null to null),
            result,
        )
    }

    private fun source(status: SourceStatus) = PlaylistSourceRecord(
        id = "s1",
        userId = "u1",
        name = "Provider",
        type = SourceType.XTREAM_CODES,
        serverUrl = null,
        playlistUrl = null,
        epgUrl = null,
        isActive = true,
        status = status,
        lastSyncedAt = null,
    )
}
