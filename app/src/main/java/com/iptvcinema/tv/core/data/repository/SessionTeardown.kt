package com.iptvcinema.tv.core.data.repository

import android.util.Log
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.core.data.local.CloudUserDataCache
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.sync.CloudDataSyncScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Signs the user out and removes everything tied to their account from this TV.
 *
 * The server call is best effort: when the TV is offline the local sign-out still
 * completes, so the user is never left half signed out.
 */
@Singleton
class SessionTeardown @Inject constructor(
    private val authRepository: AuthRepository,
    private val appSessionRepository: AppSessionRepository,
    private val cloudUserDataCache: CloudUserDataCache,
    private val playlistSourcesRepository: SupabasePlaylistSourcesRepository,
    private val localCredentialsStore: LocalCredentialsStore,
    private val cloudAccountStatus: CloudAccountStatus,
    private val parentalGate: ParentalGate,
    private val cloudDataSyncScheduler: CloudDataSyncScheduler,
) {
    suspend fun signOut() = withContext(Dispatchers.IO + NonCancellable) {
        if (authRepository.isConfigured()) {
            try {
                authRepository.signOut()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Server sign-out failed; clearing local session anyway: ${error.safeSummary()}")
            }
        }
        appSessionRepository.clearSession()
        runStep("user data cache") { cloudUserDataCache.clearAll() }
        runStep("playlist cache") { playlistSourcesRepository.clearMemoryCache() }
        runStep("sync jobs") { cloudDataSyncScheduler.cancelOneTimeSync() }
        cloudAccountStatus.reset()
        parentalGate.clearSession()
        runStep("playlist credentials") { localCredentialsStore.clearAll() }
    }

    private suspend fun runStep(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.w(TAG, "Sign-out step failed: $name: ${error.safeSummary()}")
        }
    }

    private companion object {
        const val TAG = "SessionTeardown"
    }
}
