package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.UserProfile

interface ProfilesRepository {
    suspend fun getProfiles(): List<UserProfile>
    suspend fun ensureDefaultProfile(): UserProfile?
    suspend fun createProfile(name: String, type: String): UserProfile
    suspend fun updateProfile(profileId: String, name: String): UserProfile
    suspend fun deleteProfile(profileId: String)
}
