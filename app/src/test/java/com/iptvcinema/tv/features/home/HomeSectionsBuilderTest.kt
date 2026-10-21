package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.home.BrowseCategory
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
        myList: List<FavoriteItem> = emptyList(),
        categoryLabel: (String) -> String = { it },
    ) = HomeSectionsBuilder.build(
        catalog = catalog,
        personal = personal,
        continueWatching = continueWatching,
        isBlocked = isBlocked,
        isFavorite = { _, _ -> false },
        myList = myList,
        categoryLabel = categoryLabel,
    )

    @Test
    fun `sections follow the fixed order and skip missing ones`() {
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = movies("h1"),
                newestMovies = movies("n1", "n2", "n3", "n4"),
            ),
            personal = HomePersonalSnapshot(
                nextEpisodes = listOf(HomeNextEpisode(series("s9"), episode("e2", "s9", number = 2))),
                becauseYouWatched = HomeBecauseYouWatched("Anchor", movies("b1", "b2", "b3", "b4")),
                categoryRails = listOf(HomeCategoryRail("Drama", movies("d1", "d2", "d3", "d4"))),
                categoryTiles = listOf(BrowseCategory("Drama", 0)),
                liveChannels = listOf(channel("c1")),
            ),
            continueWatching = listOf(continueCard),
            myList = listOf(favorite("m1", FavoriteContentType.MOVIE)),
        )
        assertEquals(
            listOf(
                HomeSectionIds.CONTINUE,
                HomeSectionIds.NEXT_EPISODE,
                HomeSectionIds.LIVE_NOW,
                HomeSectionIds.CATEGORIES,
                HomeSectionIds.NEW_MOVIES,
                HomeSectionIds.BECAUSE_YOU_WATCHED,
                HomeSectionIds.category("Drama"),
                HomeSectionIds.MY_LIST,
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
            personal = HomePersonalSnapshot(liveChannels = listOf(channel("c1"))),
            continueWatching = listOf(continueCard),
        )
        assertEquals(
            listOf(HomeSectionIds.CONTINUE, HomeSectionIds.LIVE_NOW),
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
    fun `live now shows what is on, else the cleaned category`() {
        val content = build(
            personal = HomePersonalSnapshot(
                liveChannels = listOf(channel("c1", category = "NEWS | أخبار"), channel("c2", category = "Sport")),
                nowPlaying = mapOf("c2" to "Evening match"),
            ),
            categoryLabel = { it.substringBefore(" |").lowercase() },
        )
        val live = content.sections.single()
        assertEquals(listOf("news", "Evening match"), live.items.map { it.subtitle })
    }

    @Test
    fun `category tiles and rails show labels but keep raw names for navigation`() {
        val raw = "Mohamed HENEDI | محمد هنيدي"
        val content = build(
            personal = HomePersonalSnapshot(
                categoryTiles = listOf(BrowseCategory(raw, 0, posterUrl = "https://img/p.jpg")),
                categoryRails = listOf(HomeCategoryRail(raw, movies("d1", "d2", "d3", "d4"))),
            ),
            categoryLabel = { "Mohamed Henedi" },
        )
        val tile = content.sections.first { it.id == HomeSectionIds.CATEGORIES }.items.single()
        assertEquals(raw, tile.contentId)
        assertEquals("Mohamed Henedi", tile.title)
        val rail = content.sections.first { it.id == HomeSectionIds.category(raw) }
        assertEquals("Mohamed Henedi", rail.title.text)
    }

    @Test
    fun `cam copies never lead the hero and rails show them without the tag`() {
        val content = build(
            catalog = HomeCatalogSnapshot(
                heroCandidates = listOf(movie("cam", title = "The Fix (HDTS)"), movie("h1")),
                newestMovies = listOf(movie("n1", title = "Sacrifice - CAM")) + movies("n2", "n3", "n4"),
            ),
        )
        assertEquals(listOf("h1"), content.hero.map { it.id })
        val newest = content.sections.single { it.id == HomeSectionIds.NEW_MOVIES }
        assertEquals("Sacrifice", newest.items.first().title)
    }

    @Test
    fun `my list holds saved movies and series only`() {
        val content = build(
            myList = listOf(
                favorite("m1", FavoriteContentType.MOVIE),
                favorite("s1", FavoriteContentType.SERIES),
                favorite("c1", FavoriteContentType.CHANNEL),
                favorite("e1", FavoriteContentType.EPISODE),
            ),
        )
        val myList = content.sections.single()
        assertEquals(HomeSectionIds.MY_LIST, myList.id)
        assertEquals(listOf("movie" to "m1", "series" to "s1"), myList.items.map { it.contentType to it.contentId })
        assertTrue(myList.items.all { it.isFavorite })
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
                liveChannels = listOf(channel("c1", category = "Adult"), channel("c2", category = "News")),
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
            ),
            myList = listOf(favorite("n1", FavoriteContentType.MOVIE)),
        )
        val updated = content.sections.withFavorite(
            contentId = "n1",
            type = FavoriteContentType.MOVIE,
            isFavorite = true,
        )
        val flags = updated.flatMap { s -> s.items.filter { it.contentId == "n1" }.map { it.isFavorite } }
        assertEquals(listOf(true, true), flags)
    }

    private fun favorite(id: String, type: FavoriteContentType) = FavoriteItem(
        id = "f-$id",
        profileId = "p",
        sourceId = "src",
        contentId = id,
        contentType = type,
        title = "Saved $id",
        posterUrl = "https://img/$id.jpg",
    )
}
