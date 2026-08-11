package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.model.SourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSyncPolicyTest {
    @Test
    fun missingTimestamp_isStale() {
        assertTrue(CatalogSyncPolicy.isStale(null, nowEpochMs = 10_000L))
    }

    @Test
    fun timestampInsideThreeHours_isFresh() {
        val now = 20_000_000L
        assertFalse(CatalogSyncPolicy.isStale(now - CatalogSyncPolicy.STARTUP_FRESHNESS_MS + 1, now))
    }

    @Test
    fun timestampAtThreeHours_isStale() {
        val now = 20_000_000L
        assertTrue(CatalogSyncPolicy.isStale(now - CatalogSyncPolicy.STARTUP_FRESHNESS_MS, now))
    }

    @Test
    fun manualAndInitialTriggersForceWhileAutomaticTriggersDoNot() {
        assertTrue(CatalogSyncTrigger.MANUAL.force)
        assertTrue(CatalogSyncTrigger.INITIAL_CONNECTION.force)
        assertFalse(CatalogSyncTrigger.STARTUP.force)
        assertFalse(CatalogSyncTrigger.PERIODIC.force)
        assertFalse(CatalogSyncTrigger.MANUAL.awaitEpgCompletion)
        assertFalse(CatalogSyncTrigger.INITIAL_CONNECTION.awaitEpgCompletion)
        assertTrue(CatalogSyncTrigger.STARTUP.awaitEpgCompletion)
        assertTrue(CatalogSyncTrigger.PERIODIC.awaitEpgCompletion)
    }

    @Test
    fun manualJoiningUnchangedPeriodicSync_requiresFollowUpForForcedEpgRefresh() {
        val unchanged = CatalogSyncCoordinatorResult.Success(
            sourceType = SourceType.XTREAM_CODES,
            liveChannelCount = 10,
            movieCount = 2,
            seriesCount = 3,
            changes = CatalogChangeSummary(),
            checkedProvider = true,
        )

        assertTrue(
            requiresForcedFollowUp(
                requestedTrigger = CatalogSyncTrigger.MANUAL,
                activeTrigger = CatalogSyncTrigger.PERIODIC,
                result = unchanged,
            ),
        )
    }

    @Test
    fun manualJoiningChangedPeriodicSync_doesNotRepeatProviderWork() {
        val changed = CatalogSyncCoordinatorResult.Success(
            sourceType = SourceType.XTREAM_CODES,
            liveChannelCount = 10,
            movieCount = 2,
            seriesCount = 3,
            changes = CatalogChangeSummary(added = 1),
            checkedProvider = true,
        )

        assertFalse(
            requiresForcedFollowUp(
                requestedTrigger = CatalogSyncTrigger.MANUAL,
                activeTrigger = CatalogSyncTrigger.PERIODIC,
                result = changed,
            ),
        )
    }

    @Test
    fun databaseBatchesStayUnderSqliteParameterLimit() {
        val batches = CatalogSyncPolicy.databaseBatches((1..1_201).toList())

        assertEquals(listOf(500, 500, 201), batches.map(List<Int>::size))
    }

    @Test
    fun automaticTriggerJoiningForegroundSync_mustAwaitForegroundEpgJob() {
        val success = CatalogSyncCoordinatorResult.Success(
            sourceType = SourceType.XTREAM_CODES,
            liveChannelCount = 1,
            movieCount = 0,
            seriesCount = 0,
            changes = CatalogChangeSummary(added = 1),
            checkedProvider = true,
        )

        assertTrue(requiresEpgJoin(CatalogSyncTrigger.PERIODIC, CatalogSyncTrigger.MANUAL, success))
        assertTrue(requiresEpgJoin(CatalogSyncTrigger.STARTUP, CatalogSyncTrigger.INITIAL_CONNECTION, success))
        assertFalse(requiresEpgJoin(CatalogSyncTrigger.MANUAL, CatalogSyncTrigger.PERIODIC, success))
    }
}
