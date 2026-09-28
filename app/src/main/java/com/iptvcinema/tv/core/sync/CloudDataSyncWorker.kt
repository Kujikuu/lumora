package com.iptvcinema.tv.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.CloudAccountRetryCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CloudDataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val cloudAccountRetryCoordinator: CloudAccountRetryCoordinator,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!authRepository.isConfigured() || !authRepository.hasActiveSession()) {
            return Result.success()
        }
        val synced = cloudAccountRetryCoordinator.retryCloudSync()
        return when {
            synced -> Result.success()
            runAttemptCount < MAX_RETRIES -> Result.retry()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
