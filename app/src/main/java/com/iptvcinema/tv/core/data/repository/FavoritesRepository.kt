package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(profileId: String): Flow<List<FavoriteItem>>
    suspend fun isFavorite(profileId: String, contentId: String, contentType: FavoriteContentType): Boolean
    suspend fun toggleFavorite(
        profileId: String,
        contentId: String,
        contentType: FavoriteContentType,
        title: String,
        posterUrl: String?,
        sourceId: String? = null,
        currentlyFavorite: Boolean? = null,
    ): Boolean

    suspend fun removeFavorite(profileId: String, favorite: FavoriteItem)
}
