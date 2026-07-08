package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.core.data.local.CloudUserDataCache
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.player.ContinueWatchingResolver
import com.iptvcinema.tv.core.supabase.dto.WatchHistoryDto
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import com.iptvcinema.tv.core.supabase.realtime.SupabaseRealtimeCoordinator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
class SupabaseWatchHistoryRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val continueWatchingResolver: ContinueWatchingResolver,
    private val appSessionRepository: AppSessionRepository,
    private val cloudUserDataCache: CloudUserDataCache,
    private val realtimeCoordinator: SupabaseRealtimeCoordinator,
) : WatchHistoryRepository {
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val upsertMutex = Mutex()
    private var lastUpsertKey: String? = null
    private var lastUpsertPositionMs: Long = -1L
    private var lastUpsertAtMs: Long = 0L

    init {
        refreshTrigger.tryEmit(Unit)
    }

    override fun observeHistory(profileId: String, limit: Int): Flow<List<WatchHistoryItem>> = flow {
        cloudUserDataCache.getWatchHistory(profileId, limit)?.let { emit(it) }
        emitAll(
            merge(
                refreshTrigger,
                realtimeCoordinator.watchHistoryChanges(),
            ).flatMapLatest {
                flow {
                    val history = runCatching { getHistory(profileId, limit) }
                        .onSuccess { cloudUserDataCache.saveWatchHistory(profileId, it) }
                        .getOrDefault(cloudUserDataCache.getWatchHistory(profileId, limit).orEmpty())
                    emit(history)
                }
            },
        )
    }

    override fun observeContinueWatching(profileId: String, limit: Int): Flow<List<WatchHistoryItem>> = flow {
        cloudUserDataCache.getWatchHistory(profileId, CONTINUE_WATCHING_FETCH_LIMIT)?.let { cached ->
            emit(resolveContinueWatching(cached, limit))
        }
        emitAll(
            merge(
                refreshTrigger,
                realtimeCoordinator.watchHistoryChanges(),
            ).flatMapLatest {
                flow {
                    val items = runCatching { loadContinueWatching(profileId, limit) }
                        .getOrDefault(emptyList())
                    emit(items)
                }
            },
        )
    }

    override fun invalidate() {
        refreshTrigger.tryEmit(Unit)
    }

    override suspend fun getProgress(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
    ): WatchHistoryItem? =
        supabaseClient.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                    eq(COLUMN_CONTENT_ID, contentId)
                    eq(COLUMN_CONTENT_TYPE, contentType.name)
                }
                limit(1)
            }
            .decodeList<WatchHistoryDto>()
            .firstOrNull()
            ?.toDomain()

    override suspend fun upsertProgress(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
        title: String,
        posterUrl: String?,
        positionMs: Long,
        durationMs: Long?,
        sourceId: String?,
        seriesId: String?,
    ) {
        val upsertKey = "$profileId:$contentId:${contentType.name}"
        val nowMs = System.currentTimeMillis()
        val shouldSkip = upsertMutex.withLock {
            val skip = upsertKey == lastUpsertKey &&
                kotlin.math.abs(positionMs - lastUpsertPositionMs) < MIN_POSITION_DELTA_MS &&
                nowMs - lastUpsertAtMs < MIN_UPSERT_INTERVAL_MS
            if (!skip) {
                lastUpsertKey = upsertKey
                lastUpsertPositionMs = positionMs
                lastUpsertAtMs = nowMs
            }
            skip
        }
        if (shouldSkip) return

        val userId = requireUserId()
        val watchedPercentage = durationMs?.takeIf { it > 0 }?.let { positionMs.toDouble() / it.toDouble() }
        val upsert = WatchHistoryUpsertDto(
            userId = userId,
            profileId = profileId,
            sourceId = sourceId,
            contentId = contentId,
            contentType = contentType.name,
            seriesId = seriesId,
            title = title,
            posterUrl = posterUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            watchedPercentage = watchedPercentage,
            lastWatchedAt = Instant.now().toString(),
        )
        supabaseClient.from(TABLE).upsert(upsert) {
            onConflict = "profile_id,content_id,content_type"
        }
        refreshTrigger.emit(Unit)
    }

    override suspend fun getDistinctSeriesIds(profileId: String): List<String> =
        supabaseClient.from(TABLE)
            .select(Columns.list(COLUMN_SERIES_ID)) {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                    eq(COLUMN_CONTENT_TYPE, WatchHistoryContentType.EPISODE.name)
                    filter(COLUMN_SERIES_ID, FilterOperator.IS, "NOT.NULL")
                }
                limit(SERIES_ID_FETCH_LIMIT.toLong())
            }
            .decodeList<WatchHistorySeriesIdDto>()
            .mapNotNull { it.seriesId?.takeIf { id -> id.isNotBlank() } }
            .distinct()

    override suspend fun remove(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
    ) {
        supabaseClient.from(TABLE)
            .delete {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                    eq(COLUMN_CONTENT_ID, contentId)
                    eq(COLUMN_CONTENT_TYPE, contentType.name)
                }
            }
        refreshTrigger.emit(Unit)
    }

    suspend fun refresh(profileId: String) {
        runCatching { getHistory(profileId, CONTINUE_WATCHING_FETCH_LIMIT) }
            .onSuccess { cloudUserDataCache.saveWatchHistory(profileId, it) }
        refreshTrigger.emit(Unit)
    }

    private suspend fun loadContinueWatching(profileId: String, limit: Int): List<WatchHistoryItem> {
        val history = getHistory(profileId, limit = CONTINUE_WATCHING_FETCH_LIMIT)
        cloudUserDataCache.saveWatchHistory(profileId, history)
        return resolveContinueWatching(history, limit)
    }

    private suspend fun resolveContinueWatching(
        history: List<WatchHistoryItem>,
        limit: Int,
    ): List<WatchHistoryItem> {
        val sourceId = runCatching {
            appSessionRepository.sessionState.first().currentSourceId
        }.getOrNull()
        return continueWatchingResolver.resolve(
            history = history,
            sourceId = sourceId,
            limit = limit,
        )
    }

    private suspend fun getHistory(profileId: String, limit: Int): List<WatchHistoryItem> =
        supabaseClient.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                }
                order(COLUMN_LAST_WATCHED_AT, Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<WatchHistoryDto>()
            .map { it.toDomain() }

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated")

    @Serializable
    private data class WatchHistorySeriesIdDto(
        @SerialName("series_id") val seriesId: String? = null,
    )

    @Serializable
    private data class WatchHistoryUpsertDto(
        @SerialName("user_id") val userId: String,
        @SerialName("profile_id") val profileId: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @SerialName("source_id") val sourceId: String? = null,
        @SerialName("content_id") val contentId: String,
        @SerialName("content_type") val contentType: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @SerialName("series_id") val seriesId: String? = null,
        val title: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @SerialName("poster_url") val posterUrl: String? = null,
        @SerialName("position_ms") val positionMs: Long,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @SerialName("duration_ms") val durationMs: Long? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @SerialName("watched_percentage") val watchedPercentage: Double? = null,
        @SerialName("last_watched_at") val lastWatchedAt: String,
    )

    companion object {
        private const val TABLE = "watch_history"
        private const val COLUMN_PROFILE_ID = "profile_id"
        private const val COLUMN_CONTENT_ID = "content_id"
        private const val COLUMN_CONTENT_TYPE = "content_type"
        private const val COLUMN_SERIES_ID = "series_id"
        private const val COLUMN_LAST_WATCHED_AT = "last_watched_at"
        private const val CONTINUE_WATCHING_FETCH_LIMIT = 50
        private const val SERIES_ID_FETCH_LIMIT = 100
        private const val MIN_POSITION_DELTA_MS = 5_000L
        private const val MIN_UPSERT_INTERVAL_MS = 5_000L
    }
}
