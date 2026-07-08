package com.iptvcinema.tv.features.mylist

import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem

data class FavoriteChannelCard(
    val id: String,
    val contentId: String,
    val name: String,
    val logoUrl: String?,
    val currentProgram: String? = null,
    val favorite: FavoriteItem,
)

data class PartitionedFavorites(
    val watchLater: List<FavoriteItem>,
    val channelFavorites: List<FavoriteItem>,
)

fun partitionFavorites(favorites: List<FavoriteItem>): PartitionedFavorites {
    val watchLater = favorites.filter { it.contentType != FavoriteContentType.CHANNEL }
    val channelFavorites = favorites.filter { it.contentType == FavoriteContentType.CHANNEL }
    return PartitionedFavorites(watchLater = watchLater, channelFavorites = channelFavorites)
}
