package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.WatchHistoryContentType.CHANNEL
import com.iptvcinema.tv.core.model.WatchHistoryContentType.EPISODE
import com.iptvcinema.tv.core.model.WatchHistoryContentType.MOVIE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeContentRulesTest {
    @Test
    fun `hero prefers higher rated movies within the newest pool`() {
        val newest = (1..20).map { movie("m$it", rating = if (it == 3) "9.1" else "6.0") } +
            movie("old", rating = "9.9")
        val hero = HomeContentRules.pickHero(newest)
        assertEquals(5, hero.size)
        assertEquals("m3", hero.first().id)
        // Ties keep recency order; movies outside the newest 15 never make it.
        assertEquals(listOf("m3", "m1", "m2", "m4", "m5"), hero.map { it.id })
    }

    @Test
    fun `rating value reads the leading number and treats junk as zero`() {
        assertEquals(7.5, HomeContentRules.ratingValue("7.5/10"), 0.0)
        assertEquals(8.0, HomeContentRules.ratingValue(" 8 "), 0.0)
        assertEquals(0.0, HomeContentRules.ratingValue("N/A"), 0.0)
        assertEquals(0.0, HomeContentRules.ratingValue(null), 0.0)
    }

    @Test
    fun `categories rank by watch count then fill from provider fallback`() {
        val ranked = HomeContentRules.rankCategories(
            watchedCategories = listOf("Drama", "Action", "Drama", null, "", "Action", "Drama", "Kids"),
            fallback = listOf("Action", "Comedy", "Horror"),
            limit = 3,
            isBlocked = { false },
        )
        assertEquals(listOf("Drama", "Action", "Kids"), ranked)
    }

    @Test
    fun `new viewer gets provider largest categories without blocked ones`() {
        val ranked = HomeContentRules.rankCategories(
            watchedCategories = emptyList(),
            fallback = listOf("Adult", "Comedy", "Action", "Horror"),
            limit = 2,
            isBlocked = { it == "Adult" },
        )
        assertEquals(listOf("Comedy", "Action"), ranked)
    }

    @Test
    fun `next episode candidates are finished latest episodes one per series`() {
        val history = listOf(
            history("s1e2", EPISODE, 300, positionMs = 97_000, seriesId = "s1"),
            history("s1e1", EPISODE, 100, positionMs = 99_000, seriesId = "s1"),
            // Latest episode of s2 is still in progress, so it belongs to Continue Watching.
            history("s2e3", EPISODE, 250, positionMs = 30_000, seriesId = "s2"),
            history("s2e2", EPISODE, 200, positionMs = 99_000, seriesId = "s2"),
            history("s3e1", EPISODE, 400, positionMs = 99_000, seriesId = "s3"),
            history("m1", MOVIE, 500, positionMs = 99_000),
            history("orphan", EPISODE, 600, positionMs = 99_000, seriesId = null),
        )
        val candidates = HomeContentRules.nextEpisodeCandidates(history, limit = 8)
        assertEquals(listOf("s3e1", "s1e2"), candidates.map { it.contentId })
    }

    @Test
    fun `recent channels are distinct and keep history order`() {
        val history = listOf(
            history("c2", CHANNEL, 500),
            history("m1", MOVIE, 400),
            history("c1", CHANNEL, 300),
            history("c2", CHANNEL, 200),
        )
        assertEquals(listOf("c2", "c1"), HomeContentRules.recentChannelIds(history, limit = 10))
    }

    @Test
    fun `last watched movie ignores episodes and channels`() {
        val history = listOf(
            history("e1", EPISODE, 500, seriesId = "s"),
            history("m2", MOVIE, 400),
            history("m1", MOVIE, 300),
        )
        assertEquals("m2", HomeContentRules.lastWatchedMovie(history)?.contentId)
        assertNull(HomeContentRules.lastWatchedMovie(emptyList()))
    }
}
