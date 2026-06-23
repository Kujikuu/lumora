package com.iptvcinema.tv.core.data.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>
    val currentUserId: Flow<String?>
    suspend fun currentUserEmail(): String?
    suspend fun awaitAuthInitialization()
    suspend fun syncSessionToLocal()
    suspend fun importSession(accessToken: String, refreshToken: String)
    suspend fun hasActiveSession(): Boolean
    suspend fun signOut()
    fun isConfigured(): Boolean
}
