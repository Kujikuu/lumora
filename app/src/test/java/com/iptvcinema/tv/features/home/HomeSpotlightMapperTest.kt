package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeSpotlightMapperTest {
    @Test
    fun `hero movie spotlight carries rating, metadata and backdrop`() {
        val movie = MovieItem(
            id = "m1", title = "Dune", year = 2024, runtimeMinutes = 155, rating = "8.3",
            plot = "Spice.", genres = listOf("Sci-Fi", "Drama", "Action"),
            imageUrl = "poster", backdropUrl = "backdrop",
        )
        val spotlight = HomeSpotlightMapper.fromHero(movie)
        assertEquals("hero:m1", spotlight.key)
        assertEquals(HomeSpotlightKind.Movie, spotlight.kind)
        assertEquals("backdrop", spotlight.backdropUrl)
        assertEquals("★ 8.3", spotlight.ratingBadge)
        assertEquals(listOf("2024", "Sci-Fi · Drama", "155m"), spotlight.metadata)
        assertEquals("Spice.", spotlight.plot)
    }

    @Test
    fun `card spotlight falls back to poster and hides zero ratings`() {
        val card = HomeContentCard(
            contentId = "s1", contentType = "series", title = "Show",
            imageUrl = "poster", backdropUrl = null, rating = "0", year = "2022",
            genres = listOf("Drama"),
        )
        val spotlight = HomeSpotlightMapper.fromCard(card)
        assertEquals(HomeSpotlightKind.Series, spotlight.kind)
        assertEquals("poster", spotlight.backdropUrl)
        assertNull(spotlight.ratingBadge)
        assertEquals(listOf("2022", "Drama"), spotlight.metadata)
    }

    @Test
    fun `continue watching spotlight shows progress and the remaining time`() {
        val card = HomeContentCard(
            contentId = "e1", contentType = "episode", title = "Show",
            subtitle = "S1E2 · Pilot", progress = 0.4f, remainingTimeLabel = "20 min",
        )
        val spotlight = HomeSpotlightMapper.fromCard(card)
        assertEquals(HomeSpotlightKind.Episode, spotlight.kind)
        assertEquals(0.4f, spotlight.progress)
        assertEquals(listOf("S1E2 · Pilot", "20 min"), spotlight.metadata)
    }

    @Test
    fun `channel spotlight uses its category as metadata`() {
        val card = HomeContentCard(contentId = "c1", contentType = "channel", title = "News One", subtitle = "News")
        val spotlight = HomeSpotlightMapper.fromCard(card)
        assertEquals(HomeSpotlightKind.Channel, spotlight.kind)
        assertEquals(listOf("News"), spotlight.metadata)
    }

    @Test
    fun `channel spotlight never stretches the logo into a backdrop`() {
        val card = HomeContentCard(contentId = "c1", contentType = "channel", title = "News One", imageUrl = "logo.png")
        assertEquals(null, HomeSpotlightMapper.fromCard(card).backdropUrl)
    }
}
