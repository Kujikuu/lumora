package com.iptvcinema.tv.features.mylist

import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyListFavoritesPartitionTest {

    @Test
    fun `partitionFavorites separates channels from vod items`() {
        val movie = favorite("movie-1", FavoriteContentType.MOVIE)
        val series = favorite("series-1", FavoriteContentType.SERIES)
        val episode = favorite("episode-1", FavoriteContentType.EPISODE)
        val channel = favorite("channel-1", FavoriteContentType.CHANNEL)

        val partitioned = partitionFavorites(listOf(movie, series, episode, channel))

        assertEquals(listOf(movie, series, episode), partitioned.watchLater)
        assertEquals(listOf(channel), partitioned.channelFavorites)
    }

    @Test
    fun `partitionFavorites returns empty lists when input is empty`() {
        val partitioned = partitionFavorites(emptyList())

        assertTrue(partitioned.watchLater.isEmpty())
        assertTrue(partitioned.channelFavorites.isEmpty())
    }

    @Test
    fun `partitionFavorites keeps only channels in channel bucket`() {
        val channelA = favorite("channel-a", FavoriteContentType.CHANNEL)
        val channelB = favorite("channel-b", FavoriteContentType.CHANNEL)

        val partitioned = partitionFavorites(listOf(channelA, channelB))

        assertTrue(partitioned.watchLater.isEmpty())
        assertEquals(listOf(channelA, channelB), partitioned.channelFavorites)
    }

    private fun favorite(contentId: String, contentType: FavoriteContentType): FavoriteItem =
        FavoriteItem(
            id = "fav-$contentId",
            profileId = "profile-1",
            sourceId = "source-1",
            contentId = contentId,
            contentType = contentType,
            title = contentId,
            posterUrl = null,
        )
}
