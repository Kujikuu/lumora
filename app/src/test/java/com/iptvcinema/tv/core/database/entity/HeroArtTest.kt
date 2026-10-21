package com.iptvcinema.tv.core.database.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroArtTest {
    @Test
    fun needsBothBackdropAndPlot() {
        assertTrue(HeroArt.of("https://img/b.jpg", "A plot"))
        assertFalse(HeroArt.of(null, "A plot"))
        assertFalse(HeroArt.of("https://img/b.jpg", null))
        assertFalse(HeroArt.of(null, null))
    }

    // The v11 back-fill uses TRIM(x) != '', and SQLite's TRIM strips spaces only.
    @Test
    fun matchesSqliteTrim() {
        assertFalse(HeroArt.of("   ", "A plot"))
        assertFalse(HeroArt.of("https://img/b.jpg", ""))
        assertTrue(HeroArt.of("https://img/b.jpg", "\t"))
        assertTrue(HeroArt.of("\n", "A plot"))
    }

    @Test
    fun entityComputesItFromBackdropAndPlot() {
        val movie = LocalMovieEntity(
            id = "1",
            sourceId = "src",
            title = "Movie",
            streamUrl = "u",
            posterUrl = null,
            backdropUrl = "https://img/b.jpg",
            categoryId = null,
            categoryName = null,
            year = null,
            durationMinutes = null,
            rating = null,
            plot = "A plot",
            genres = null,
        )
        assertTrue(movie.hasHeroArt)
    }
}
