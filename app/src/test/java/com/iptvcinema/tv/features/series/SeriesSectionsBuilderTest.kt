package com.iptvcinema.tv.features.series

import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.BrowseCategory
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.HomeNextEpisode
import com.iptvcinema.tv.core.model.home.SeriesBecauseYouWatched
import com.iptvcinema.tv.core.model.home.SeriesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.SeriesCategoryRail
import com.iptvcinema.tv.core.model.home.SeriesPersonalSnapshot
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import com.iptvcinema.tv.features.home.episode
import com.iptvcinema.tv.features.home.series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesSectionsBuilderTest {
    private val episodeCard = HomeContentCard(contentId = "e1", contentType = "episode", seriesId = "s1", title = "Ep")
    private val movieCard = HomeContentCard(contentId = "m1", contentType = "movie", title = "Movie")

    private fun seriesList(vararg ids: String) = ids.map { series(it) }

    private fun build(
        catalog: SeriesCatalogSnapshot = SeriesCatalogSnapshot(),
        personal: SeriesPersonalSnapshot = SeriesPersonalSnapshot(),
        continueWatching: List<HomeContentCard> = emptyList(),
        isBlocked: (String?, String?) -> Boolean = { _, _ -> false },
    ) = SeriesSectionsBuilder.build(
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
            catalog = SeriesCatalogSnapshot(
                latest = seriesList("l1", "l2", "l3", "l4"),
                topRated = seriesList("t1", "t2", "t3", "t4"),
                categories = listOf(BrowseCategory("Drama", 40)),
            ),
            personal = SeriesPersonalSnapshot(
                nextEpisodes = listOf(HomeNextEpisode(series("s9"), episode("e2", "s9", number = 2))),
                becauseYouWatched = SeriesBecauseYouWatched("Anchor", seriesList("b1", "b2", "b3", "b4")),
                categoryRails = listOf(SeriesCategoryRail("Comedy", seriesList("c1", "c2", "c3", "c4"))),
            ),
            continueWatching = listOf(episodeCard),
        )
        assertEquals(
            listOf(
                HomeSectionIds.CONTINUE,
                HomeSectionIds.NEXT_EPISODE,
                HomeSectionIds.LATEST_SERIES,
                HomeSectionIds.CATEGORIES,
                HomeSectionIds.BECAUSE_YOU_WATCHED_SERIES,
                HomeSectionIds.TOP_RATED_SERIES,
                HomeSectionIds.category("Comedy"),
            ),
            sections.map { it.id },
        )
    }

    @Test
    fun `continue watching keeps only episodes`() {
        val sections = build(continueWatching = listOf(movieCard, episodeCard))
        assertEquals(listOf("e1"), sections.single().items.map { it.contentId })
    }

    @Test
    fun `watched series are not recommended again`() {
        val sections = build(
            personal = SeriesPersonalSnapshot(
                becauseYouWatched = SeriesBecauseYouWatched("Anchor", seriesList("w1", "b1", "b2", "b3", "b4")),
                watchedSeriesIds = setOf("w1"),
            ),
        )
        val because = sections.single { it.id == HomeSectionIds.BECAUSE_YOU_WATCHED_SERIES }
        assertEquals(listOf("b1", "b2", "b3", "b4"), because.items.map { it.contentId })
    }

    @Test
    fun `a series shows once across discovery rails`() {
        val sections = build(
            catalog = SeriesCatalogSnapshot(latest = seriesList("l1", "l2", "l3", "l4")),
            personal = SeriesPersonalSnapshot(
                categoryRails = listOf(SeriesCategoryRail("Drama", seriesList("l1", "d1", "d2", "d3", "d4"))),
            ),
        )
        val drama = sections.single { it.id == HomeSectionIds.category("Drama") }
        assertEquals(listOf("d1", "d2", "d3", "d4"), drama.items.map { it.contentId })
    }

    @Test
    fun `blocked next episodes and categories are left out`() {
        val sections = build(
            catalog = SeriesCatalogSnapshot(categories = listOf(BrowseCategory("Kids", 3), BrowseCategory("Adult", 3))),
            personal = SeriesPersonalSnapshot(
                nextEpisodes = listOf(HomeNextEpisode(series("s9", category = "Adult"), episode("e2", "s9"))),
            ),
            isBlocked = { category, _ -> category == "Adult" },
        )
        assertTrue(sections.none { it is HomeSection.NextEpisode })
        val tiles = sections.filterIsInstance<HomeSection.Categories>().single().items
        assertEquals(listOf("Kids"), tiles.map { it.title })
        assertEquals(BrowseCardTypes.SERIES_CATEGORY, tiles.single().contentType)
    }
}
