package com.iptvcinema.tv.features.movies

import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.BrowseCategory
import com.iptvcinema.tv.core.model.home.HomeBecauseYouWatched
import com.iptvcinema.tv.core.model.home.HomeCategoryRail
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.MoviesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.MoviesPersonalSnapshot
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import com.iptvcinema.tv.features.home.movie
import com.iptvcinema.tv.features.home.movies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoviesSectionsBuilderTest {
    private val movieCard = HomeContentCard(contentId = "cw1", contentType = "movie", title = "In progress")
    private val episodeCard = HomeContentCard(contentId = "e1", contentType = "episode", seriesId = "s1", title = "Ep")

    private fun build(
        catalog: MoviesCatalogSnapshot = MoviesCatalogSnapshot(),
        personal: MoviesPersonalSnapshot = MoviesPersonalSnapshot(),
        continueWatching: List<HomeContentCard> = emptyList(),
        isBlocked: (String?, String?) -> Boolean = { _, _ -> false },
    ) = MoviesSectionsBuilder.build(
        catalog = catalog,
        personal = personal,
        continueWatching = continueWatching,
        isBlocked = isBlocked,
        isFavorite = { _, _ -> false },
        countLabel = { "$it titles" },
    )

    @Test
    fun `rails follow the fixed order`() {
        val sections = build(
            catalog = MoviesCatalogSnapshot(
                newest = movies("n1", "n2", "n3", "n4"),
                topRated = movies("t1", "t2", "t3", "t4"),
                categories = listOf(BrowseCategory("Drama", 40)),
            ),
            personal = MoviesPersonalSnapshot(
                becauseYouWatched = HomeBecauseYouWatched("Anchor", movies("b1", "b2", "b3", "b4")),
                categoryRails = listOf(HomeCategoryRail("Action", movies("a1", "a2", "a3", "a4"))),
            ),
            continueWatching = listOf(movieCard),
        )
        assertEquals(
            listOf(
                HomeSectionIds.CONTINUE,
                HomeSectionIds.NEW_MOVIES,
                HomeSectionIds.CATEGORIES,
                HomeSectionIds.BECAUSE_YOU_WATCHED,
                HomeSectionIds.TOP_RATED_MOVIES,
                HomeSectionIds.category("Action"),
            ),
            sections.map { it.id },
        )
    }

    @Test
    fun `continue watching keeps only movies`() {
        val sections = build(continueWatching = listOf(movieCard, episodeCard))
        assertEquals(listOf("cw1"), sections.single().items.map { it.contentId })
    }

    @Test
    fun `a movie shows once across discovery rails but keeps its top rated rank`() {
        val sections = build(
            catalog = MoviesCatalogSnapshot(
                newest = movies("n1", "n2", "n3", "n4"),
                topRated = movies("n1", "t2", "t3", "t4"),
            ),
            personal = MoviesPersonalSnapshot(
                categoryRails = listOf(HomeCategoryRail("Drama", movies("n1", "d1", "d2", "d3", "d4"))),
            ),
        )
        val drama = sections.first { it.id == HomeSectionIds.category("Drama") }
        assertEquals(listOf("d1", "d2", "d3", "d4"), drama.items.map { it.contentId })
        val top = sections.first { it.id == HomeSectionIds.TOP_RATED_MOVIES }
        assertEquals(listOf("n1", "t2", "t3", "t4"), top.items.map { it.contentId })
        assertEquals(listOf(1, 2, 3, 4), top.items.map { it.rank })
    }

    @Test
    fun `blocked categories and ratings are left out everywhere`() {
        val sections = build(
            catalog = MoviesCatalogSnapshot(
                newest = movies("n1", "n2", "n3", "n4") + movie("x1", category = "Adult"),
                categories = listOf(BrowseCategory("Drama", 10), BrowseCategory("Adult", 5)),
            ),
            personal = MoviesPersonalSnapshot(
                categoryRails = listOf(HomeCategoryRail("Adult", movies("a1", "a2", "a3", "a4"))),
            ),
            isBlocked = { category, _ -> category == "Adult" },
        )
        assertTrue(sections.none { it.id == HomeSectionIds.category("Adult") })
        assertTrue(sections.flatMap { it.items }.none { it.contentId == "x1" })
        val tiles = sections.filterIsInstance<HomeSection.Categories>().single()
        assertEquals(listOf("Drama"), tiles.items.map { it.title })
    }

    @Test
    fun `category tiles open the movie catalog and carry their size`() {
        val tiles = build(
            catalog = MoviesCatalogSnapshot(
                categories = listOf(BrowseCategory("Drama", 12, backdropUrl = null, posterUrl = "p.jpg")),
            ),
        ).filterIsInstance<HomeSection.Categories>().single()
        val tile = tiles.items.single()
        assertEquals(BrowseCardTypes.MOVIE_CATEGORY, tile.contentType)
        assertEquals("Drama", tile.contentId)
        assertEquals("12 titles", tile.subtitle)
        assertEquals("p.jpg", tile.backdropUrl)
    }

    @Test
    fun `discovery rails with too few movies are hidden`() {
        val sections = build(catalog = MoviesCatalogSnapshot(newest = movies("n1", "n2", "n3")))
        assertTrue(sections.isEmpty())
    }
}
