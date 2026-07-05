package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.catalog.CatalogSyncProgressMapper
import com.iptvcinema.tv.core.catalog.SyncMilestone
import com.iptvcinema.tv.core.m3u.M3uSyncProgress
import com.iptvcinema.tv.core.m3u.M3uSyncStep
import com.iptvcinema.tv.core.xtream.XtreamSyncProgress
import com.iptvcinema.tv.core.xtream.XtreamSyncStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSyncProgressMapperTest {
    @Test
    fun `empty xtream progress starts at zero`() {
        val result = CatalogSyncProgressMapper.mapXtream(emptyList())
        assertEquals(0f, result.fraction, 0.001f)
        assertEquals(SyncMilestone.CONNECTING, result.milestone)
    }

    @Test
    fun `mid-sync xtream after vod streams is roughly two thirds`() {
        val steps = listOf(
            XtreamSyncProgress(XtreamSyncStep.VALIDATING_URL, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.AUTHENTICATING, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.LIVE_STREAMS, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.VOD_STREAMS, isSuccess = true),
        )
        val result = CatalogSyncProgressMapper.mapXtream(steps)
        assertEquals(4f / 6f, result.fraction, 0.001f)
        assertEquals(SyncMilestone.MOVIES, result.milestone)
    }

    @Test
    fun `complete xtream progress reaches one`() {
        val steps = listOf(
            XtreamSyncProgress(XtreamSyncStep.VALIDATING_URL, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.AUTHENTICATING, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.LIVE_STREAMS, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.VOD_STREAMS, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.SERIES, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.COMPLETE, isSuccess = true),
        )
        val result = CatalogSyncProgressMapper.mapXtream(steps)
        assertEquals(1f, result.fraction, 0.001f)
        assertEquals(SyncMilestone.COMPLETE, result.milestone)
    }

    @Test
    fun `m3u two completed milestones is half`() {
        val steps = listOf(
            M3uSyncProgress(M3uSyncStep.VALIDATING_URL, isSuccess = true),
            M3uSyncProgress(M3uSyncStep.DOWNLOADING, isSuccess = true),
        )
        val result = CatalogSyncProgressMapper.mapM3u(steps)
        assertEquals(0.5f, result.fraction, 0.001f)
        assertEquals(SyncMilestone.M3U_DOWNLOAD, result.milestone)
    }

    @Test
    fun `in-progress xtream step adds partial credit`() {
        val steps = listOf(
            XtreamSyncProgress(XtreamSyncStep.VALIDATING_URL, isSuccess = true),
            XtreamSyncProgress(XtreamSyncStep.AUTHENTICATING, isSuccess = false),
        )
        val result = CatalogSyncProgressMapper.mapXtream(steps)
        assertTrue(result.fraction > 1f / 6f)
        assertTrue(result.fraction < 2f / 6f)
    }
}
