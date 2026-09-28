package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.MovieItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeroCarouselLogicTest {
    @Test
    fun nextIndex_advancesAndWrapsAround() {
        assertEquals(1, HeroCarouselLogic.nextIndex(current = 0, count = 3))
        assertEquals(0, HeroCarouselLogic.nextIndex(current = 2, count = 3))
        assertEquals(0, HeroCarouselLogic.nextIndex(current = 0, count = 0))
    }

    @Test
    fun ratingBadge_hidesMissingAndZeroRatings() {
        assertNull(HeroCarouselLogic.ratingBadge(null))
        assertNull(HeroCarouselLogic.ratingBadge(""))
        assertNull(HeroCarouselLogic.ratingBadge("0"))
        assertNull(HeroCarouselLogic.ratingBadge("0.0"))
        assertEquals("★ 7.2", HeroCarouselLogic.ratingBadge("7.2"))
    }

    @Test
    fun metadata_skipsEmptyFieldsAndCapsGenres() {
        val movie = movie(year = 2024, runtime = 95, genres = listOf("Drama", "Comedy", "Action"))
        assertEquals(listOf("2024", "Drama · Comedy", "95m"), HeroCarouselLogic.metadata(movie))
        assertEquals(emptyList<String>(), HeroCarouselLogic.metadata(movie(year = 0, runtime = 0, genres = emptyList())))
    }

    private fun movie(year: Int, runtime: Int, genres: List<String>) = MovieItem(
        id = "m1",
        title = "Title",
        year = year,
        runtimeMinutes = runtime,
        rating = "",
        plot = "",
        genres = genres,
    )
}
