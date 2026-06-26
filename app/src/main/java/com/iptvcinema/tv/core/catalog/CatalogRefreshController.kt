package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.m3u.M3uSyncRepository
import com.iptvcinema.tv.core.m3u.M3uSyncResult
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository
import com.iptvcinema.tv.core.xtream.XtreamSyncResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

sealed interface CatalogRefreshResult {
    val message: String

    data class Success(override val message: String) : CatalogRefreshResult
    data class Failed(override val message: String) : CatalogRefreshResult
}

sealed interface CatalogRefreshState {
    data object Idle : CatalogRefreshState
    data object Refreshing : CatalogRefreshState
    data class Success(val message: String) : CatalogRefreshState
    data class Failed(val message: String) : CatalogRefreshState
}

@Singleton
class CatalogRefreshController @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val localCredentialsStore: LocalCredentialsStore,
    private val xtreamSyncRepository: XtreamSyncRepository,
    private val m3uSyncRepository: M3uSyncRepository,
    private val appStrings: AppStrings,
) {
    suspend fun refreshCurrentSource(): CatalogRefreshResult = runCatching {
        val session = appSessionRepository.sessionState.first()
        if (session.isDemoMode || session.sourceType == SourceType.DEMO) {
            return@runCatching CatalogRefreshResult.Failed(appStrings.get(R.string.refresh_demo_unavailable))
        }
        val sourceId = session.currentSourceId
            ?: return@runCatching CatalogRefreshResult.Failed(appStrings.get(R.string.refresh_failed))
        when (session.sourceType) {
            SourceType.XTREAM_CODES -> refreshXtreamSource(sourceId)
            SourceType.M3U -> refreshM3uSource(sourceId)
            SourceType.DEMO, null -> CatalogRefreshResult.Failed(appStrings.get(R.string.refresh_failed))
        }
    }.getOrElse {
        CatalogRefreshResult.Failed(appStrings.get(R.string.refresh_failed))
    }

    private suspend fun refreshXtreamSource(sourceId: String): CatalogRefreshResult {
        val credentials = localCredentialsStore.getXtreamCredentials(sourceId)
            ?: return CatalogRefreshResult.Failed(appStrings.get(R.string.refresh_credentials_missing))
        return when (val result = xtreamSyncRepository.syncSource(sourceId, credentials)) {
            is XtreamSyncResult.Success -> CatalogRefreshResult.Success(
                appStrings.get(
                    R.string.refresh_success_xtream,
                    result.liveChannelCount,
                    result.movieCount,
                    result.seriesCount,
                ),
            )
            is XtreamSyncResult.AuthFailed -> CatalogRefreshResult.Failed(result.message)
            is XtreamSyncResult.Failed -> CatalogRefreshResult.Failed(result.message)
        }
    }

    private suspend fun refreshM3uSource(sourceId: String): CatalogRefreshResult {
        val credentials = localCredentialsStore.getM3uCredentials(sourceId)
            ?: return CatalogRefreshResult.Failed(appStrings.get(R.string.refresh_credentials_missing))
        return when (val result = m3uSyncRepository.syncSource(sourceId, credentials)) {
            is M3uSyncResult.Success -> CatalogRefreshResult.Success(
                appStrings.get(
                    R.string.refresh_success_m3u,
                    result.liveChannelCount,
                ),
            )
            is M3uSyncResult.Unreachable -> CatalogRefreshResult.Failed(result.message)
            is M3uSyncResult.Failed -> CatalogRefreshResult.Failed(result.message)
        }
    }
}
