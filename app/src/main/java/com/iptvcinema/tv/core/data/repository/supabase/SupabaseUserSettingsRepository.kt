package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.core.data.local.CloudUserDataCache
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.supabase.dto.UserSettingsDto
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import com.iptvcinema.tv.core.supabase.mapper.toDto
import com.iptvcinema.tv.core.supabase.realtime.SupabaseRealtimeCoordinator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

@Singleton
class SupabaseUserSettingsRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val cloudUserDataCache: CloudUserDataCache,
    private val realtimeCoordinator: SupabaseRealtimeCoordinator,
) : UserSettingsRepository {
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        refreshTrigger.tryEmit(Unit)
    }

    override fun observeSettings(): Flow<UserSettings?> = flow {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
        userId?.let { cloudUserDataCache.getUserSettings(it) }?.let { emit(it) }
        emitAll(
            merge(
                refreshTrigger,
                realtimeCoordinator.settingsChanges(),
            ).flatMapLatest {
                flow {
                    val settings = runCatching { getSettings() }
                        .onSuccess { setting ->
                            setting?.let { cloudUserDataCache.saveUserSettings(it) }
                        }
                        .getOrNull()
                        ?: userId?.let { cloudUserDataCache.getUserSettings(it) }
                    emit(settings)
                }
            },
        )
    }

    override suspend fun getSettings(): UserSettings? {
        val userId = requireUserId()
        return supabaseClient.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq(COLUMN_USER_ID, userId)
                }
            }
            .decodeSingleOrNull<UserSettingsDto>()
            ?.toDomain()
            ?.also { cloudUserDataCache.saveUserSettings(it) }
    }

    override suspend fun updateSettings(settings: UserSettings) {
        val userId = requireUserId()
        supabaseClient.from(TABLE)
            .update(settings.toDto(userId)) {
                filter {
                    eq(COLUMN_USER_ID, userId)
                }
            }
        cloudUserDataCache.saveUserSettings(settings)
        refreshTrigger.emit(Unit)
    }

    suspend fun refresh() {
        runCatching { getSettings() }
        refreshTrigger.emit(Unit)
    }

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated")

    companion object {
        private const val TABLE = "user_settings"
        private const val COLUMN_USER_ID = "user_id"
    }
}
