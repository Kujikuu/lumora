package com.iptvcinema.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeWatchProgressTest {
    @Test
    fun below95Percent_isPartial() {
        assertEquals(EpisodeWatchVisualState.PARTIAL, EpisodeWatchProgress.visualState(0.949f))
    }

    @Test
    fun exactly95Percent_isWatched() {
        assertEquals(EpisodeWatchVisualState.WATCHED, EpisodeWatchProgress.visualState(0.95f))
    }

    @Test
    fun nullAndZero_areUnwatched() {
        assertEquals(EpisodeWatchVisualState.UNWATCHED, EpisodeWatchProgress.visualState(null))
        assertEquals(EpisodeWatchVisualState.UNWATCHED, EpisodeWatchProgress.visualState(0f))
    }

    @Test
    fun nonFiniteProgress_isUnwatched() {
        assertEquals(EpisodeWatchVisualState.UNWATCHED, EpisodeWatchProgress.visualState(Float.NaN))
        assertEquals(
            EpisodeWatchVisualState.UNWATCHED,
            EpisodeWatchProgress.visualState(Float.POSITIVE_INFINITY),
        )
    }
}
