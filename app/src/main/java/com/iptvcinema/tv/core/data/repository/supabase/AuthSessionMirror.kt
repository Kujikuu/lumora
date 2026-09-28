package com.iptvcinema.tv.core.data.repository.supabase

import android.util.Log
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.core.di.ApplicationScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Keeps the DataStore session in step with Supabase for the whole app lifetime, so a
 * refresh that succeeds after an offline start (or a server-side sign-out) is reflected
 * without a restart. It is the only place outside activation and sign-out that writes
 * the authenticated flag.
 */
@Singleton
class AuthSessionMirror @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: SupabaseAuthRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        if (job != null || !authRepository.isConfigured()) return
        job = supabaseClient.auth.sessionStatus
            .onEach { status -> authRepository.applyToLocal(status) }
            .catch { error -> Log.w(TAG, "Session mirror stopped: ${error.safeSummary()}") }
            .launchIn(applicationScope)
    }

    private companion object {
        const val TAG = "AuthSessionMirror"
    }
}
