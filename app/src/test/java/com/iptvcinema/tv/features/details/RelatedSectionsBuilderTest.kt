package com.iptvcinema.tv.features.details

import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.RelatedItem
import com.iptvcinema.tv.core.model.home.RelatedSnapshot
import com.iptvcinema.tv.features.home.HomeSectionIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedSectionsBuilderTest {
    private fun item(id: String, category: String? = "Drama", rating: String? = null) =
        RelatedItem(HomeContentCard(contentId = id, contentType = "movie", title = "Movie $id"), category, rating)

    private fun items(vararg ids: String) = ids.map { item(it) }

    private fun build(
        snapshot: RelatedSnapshot,
        isBlocked: (String?, String?) -> Boolean = { _, _ -> false },
        isFavorite: (String, FavoriteContentType) -> Boolean = { _, _ -> false },
    ) = RelatedSectionsBuilder.build(RelatedKind.Movie, snapshot, isBlocked, isFavorite)

    @Test
    fun `similar first, then top rated and new, never the title itself or a repeat`() {
        val sections = build(
            RelatedSnapshot(
                anchorId = "a",
                anchorTitle = "Anchor",
                anchorCategory = "Drama",
                similar = items("a", "s1", "s2"),
                topRated = items("s1", "t1", "t2", "t3", "t4"),
                newest = items("t1", "n1", "n2", "n3", "n4"),
            ),
        )
        assertEquals(
            listOf(RelatedSectionIds.SIMILAR, HomeSectionIds.TOP_RATED_MOVIES, HomeSectionIds.NEW_MOVIES),
            sections.map { it.id },
        )
        assertEquals(listOf("s1", "s2"), sections[0].items.map { it.contentId })
        assertEquals(listOf("t1", "t2", "t3", "t4"), sections[1].items.map { it.contentId })
        assertEquals(listOf("n1", "n2", "n3", "n4"), sections[2].items.map { it.contentId })
    }

    @Test
    fun `a single similar title still shows, short secondary rails do not`() {
        val sections = build(
            RelatedSnapshot("a", "Anchor", "Drama", similar = items("s1"), topRated = items("t1", "t2")),
        )
        assertEquals(listOf(RelatedSectionIds.SIMILAR), sections.map { it.id })
    }

    @Test
    fun `blocked titles are left out and favorites are marked`() {
        val sections = build(
            RelatedSnapshot(
                anchorId = "a",
                anchorTitle = "Anchor",
                anchorCategory = "Drama",
                similar = listOf(item("s1"), item("x1", category = "Adult"), item("r1", rating = "R")),
            ),
            isBlocked = { category, rating -> category == "Adult" || rating == "R" },
            isFavorite = { id, type -> id == "s1" && type == FavoriteContentType.MOVIE },
        )
        val cards = sections.single().items
        assertEquals(listOf("s1"), cards.map { it.contentId })
        assertTrue(cards.single().isFavorite)
    }
}
