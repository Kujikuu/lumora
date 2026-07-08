package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.core.data.local.CloudUserDataCache
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.supabase.dto.ParentalControlsDto
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import com.iptvcinema.tv.core.supabase.mapper.toDto
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
class SupabaseParentalControlsRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val cloudUserDataCache: CloudUserDataCache,
) : ParentalControlsRepository {
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        refreshTrigger.tryEmit(Unit)
    }

    override fun observeControls(profileId: String): Flow<ParentalControls?> = flow {
        cloudUserDataCache.getParentalControls(profileId)?.let { emit(it) }
        emitAll(
            refreshTrigger.flatMapLatest {
                flow {
                    val controls = runCatching { getControls(profileId) }
                        .onSuccess { control ->
                            control?.let { cloudUserDataCache.saveParentalControls(it) }
                        }
                        .getOrNull()
                        ?: cloudUserDataCache.getParentalControls(profileId)
                    emit(controls)
                }
            },
        )
    }

    override suspend fun getControls(profileId: String): ParentalControls? {
        val userId = requireUserId()
        return supabaseClient.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq(COLUMN_PROFILE_ID, profileId)
                    eq(COLUMN_USER_ID, userId)
                }
            }
            .decodeSingleOrNull<ParentalControlsDto>()
            ?.toDomain()
            ?.also { cloudUserDataCache.saveParentalControls(it) }
    }

    override suspend fun updateControls(controls: ParentalControls) {
        val userId = requireUserId()
        supabaseClient.from(TABLE)
            .update(controls.toDto()) {
                filter {
                    eq(COLUMN_ID, controls.id)
                    eq(COLUMN_USER_ID, userId)
                }
            }
        cloudUserDataCache.saveParentalControls(controls)
        refreshTrigger.emit(Unit)
    }

    override suspend fun ensureControls(profileId: String): ParentalControls {
        return getControls(profileId) ?: run {
            val userId = requireUserId()
            val insert = ParentalControlsInsertDto(
                userId = userId,
                profileId = profileId,
            )
            val created = supabaseClient.from(TABLE)
                .insert(insert) {
                    select(Columns.ALL)
                }
                .decodeSingle<ParentalControlsDto>()
                .toDomain()
            cloudUserDataCache.saveParentalControls(created)
            refreshTrigger.emit(Unit)
            created
        }
    }

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated")

    @Serializable
    private data class ParentalControlsInsertDto(
        @SerialName("user_id") val userId: String,
        @SerialName("profile_id") val profileId: String,
    )

    companion object {
        private const val TABLE = "parental_controls"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_PROFILE_ID = "profile_id"
    }
}
