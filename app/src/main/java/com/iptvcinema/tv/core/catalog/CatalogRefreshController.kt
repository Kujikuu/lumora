package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.util.AppStrings
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CatalogRefreshResult {
    val message: String

    data class Success(override val message: String) : CatalogRefreshResult
    data class Failed(override val message: String) : CatalogRefreshResult
}

sealed interface CatalogRefreshState {
    data object Idle : CatalogRefreshState
    data class Refreshing(val progress: Float = 0f, val stepLabel: String = "") : CatalogRefreshState
    data class Success(val message: String) : CatalogRefreshState
    data class Failed(val message: String) : CatalogRefreshState
}

@Singleton
class CatalogRefreshController @Inject constructor(
    private val catalogSyncCoordinator: CatalogSyncCoordinator,
    private val appStrings: AppStrings,
    private val messageFormatter: CatalogSyncMessageFormatter,
) {
    suspend fun refreshCurrentSource(): CatalogRefreshResult = runCatching {
        when (val result = catalogSyncCoordinator.syncCurrent(CatalogSyncTrigger.MANUAL)) {
            is CatalogSyncCoordinatorResult.Success -> CatalogRefreshResult.Success(successMessage(result))
            is CatalogSyncCoordinatorResult.Failed -> CatalogRefreshResult.Failed(messageFormatter.failure(result))
        }
    }.getOrElse {
        CatalogRefreshResult.Failed(appStrings.get(R.string.refresh_failed))
    }

    private fun successMessage(result: CatalogSyncCoordinatorResult.Success): String {
        val detail = if (result.changes.hasChanges) {
            appStrings.get(
                R.string.refresh_catalog_changes,
                result.changes.added,
                result.changes.updated,
                result.changes.removed,
            )
        } else {
            appStrings.get(R.string.refresh_catalog_up_to_date)
        }
        return appStrings.get(R.string.refresh_success_title, detail)
    }
}
