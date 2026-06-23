package com.iptvcinema.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchHistoryResumePolicyTest {
    @Test
    fun resumePositionMs_returnsZero_whenBelowMinimum() {
        assertEquals(0L, WatchHistoryResumePolicy.resumePositionMs(4_999L, 100_000L))
    }

    @Test
    fun resumePositionMs_returnsZero_whenNearEnd() {
        assertEquals(0L, WatchHistoryResumePolicy.resumePositionMs(96_000L, 100_000L))
    }

    @Test
    fun resumePositionMs_returnsPosition_whenInRange() {
        assertEquals(30_000L, WatchHistoryResumePolicy.resumePositionMs(30_000L, 100_000L))
    }

    @Test
    fun isContinueWatching_true_forInProgress() {
        assertTrue(WatchHistoryResumePolicy.isContinueWatching(30_000L, 100_000L))
    }

    @Test
    fun isContinueWatching_false_forCompleted() {
        assertFalse(WatchHistoryResumePolicy.isContinueWatching(96_000L, 100_000L))
    }

    @Test
    fun shouldShowAutoplay_true_when30SecondsRemain() {
        assertTrue(WatchHistoryResumePolicy.shouldShowAutoplay(270_000L, 300_000L))
    }

    @Test
    fun shouldShowAutoplay_false_whenMoreThan30SecondsRemain() {
        assertFalse(WatchHistoryResumePolicy.shouldShowAutoplay(260_000L, 300_000L))
    }

    @Test
    fun isBeforeAutoplayWindow_true_whenOutsideWindow() {
        assertTrue(WatchHistoryResumePolicy.isBeforeAutoplayWindow(200_000L, 300_000L))
    }

    @Test
    fun isNearEnd_true_atThreshold() {
        assertTrue(WatchHistoryResumePolicy.isNearEnd(95_000L, 100_000L))
    }

    @Test
    fun selectContinueWatching_keepsLatestEpisodePerSeries() {
        val items = listOf(
            episodeHistory(id = "1", seriesId = "series-a", episodeId = "e17", lastWatchedEpochSecond = 100),
            episodeHistory(id = "2", seriesId = "series-a", episodeId = "e14", lastWatchedEpochSecond = 90),
            episodeHistory(id = "3", seriesId = "series-b", episodeId = "e3", lastWatchedEpochSecond = 80),
        )
        val result = WatchHistoryResumePolicy.selectContinueWatching(items, limit = 10)
        assertEquals(listOf("e17", "e3"), result.map { it.contentId })
    }

    @Test
    fun selectContinueWatching_includesMoviesAndRespectsLimit() {
        val items = listOf(
            movieHistory(id = "m1", lastWatchedEpochSecond = 200),
            episodeHistory(id = "1", seriesId = "series-a", episodeId = "e17", lastWatchedEpochSecond = 100),
            episodeHistory(id = "2", seriesId = "series-a", episodeId = "e14", lastWatchedEpochSecond = 90),
        )
        val result = WatchHistoryResumePolicy.selectContinueWatching(items, limit = 2)
        assertEquals(listOf("m1", "e17"), result.map { it.contentId })
    }

    private fun episodeHistory(
        id: String,
        seriesId: String,
        episodeId: String,
        lastWatchedEpochSecond: Long,
    ) = com.iptvcinema.tv.core.model.WatchHistoryItem(
        id = id,
        profileId = "profile-1",
        sourceId = "source-1",
        contentId = episodeId,
        contentType = com.iptvcinema.tv.core.model.WatchHistoryContentType.EPISODE,
        seriesId = seriesId,
        title = "Episode",
        posterUrl = null,
        positionMs = 30_000L,
        durationMs = 100_000L,
        lastWatchedAt = java.time.Instant.ofEpochSecond(lastWatchedEpochSecond),
    )

    private fun movieHistory(
        id: String,
        lastWatchedEpochSecond: Long,
    ) = com.iptvcinema.tv.core.model.WatchHistoryItem(
        id = id,
        profileId = "profile-1",
        sourceId = "source-1",
        contentId = id,
        contentType = com.iptvcinema.tv.core.model.WatchHistoryContentType.MOVIE,
        seriesId = null,
        title = "Movie",
        posterUrl = null,
        positionMs = 30_000L,
        durationMs = 100_000L,
        lastWatchedAt = java.time.Instant.ofEpochSecond(lastWatchedEpochSecond),
    )
}
