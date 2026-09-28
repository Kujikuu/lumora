package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.home.HomeBecauseYouWatched
import com.iptvcinema.tv.core.model.home.HomeCatalogSnapshot
import com.iptvcinema.tv.core.model.home.HomeCategoryRail
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.HomeNextEpisode
import com.iptvcinema.tv.core.model.home.HomePersonalSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionsBuilderTest {
    private val continueCard = HomeContentCard(contentId = "cw1", contentType = "movie", title = "In progress")

    private fun build(
        catalog: HomeCatalogSnapshot = HomeCatalogSnapshot(),
        personal: HomePersonalSnapshot = HomePersonalSnapshot(),
        continueWatching: List<HomeContentCard> = emptyList(),
        isBlocked: (String?, String?) -> Boolean = { _, _ -> false },
    ) = HomeSectionsBuilder.build(
        catalog = catalog,
        personal = personal,
        continueWatching = continueWatching,
        isBlocked = isBlocked,
        isFavorite = { _, _ -> false },
    )

    @Test
    fun `sections follow the fixed order and skip missing ones`() {
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = movies("h1"),
                newestMovies = movies("n1", "n2", "n3", "n4"),
                topRatedMovies = movies("t1", "t2", "t3", "t4"),
                topRatedSeries = listOf(series("s1"), series("s2"), series("s3"), series("s4")),
            ),
            personal = HomePersonalSnapshot(
                nextEpisodes = listOf(HomeNextEpisode(series("s9"), episode("e2", "s9", number = 2))),
                becauseYouWatched = HomeBecauseYouWatched("Anchor", movies("b1", "b2", "b3", "b4")),
                categoryRails = listOf(HomeCategoryRail("Drama", movies("d1", "d2", "d3", "d4"))),
                recentChannels = listOf(channel("c1")),
            ),
            continueWatching = listOf(continueCard),
        )
        assertEquals(
            listOf(
                HomeSectionIds.CONTINUE,
                HomeSectionIds.NEXT_EPISODE,
                HomeSectionIds.NEW_MOVIES,
                HomeSectionIds.BECAUSE_YOU_WATCHED,
                HomeSectionIds.TOP_RATED_MOVIES,
                HomeSectionIds.TOP_RATED_SERIES,
                HomeSectionIds.category("Drama"),
                HomeSectionIds.RECENT_CHANNELS,
            ),
            content.sections.map { it.id },
        )
    }

    @Test
    fun `hero movies never repeat in discovery rails`() {
        val hero = movies("h1", "h2", "h3", "h4", "h5")
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = hero,
                newestMovies = hero + movies("n1", "n2", "n3", "n4"),
            ),
            personal = HomePersonalSnapshot(
                categoryRails = listOf(HomeCategoryRail("Drama", movies("n1", "d1", "d2", "d3", "d4"))),
            ),
        )
        assertEquals(listOf("h1", "h2", "h3", "h4", "h5"), content.hero.map { it.id })
        val newMovies = content.sections.first { it.id == HomeSectionIds.NEW_MOVIES }
        assertEquals(listOf("n1", "n2", "n3", "n4"), newMovies.items.map { it.contentId })
        val drama = content.sections.first { it.id == HomeSectionIds.category("Drama") }
        assertEquals(listOf("d1", "d2", "d3", "d4"), drama.items.map { it.contentId })
    }

    @Test
    fun `discovery rails with fewer than four items are hidden but personal rails are not`() {
        val content = build(
            catalog = HomeCatalogSnapshot(heroCandidates = movies("h1"), newestMovies = movies("n1", "n2", "n3")),
            personal = HomePersonalSnapshot(recentChannels = listOf(channel("c1"))),
            continueWatching = listOf(continueCard),
        )
        assertEquals(
            listOf(HomeSectionIds.CONTINUE, HomeSectionIds.RECENT_CHANNELS),
            content.sections.map { it.id },
        )
    }

    @Test
    fun `hidden rail does not consume ids for later rails`() {
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = movies("h1"),
                newestMovies = movies("x1", "x2", "x3"),
            ),
            personal = HomePersonalSnapshot(
                categoryRails = listOf(HomeCategoryRail("Drama", movies("x1", "x2", "x3", "x4"))),
            ),
        )
        val drama = content.sections.single()
        assertEquals(listOf("x1", "x2", "x3", "x4"), drama.items.map { it.contentId })
    }

    @Test
    fun `top rated rails are ranked, capped at ten and exempt from de-duplication`() {
        val rated = (1..14).map { movie("t$it", rating = "8.0") }
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = listOf(rated.first()),
                topRatedMovies = rated,
            ),
        )
        val top = content.sections.single { it.id == HomeSectionIds.TOP_RATED_MOVIES }
        assertEquals(10, top.items.size)
        assertEquals("t1", top.items.first().contentId)
        assertEquals((1..10).toList(), top.items.map { it.rank })
    }

    @Test
    fun `because you watched drops movies already watched`() {
        val content = build(
            personal = HomePersonalSnapshot(
                becauseYouWatched = HomeBecauseYouWatched("Anchor", movies("w1", "b1", "b2", "b3", "b4")),
                watchedMovieIds = setOf("w1"),
            ),
        )
        val because = content.sections.single()
        assertEquals(listOf("b1", "b2", "b3", "b4"), because.items.map { it.contentId })
        assertEquals("Anchor", because.title.argument)
    }

    @Test
    fun `parental filter applies to every rail including channels`() {
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = listOf(movie("h1", category = "Adult")),
                newestMovies = movies("n1", "n2", "n3", "n4") + movie("a1", category = "Adult"),
            ),
            personal = HomePersonalSnapshot(
                recentChannels = listOf(channel("c1", category = "Adult"), channel("c2", category = "News")),
            ),
            isBlocked = { category, _ -> category == "Adult" },
        )
        val shownIds = content.hero.map { it.id } + content.sections.flatMap { s -> s.items.map { it.contentId } }
        assertTrue("h1" !in shownIds && "a1" !in shownIds && "c1" !in shownIds)
        assertTrue("c2" in shownIds)
    }

    @Test
    fun `hero falls back to newest movies when none have artwork`() {
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = emptyList(),
                newestMovies = movies("n1", "n2", "n3", "n4", "n5", "n6"),
            ),
        )
        assertEquals(listOf("n1", "n2", "n3", "n4", "n5"), content.hero.map { it.id })
        // All newest went to the hero, so New movies has nothing left to show.
        assertTrue(content.sections.none { it.id == HomeSectionIds.NEW_MOVIES })
    }

    @Test
    fun `next episode card points at the next episode of its series`() {
        val content = build(
            personal = HomePersonalSnapshot(
                nextEpisodes = listOf(HomeNextEpisode(series("s1"), episode("e5", "s1", season = 2, number = 5))),
            ),
        )
        val card = content.sections.single().items.single()
        assertEquals("e5", card.contentId)
        assertEquals("episode", card.contentType)
        assertEquals("s1", card.seriesId)
        assertEquals("Series s1", card.title)
        assertEquals("S2E5 · Episode 5", card.subtitle)
    }

    @Test
    fun `favorite update reaches every section holding the item`() {
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = movies("h1"),
                newestMovies = movies("n1", "n2", "n3", "n4"),
                topRatedMovies = movies("n1", "t2", "t3", "t4"),
            ),
        )
        val updated = content.sections.withFavorite(
            contentId = "n1",
            type = com.iptvcinema.tv.core.model.FavoriteContentType.MOVIE,
            isFavorite = true,
        )
        val flags = updated.flatMap { s -> s.items.filter { it.contentId == "n1" }.map { it.isFavorite } }
        assertEquals(listOf(true, true), flags)
    }
}
