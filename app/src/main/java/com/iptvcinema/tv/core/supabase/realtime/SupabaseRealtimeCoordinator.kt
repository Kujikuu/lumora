package com.iptvcinema.tv.core.supabase.realtime

import com.iptvcinema.tv.core.data.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class SupabaseRealtimeCoordinator @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val favoritesTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val watchHistoryTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val settingsTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun start() {
        scope.launch {
            while (isActive) {
                if (!authRepository.isConfigured() || !authRepository.hasActiveSession()) {
                    delay(RECONNECT_DELAY_MS)
                    continue
                }
                val userId = supabaseClient.auth.currentUserOrNull()?.id
                if (userId.isNullOrBlank()) {
                    delay(RECONNECT_DELAY_MS)
                    continue
                }
                runCatching {
                    subscribeForUser(userId)
                }.onFailure {
                    pollingFallbackLoop()
                }
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    fun favoritesChanges(): Flow<Unit> = favoritesTrigger.asSharedFlow()

    fun watchHistoryChanges(): Flow<Unit> = watchHistoryTrigger.asSharedFlow()

    fun settingsChanges(): Flow<Unit> = settingsTrigger.asSharedFlow()

    private suspend fun subscribeForUser(userId: String) = coroutineScope {
        val channel = supabaseClient.realtime.channel("cloud-user-data-$userId")
        val favoritesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = TABLE_FAVORITES
        }
        val historyFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = TABLE_WATCH_HISTORY
        }
        val settingsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = TABLE_USER_SETTINGS
        }

        channel.subscribe(blockUntilSubscribed = true)

        launch {
            favoritesFlow.collect { favoritesTrigger.emit(Unit) }
        }
        launch {
            historyFlow.collect { watchHistoryTrigger.emit(Unit) }
        }
        launch {
            settingsFlow.collect { settingsTrigger.emit(Unit) }
        }

        while (isActive && authRepository.hasActiveSession()) {
            delay(FALLBACK_POLL_MS)
            emitPollingFallback()
        }
    }

    private suspend fun pollingFallbackLoop() = coroutineScope {
        while (isActive && authRepository.hasActiveSession()) {
            emitPollingFallback()
            delay(FALLBACK_POLL_MS)
        }
    }

    private suspend fun emitPollingFallback() {
        favoritesTrigger.emit(Unit)
        watchHistoryTrigger.emit(Unit)
        settingsTrigger.emit(Unit)
    }

    companion object {
        private const val TABLE_FAVORITES = "favorites"
        private const val TABLE_WATCH_HISTORY = "watch_history"
        private const val TABLE_USER_SETTINGS = "user_settings"
        private const val RECONNECT_DELAY_MS = 5_000L
        private const val FALLBACK_POLL_MS = 60_000L
    }
}
