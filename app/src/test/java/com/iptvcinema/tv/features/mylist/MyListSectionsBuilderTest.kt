package com.iptvcinema.tv.features.mylist

import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyListSectionsBuilderTest {
    private fun entry(
        id: String,
        type: FavoriteContentType,
        category: String? = null,
        rating: String? = null,
    ) = MyListEntry(
        favorite = FavoriteItem(
            id = "f-$id",
            profileId = "p",
            sourceId = "src",
            contentId = id,
            contentType = type,
            title = "Title $id",
            posterUrl = null,
        ),
        card = HomeContentCard(contentId = id, contentType = type.cardType(), title = "Title $id", isFavorite = true),
        categoryName = category,
        rating = rating,
    )

    private val continueCard = HomeContentCard(contentId = "cw1", contentType = "movie", title = "In progress")

    @Test
    fun `saved rails come first by type, then continue watching`() {
        val sections = MyListSectionsBuilder.build(
            saved = MyListSaved(
                movies = listOf(entry("m1", FavoriteContentType.MOVIE)),
                series = listOf(entry("s1", FavoriteContentType.SERIES)),
                episodes = listOf(entry("e1", FavoriteContentType.EPISODE)),
                channels = listOf(entry("c1", FavoriteContentType.CHANNEL)),
            ),
            continueWatching = listOf(continueCard),
            isBlocked = { _, _ -> false },
        )
        assertEquals(
            listOf(
                MyListSectionIds.MOVIES,
                MyListSectionIds.SERIES,
                MyListSectionIds.EPISODES,
                MyListSectionIds.CHANNELS,
                HomeSectionIds.CONTINUE,
            ),
            sections.map { it.id },
        )
        assertTrue(sections.single { it.id == MyListSectionIds.CHANNELS } is HomeSection.Channels)
    }

    @Test
    fun `saved order is kept within a rail`() {
        val sections = MyListSectionsBuilder.build(
            saved = MyListSaved(movies = listOf("m3", "m1", "m2").map { entry(it, FavoriteContentType.MOVIE) }),
            continueWatching = emptyList(),
            isBlocked = { _, _ -> false },
        )
        assertEquals(listOf("m3", "m1", "m2"), sections.single().items.map { it.contentId })
    }

    @Test
    fun `titles blocked by category or rating are hidden`() {
        val sections = MyListSectionsBuilder.build(
            saved = MyListSaved(
                movies = listOf(
                    entry("m1", FavoriteContentType.MOVIE, category = "Drama"),
                    entry("m2", FavoriteContentType.MOVIE, category = "Adult"),
                    entry("m3", FavoriteContentType.MOVIE, rating = "R"),
                ),
                channels = listOf(entry("c1", FavoriteContentType.CHANNEL, category = "Adult")),
            ),
            continueWatching = emptyList(),
            isBlocked = { category, rating -> category == "Adult" || rating == "R" },
        )
        assertEquals(listOf("m1"), sections.single().items.map { it.contentId })
    }

    @Test
    fun `removed favorites disappear at once without touching same id of another type`() {
        val sections = MyListSectionsBuilder.build(
            saved = MyListSaved(
                movies = listOf(entry("42", FavoriteContentType.MOVIE)),
                channels = listOf(entry("42", FavoriteContentType.CHANNEL)),
            ),
            continueWatching = emptyList(),
            isBlocked = { _, _ -> false },
            hiddenKeys = setOf("movie:42"),
        )
        assertEquals(listOf(MyListSectionIds.CHANNELS), sections.map { it.id })
    }

    @Test
    fun `an empty list has no rails`() {
        val sections = MyListSectionsBuilder.build(MyListSaved(), emptyList(), { _, _ -> false })
        assertTrue(sections.isEmpty())
    }
}
