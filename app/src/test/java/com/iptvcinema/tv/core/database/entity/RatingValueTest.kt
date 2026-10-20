package com.iptvcinema.tv.core.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingValueTest {
    @Test
    fun nullRating_staysNull() {
        assertNull(RatingValue.of(null))
    }

    // Expected values were taken from SQLite: SELECT CAST(v AS REAL). The v10 migration back-fills
    // old rows with that CAST, so new rows must get the very same numbers.
    @Test
    fun matchesSqliteCastAsReal() {
        val cases = mapOf(
            "7.5" to 7.5,
            "7.5/10" to 7.5,
            " 8" to 8.0,
            "\t6.2" to 6.2,
            "7.25 stars" to 7.25,
            "10" to 10.0,
            ".5" to 0.5,
            "5." to 5.0,
            "+3" to 3.0,
            "-2.5" to -2.5,
            "1e1" to 10.0,
            "1e" to 1.0,
            "8,5" to 8.0,
            "0x10" to 0.0,
            "N/A" to 0.0,
            "" to 0.0,
            "-" to 0.0,
            "٧" to 0.0,
        )
        cases.forEach { (rating, expected) ->
            assertEquals("rating '$rating'", expected, RatingValue.of(rating))
        }
    }

    @Test
    fun entitiesFillRatingValueFromRating() {
        val movie = LocalMovieEntity(
            id = "1",
            sourceId = "s",
            title = "Movie",
            streamUrl = "u",
            posterUrl = null,
            backdropUrl = null,
            categoryId = null,
            categoryName = null,
            year = null,
            durationMinutes = null,
            rating = "8.1/10",
            plot = null,
            genres = null,
        )
        val series = LocalSeriesEntity(
            id = "1",
            sourceId = "s",
            title = "Series",
            posterUrl = null,
            backdropUrl = null,
            categoryId = null,
            categoryName = null,
            plot = null,
            rating = null,
            year = null,
        )

        assertEquals(8.1, movie.ratingValue)
        assertNull(series.ratingValue)
    }
}
