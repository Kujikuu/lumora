package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.util.AccountDisplayNameResolver
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val appSessionRepository: AppSessionRepository,
) : AuthRepository {
    override val isAuthenticated: Flow<Boolean> = supabaseClient.auth.sessionStatus.map { status ->
        status is SessionStatus.Authenticated
    }

    override val currentUserId: Flow<String?> = supabaseClient.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.id
    }

    override suspend fun currentUserEmail(): String? =
        supabaseClient.auth.currentSessionOrNull()?.user?.email

    override suspend fun currentUserDisplayName(): String? {
        val user = supabaseClient.auth.currentSessionOrNull()?.user ?: return null
        val metadata = user.userMetadata?.mapValues { (_, value) -> value } ?: emptyMap()
        return AccountDisplayNameResolver.resolve(
            email = user.email,
            metadata = metadata,
        )
    }

    override suspend fun awaitAuthInitialization() {
        if (!isConfigured()) return
        supabaseClient.auth.sessionStatus.first { it !is SessionStatus.Initializing }
    }

    override suspend fun syncSessionToLocal() {
        awaitAuthInitialization()
        val session = supabaseClient.auth.currentSessionOrNull()
        if (session != null) {
            appSessionRepository.setAuthenticated(
                authenticated = true,
                userId = session.user?.id,
            )
        } else {
            appSessionRepository.setAuthenticated(authenticated = false, userId = null)
        }
    }

    override suspend fun importSession(accessToken: String, refreshToken: String) {
        supabaseClient.auth.importAuthToken(accessToken = accessToken, refreshToken = refreshToken)
        syncSessionToLocal()
    }

    override suspend fun hasActiveSession(): Boolean {
        syncSessionToLocal()
        return supabaseClient.auth.currentSessionOrNull() != null
    }

    override suspend fun signOut() {
        supabaseClient.auth.signOut()
        appSessionRepository.clearSession()
    }

    override fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
}
