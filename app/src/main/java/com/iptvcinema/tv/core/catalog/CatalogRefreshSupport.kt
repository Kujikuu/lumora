package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.util.SyncStatusFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
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
    ) = scope.launch {
        if (getRefreshState() == CatalogRefreshState.Refreshing) return@launch
        setRefreshState(CatalogRefreshState.Refreshing)
        val result = catalogRefreshController.refreshCurrentSource()
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
