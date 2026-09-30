package com.iptvcinema.tv.features.search

import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import com.iptvcinema.tv.features.home.movie
import com.iptvcinema.tv.features.home.series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSectionsBuilderTest {
    private fun movieItem(id: String, category: String? = "Drama", genres: List<String> = emptyList(), rating: String = "") =
        MovieItem(id, "Movie $id", 2024, 100, rating, "Plot", genres, categoryName = category)

    private fun seriesItem(id: String, category: String? = "Drama") =
        SeriesItem(id, "Series $id", 2023, "", "Plot", emptyList(), seasonCount = 1, categoryName = category)

    private fun channelItem(id: String, category: String = "News") = ChannelItem(
        id = id,
        name = "Channel $id",
        category = category,
        currentProgram = "Evening news",
        programStart = "",
        programEnd = "",
        programProgress = 0f,
    )

    @Test
    fun `results are grouped as movies, series, then channels`() {
        val sections = SearchSectionsBuilder.results(
            movies = listOf(movieItem("m1")),
            series = listOf(seriesItem("s1")),
            channels = listOf(channelItem("c1")),
            isBlocked = { _, _ -> false },
            isFavorite = { _, _ -> false },
        )
        assertEquals(listOf(SearchSectionIds.MOVIES, SearchSectionIds.SERIES, SearchSectionIds.CHANNELS), sections.map { it.id })
        assertTrue(sections.last() is HomeSection.Channels)
        assertEquals("Evening news", sections.last().items.single().subtitle)
    }

    @Test
    fun `parental blocks use the provider category, not the first genre`() {
        val sections = SearchSectionsBuilder.results(
            movies = listOf(
                movieItem("m1", category = "Adult", genres = listOf("Drama")),
                movieItem("m2", category = "Drama", genres = listOf("Adult")),
            ),
            series = listOf(seriesItem("s1", category = "Adult")),
            channels = listOf(channelItem("c1", category = "Adult")),
            isBlocked = { category, _ -> category == "Adult" },
            isFavorite = { _, _ -> false },
        )
        assertEquals(listOf("m2"), sections.single().items.map { it.contentId })
    }

    @Test
    fun `no matches means no rails`() {
        val sections = SearchSectionsBuilder.results(emptyList(), emptyList(), emptyList(), { _, _ -> false }, { _, _ -> false })
        assertTrue(sections.isEmpty())
    }

    @Test
    fun `suggestions show recent searches once each, then top rated`() {
        val sections = SearchSectionsBuilder.suggestions(
            recentSearches = listOf("Dune", "dune ", "  ", "Lost"),
            topMovies = listOf("t1", "t2", "t3", "t4").map { movie(it) },
            topSeries = listOf("s1", "s2").map { series(it) },
            isBlocked = { _, _ -> false },
            isFavorite = { _, _ -> false },
        )
        assertEquals(listOf(SearchSectionIds.RECENT, HomeSectionIds.TOP_RATED_MOVIES), sections.map { it.id })
        val recent = sections.first().items
        assertEquals(listOf("Dune", "Lost"), recent.map { it.title })
        assertTrue(recent.all { it.contentType == BrowseCardTypes.SEARCH_TERM })
    }

    @Test
    fun `each search gets new rail ids so rails start at the best match`() {
        fun ids(number: Int) = SearchSectionsBuilder.results(
            movies = listOf(movieItem("m1")),
            series = emptyList(),
            channels = emptyList(),
            isBlocked = { _, _ -> false },
            isFavorite = { _, _ -> false },
            searchNumber = number,
        ).map { it.id }
        assertTrue(ids(1) != ids(2))
        assertEquals(ids(3), ids(3))
    }
}
