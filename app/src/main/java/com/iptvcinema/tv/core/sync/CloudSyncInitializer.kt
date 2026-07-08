package com.iptvcinema.tv.core.sync

import com.iptvcinema.tv.core.supabase.realtime.SupabaseRealtimeCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncInitializer @Inject constructor(
    realtimeCoordinator: SupabaseRealtimeCoordinator,
    cloudDataSyncScheduler: CloudDataSyncScheduler,
) {
    init {
        realtimeCoordinator.start()
        cloudDataSyncScheduler.schedulePeriodicSync()
    }
}
