package com.iptvcinema.tv.core.supabase.realtime

import android.util.Log
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.core.data.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One realtime channel for the signed-in user. It follows [AuthRepository.currentUserId]:
 * a new user cancels the previous channel (which is then removed), sign-out closes it.
 * While realtime is down it retries with backoff and asks screens to refetch instead.
 */
@Singleton
class SupabaseRealtimeCoordinator @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val favoritesTrigger = triggerFlow()
    private val watchHistoryTrigger = triggerFlow()
    private val settingsTrigger = triggerFlow()
    private var job: Job? = null

    fun start() {
        if (job != null || !authRepository.isConfigured()) return
        job = scope.launch {
            authRepository.currentUserId.collectLatest { userId ->
                if (userId != null) runForUser(userId)
            }
        }
    }

    fun favoritesChanges(): Flow<Unit> = favoritesTrigger.asSharedFlow()

    fun watchHistoryChanges(): Flow<Unit> = watchHistoryTrigger.asSharedFlow()

    fun settingsChanges(): Flow<Unit> = settingsTrigger.asSharedFlow()

    private suspend fun runForUser(userId: String) {
        var failures = 0
        while (true) {
            val wasConnected = try {
                listen(userId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Realtime subscribe failed: ${error.safeSummary()}")
                false
            }
            failures = if (wasConnected) 0 else failures + 1
            // Realtime is down: refetch so screens stay fresh, then retry with backoff.
            emitRefreshAll()
            delay(retryDelayMs(failures))
        }
    }

    /** Suspends while the channel is healthy. Returns true if it managed to subscribe. */
    private suspend fun listen(userId: String): Boolean = coroutineScope {
        val channel = supabaseClient.realtime.channel("cloud-user-data-$userId")
        try {
            val favorites = channel.changesFor(TABLE_FAVORITES, userId)
            val history = channel.changesFor(TABLE_WATCH_HISTORY, userId)
            val settings = channel.changesFor(TABLE_USER_SETTINGS, userId)

            channel.subscribe(blockUntilSubscribed = true)
            // Catch up on anything that changed while we were not listening.
            emitRefreshAll()

            launch { favorites.collect { favoritesTrigger.emit(Unit) } }
            launch { history.collect { watchHistoryTrigger.emit(Unit) } }
            launch { settings.collect { settingsTrigger.emit(Unit) } }

            channel.status.first { it != RealtimeChannel.Status.SUBSCRIBED }
            coroutineContext.cancelChildren()
            true
        } finally {
            withContext(NonCancellable) {
                runCatching {
                    channel.unsubscribe()
                    supabaseClient.realtime.removeChannel(channel)
                }
            }
        }
    }

    private fun RealtimeChannel.changesFor(tableName: String, userId: String): Flow<PostgresAction> =
        postgresChangeFlow<PostgresAction>(schema = "public") {
            table = tableName
            filter(COLUMN_USER_ID, FilterOperator.EQ, userId)
        }

    private suspend fun emitRefreshAll() {
        favoritesTrigger.emit(Unit)
        watchHistoryTrigger.emit(Unit)
        settingsTrigger.emit(Unit)
    }

    private fun retryDelayMs(failures: Int): Long =
        (MIN_RETRY_MS shl failures.coerceIn(0, 4)).coerceAtMost(MAX_RETRY_MS)

    private companion object {
        const val TAG = "RealtimeCoordinator"
        const val TABLE_FAVORITES = "favorites"
        const val TABLE_WATCH_HISTORY = "watch_history"
        const val TABLE_USER_SETTINGS = "user_settings"
        const val COLUMN_USER_ID = "user_id"
        const val MIN_RETRY_MS = 5_000L
        const val MAX_RETRY_MS = 60_000L

        fun triggerFlow() = MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }
}
