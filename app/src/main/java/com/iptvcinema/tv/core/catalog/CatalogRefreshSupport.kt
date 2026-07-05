package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.util.SyncStatusFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object CatalogRefreshSupport {
    const val MESSAGE_VISIBLE_MS = 5_000L

    fun observeSyncBanner(
        scope: CoroutineScope,
        catalogRepository: CatalogRepository,
        onBanner: (String?) -> Unit,
    ) = scope.launch {
        combine(
            catalogRepository.observeSyncState(),
            catalogRepository.observeSourceMeta(),
        ) { syncState, (status, _) ->
            SyncStatusFormatter.formatBanner(
                statusSyncing = status == SourceStatus.SYNCING,
                lastSyncedAtEpochMs = syncState?.lastSyncedAtEpochMs,
            )
        }.collect(onBanner)
    }

    fun runCatalogRefresh(
        scope: CoroutineScope,
        getRefreshState: () -> CatalogRefreshState,
        setRefreshState: (CatalogRefreshState) -> Unit,
        catalogRefreshController: CatalogRefreshController,
        catalogSyncProgressTracker: CatalogSyncProgressTracker,
        appSessionRepository: AppSessionRepository,
    ) = scope.launch {
        if (getRefreshState() is CatalogRefreshState.Refreshing) return@launch
        val sourceType = appSessionRepository.sessionState.first().sourceType
        val initial = catalogSyncProgressTracker.initialProgress(sourceType)
        setRefreshState(
            CatalogRefreshState.Refreshing(
                progress = initial.fraction,
                stepLabel = initial.stepLabel,
            ),
        )
        var progressJob: Job? = null
        progressJob = scope.launch {
            catalogSyncProgressTracker.observeProgress().collect { display ->
                if (getRefreshState() is CatalogRefreshState.Refreshing) {
                    setRefreshState(
                        CatalogRefreshState.Refreshing(
                            progress = display.fraction,
                            stepLabel = display.stepLabel,
                        ),
                    )
                }
            }
        }
        val result = catalogRefreshController.refreshCurrentSource()
        progressJob.cancel()
        setRefreshState(
            when (result) {
                is CatalogRefreshResult.Success -> CatalogRefreshState.Success(result.message)
                is CatalogRefreshResult.Failed -> CatalogRefreshState.Failed(result.message)
            },
        )
        delay(MESSAGE_VISIBLE_MS)
        if (getRefreshState() !is CatalogRefreshState.Refreshing) {
            setRefreshState(CatalogRefreshState.Idle)
        }
    }
}
