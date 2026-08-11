package com.iptvcinema.tv.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.iptvcinema.tv.core.catalog.CatalogSyncPolicy
import com.iptvcinema.tv.core.catalog.CatalogSyncTrigger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(
            CatalogSyncPolicy.PERIODIC_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setInputData(triggerData(CatalogSyncTrigger.PERIODIC))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueStartupCheck() {
        val request = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
            .setConstraints(constraints)
            .setInputData(triggerData(CatalogSyncTrigger.STARTUP))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            STARTUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun triggerData(trigger: CatalogSyncTrigger): Data = Data.Builder()
        .putString(CatalogSyncWorker.KEY_TRIGGER, trigger.name)
        .build()

    companion object {
        private const val PERIODIC_WORK_NAME = "catalog-sync-periodic"
        private const val STARTUP_WORK_NAME = "catalog-sync-startup"
    }
}
