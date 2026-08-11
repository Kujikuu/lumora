package com.iptvcinema.tv.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iptvcinema.tv.core.catalog.CatalogSyncCoordinator
import com.iptvcinema.tv.core.catalog.CatalogSyncCoordinatorResult
import com.iptvcinema.tv.core.catalog.CatalogSyncTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CatalogSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val catalogSyncCoordinator: CatalogSyncCoordinator,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER)
            ?.let { runCatching { CatalogSyncTrigger.valueOf(it) }.getOrNull() }
            ?: CatalogSyncTrigger.PERIODIC
        return when (val result = catalogSyncCoordinator.syncCurrent(trigger)) {
            is CatalogSyncCoordinatorResult.Success -> Result.success()
            is CatalogSyncCoordinatorResult.Failed -> if (result.retryable) Result.retry() else Result.success()
        }
    }

    companion object {
        const val KEY_TRIGGER = "catalog_sync_trigger"
    }
}
