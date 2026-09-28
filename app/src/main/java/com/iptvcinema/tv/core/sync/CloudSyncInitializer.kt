package com.iptvcinema.tv.core.sync

import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.supabase.AuthSessionMirror
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.supabase.realtime.SupabaseRealtimeCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Singleton
class CloudSyncInitializer @Inject constructor(
    authSessionMirror: AuthSessionMirror,
    authRepository: AuthRepository,
    realtimeCoordinator: SupabaseRealtimeCoordinator,
    cloudDataSyncScheduler: CloudDataSyncScheduler,
    catalogSyncScheduler: CatalogSyncScheduler,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    init {
        authSessionMirror.start()
        realtimeCoordinator.start()
        cloudDataSyncScheduler.schedulePeriodicSync()
        catalogSyncScheduler.schedulePeriodicSync()
        if (authRepository.isConfigured()) {
            // Pull the account's data as soon as someone signs in (and once per app start).
            authRepository.currentUserId
                .filterNotNull()
                .onEach { cloudDataSyncScheduler.scheduleOneTimeSync() }
                .launchIn(applicationScope)
        }
    }
}
