package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.model.UserSettings
import com.iptvcinema.tv.core.supabase.dto.UserSettingsDto
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import com.iptvcinema.tv.core.supabase.mapper.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class SupabaseUserSettingsRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : UserSettingsRepository {
    override fun observeSettings(): Flow<UserSettings?> = flow {
        emit(getSettings())
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
    }

    override suspend fun updateSettings(settings: UserSettings) {
        val userId = requireUserId()
        supabaseClient.from(TABLE)
            .update(settings.toDto(userId)) {
                filter {
                    eq(COLUMN_USER_ID, userId)
                }
            }
    }

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated")

    companion object {
        private const val TABLE = "user_settings"
        private const val COLUMN_USER_ID = "user_id"
    }
}
