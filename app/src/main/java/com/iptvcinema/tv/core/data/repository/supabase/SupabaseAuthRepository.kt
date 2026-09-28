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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val appSessionRepository: AppSessionRepository,
) : AuthRepository {
    override val isAuthenticated: Flow<Boolean> = supabaseClient.auth.sessionStatus
        .map { it.hasUsableSession() }
        .distinctUntilChanged()

    // Skips transient states (initializing, refresh failing) so collectors only see real
    // account changes: a user id on sign-in, null on sign-out.
    override val currentUserId: Flow<String?> = supabaseClient.auth.sessionStatus
        .mapNotNull { status ->
            when (val action = status.toMirrorAction()) {
                is AuthMirrorAction.SignedIn -> action.userId?.let { UserIdEvent(it) }
                AuthMirrorAction.SignedOut -> UserIdEvent(null)
                AuthMirrorAction.NoChange -> null
            }
        }
        .map { it.userId }
        .distinctUntilChanged()

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
        withTimeoutOrNull(AUTH_INIT_TIMEOUT_MS) {
            supabaseClient.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        }
    }

    override suspend fun syncSessionToLocal() {
        awaitAuthInitialization()
        applyToLocal(supabaseClient.auth.sessionStatus.value)
    }

    suspend fun applyToLocal(status: SessionStatus) {
        when (val action = status.toMirrorAction()) {
            is AuthMirrorAction.SignedIn -> appSessionRepository.setAuthenticated(
                authenticated = true,
                userId = action.userId,
            )
            AuthMirrorAction.SignedOut -> appSessionRepository.setAuthenticated(authenticated = false)
            AuthMirrorAction.NoChange -> Unit
        }
    }

    override suspend fun importSession(accessToken: String, refreshToken: String) {
        supabaseClient.auth.importAuthToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            retrieveUser = true,
            autoRefresh = true,
        )
        syncSessionToLocal()
    }

    override suspend fun hasActiveSession(): Boolean {
        awaitAuthInitialization()
        return supabaseClient.auth.sessionStatus.value.hasUsableSession()
    }

    override suspend fun signOut() {
        supabaseClient.auth.signOut()
        appSessionRepository.clearSession()
    }

    override fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private data class UserIdEvent(val userId: String?)

    private companion object {
        const val AUTH_INIT_TIMEOUT_MS = 10_000L
    }
}
