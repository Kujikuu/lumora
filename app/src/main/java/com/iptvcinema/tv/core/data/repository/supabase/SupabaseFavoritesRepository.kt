package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.core.data.local.CloudUserDataCache
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.supabase.dto.FavoriteDto
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import com.iptvcinema.tv.core.supabase.realtime.SupabaseRealtimeCoordinator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
class SupabaseFavoritesRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val cloudUserDataCache: CloudUserDataCache,
    private val realtimeCoordinator: SupabaseRealtimeCoordinator,
) : FavoritesRepository {
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        refreshTrigger.tryEmit(Unit)
    }

    override fun observeFavorites(profileId: String): Flow<List<FavoriteItem>> = flow {
        cloudUserDataCache.getFavorites(profileId)?.let { emit(it) }
        emitAll(
            merge(
                refreshTrigger,
                realtimeCoordinator.favoritesChanges(),
            ).flatMapLatest {
                flow {
                    val favorites = runCatching { getFavorites(profileId) }
                        .onSuccess { cloudUserDataCache.saveFavorites(profileId, it) }
                        .getOrDefault(cloudUserDataCache.getFavorites(profileId).orEmpty())
                    emit(favorites)
                }
            },
        )
    }

    override suspend fun isFavorite(
        profileId: String,
        contentId: String,
        contentType: FavoriteContentType,
    ): Boolean = runCatching {
        supabaseClient.from(TABLE)
            .select(Columns.list(COLUMN_ID)) {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                    eq(COLUMN_CONTENT_ID, contentId)
                    eq(COLUMN_CONTENT_TYPE, contentType.name)
                }
            }
            .decodeList<FavoriteIdDto>()
            .isNotEmpty()
    }.getOrDefault(false)

    override suspend fun toggleFavorite(
        profileId: String,
        contentId: String,
        contentType: FavoriteContentType,
        title: String,
        posterUrl: String?,
        sourceId: String?,
        currentlyFavorite: Boolean?,
    ): Boolean {
        val userId = requireUserId()
        val shouldRemove = currentlyFavorite ?: isFavorite(profileId, contentId, contentType)
        if (shouldRemove) {
            supabaseClient.from(TABLE)
                .delete {
                    filter {
                        eq(COLUMN_PROFILE_ID, profileId)
                        eq(COLUMN_CONTENT_ID, contentId)
                        eq(COLUMN_CONTENT_TYPE, contentType.name)
                    }
                }
            refreshTrigger.emit(Unit)
            return false
        }

        val insert = FavoriteInsertDto(
            userId = userId,
            profileId = profileId,
            sourceId = sourceId,
            contentId = contentId,
            contentType = contentType.name,
            title = title,
            posterUrl = posterUrl,
        )
        supabaseClient.from(TABLE).insert(insert)
        refreshTrigger.emit(Unit)
        return true
    }

    override suspend fun removeFavorite(profileId: String, favorite: FavoriteItem) {
        supabaseClient.from(TABLE)
            .delete {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                    eq(COLUMN_CONTENT_ID, favorite.contentId)
                    eq(COLUMN_CONTENT_TYPE, favorite.contentType.name)
                }
            }
        refreshTrigger.emit(Unit)
    }

    suspend fun refresh(profileId: String) {
        runCatching { getFavorites(profileId) }
            .onSuccess { cloudUserDataCache.saveFavorites(profileId, it) }
        refreshTrigger.emit(Unit)
    }

    private suspend fun getFavorites(profileId: String): List<FavoriteItem> =
        supabaseClient.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                }
                order(COLUMN_CREATED_AT, Order.DESCENDING)
                limit(FAVORITES_PAGE_SIZE.toLong())
            }
            .decodeList<FavoriteDto>()
            .map { it.toDomain() }

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated")

    @Serializable
    private data class FavoriteIdDto(val id: String)

    @Serializable
    private data class FavoriteInsertDto(
        @SerialName("user_id") val userId: String,
        @SerialName("profile_id") val profileId: String,
        @SerialName("source_id") val sourceId: String? = null,
        @SerialName("content_id") val contentId: String,
        @SerialName("content_type") val contentType: String,
        val title: String,
        @SerialName("poster_url") val posterUrl: String? = null,
    )

    companion object {
        private const val TABLE = "favorites"
        private const val COLUMN_ID = "id"
        private const val COLUMN_PROFILE_ID = "profile_id"
        private const val COLUMN_CONTENT_ID = "content_id"
        private const val COLUMN_CONTENT_TYPE = "content_type"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val FAVORITES_PAGE_SIZE = 500
    }
}
