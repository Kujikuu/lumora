package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.supabase.dto.ProfileDto
import com.iptvcinema.tv.core.supabase.mapper.toUserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
class SupabaseProfilesRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : ProfilesRepository {
    override suspend fun getProfiles(): List<UserProfile> {
        val userId = requireUserId()
        return supabaseClient.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq(COLUMN_USER_ID, userId)
                }
            }
            .decodeList<ProfileDto>()
            .map { it.toUserProfile() }
    }

    override suspend fun ensureDefaultProfile(): UserProfile? {
        val profiles = getProfiles()
        if (profiles.isNotEmpty()) return profiles.first()
        return createProfile(name = "Main", type = ProfileType.MAIN.name)
    }

    override suspend fun createProfile(name: String, type: String): UserProfile {
        val userId = requireUserId()
        val insert = ProfileInsertDto(
            userId = userId,
            name = name,
            type = type,
        )
        return supabaseClient.from(TABLE)
            .insert(insert) {
                select(Columns.ALL)
            }
            .decodeSingle<ProfileDto>()
            .toUserProfile()
    }

    override suspend fun updateProfile(profileId: String, name: String): UserProfile {
        val userId = requireUserId()
        return supabaseClient.from(TABLE)
            .update(ProfileUpdateDto(name = name)) {
                filter {
                    eq(COLUMN_ID, profileId)
                    eq(COLUMN_USER_ID, userId)
                }
                select(Columns.ALL)
            }
            .decodeSingle<ProfileDto>()
            .toUserProfile()
    }

    override suspend fun deleteProfile(profileId: String) {
        val userId = requireUserId()
        supabaseClient.from(TABLE)
            .delete {
                filter {
                    eq(COLUMN_ID, profileId)
                    eq(COLUMN_USER_ID, userId)
                }
            }
    }

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated")

    @Serializable
    private data class ProfileInsertDto(
        @SerialName("user_id") val userId: String,
        val name: String,
        val type: String,
    )

    @Serializable
    private data class ProfileUpdateDto(
        val name: String,
    )

    companion object {
        private const val TABLE = "profiles"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
    }
}
