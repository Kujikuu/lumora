package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.m3u.M3uSyncRepository
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class CatalogSyncProgressDisplay(
    val fraction: Float,
    val stepLabel: String,
)

@Singleton
class CatalogSyncProgressTracker @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val xtreamSyncRepository: XtreamSyncRepository,
    private val m3uSyncRepository: M3uSyncRepository,
    private val appStrings: AppStrings,
) {
    fun observeProgress(): Flow<CatalogSyncProgressDisplay> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            when (session.sourceType) {
                SourceType.XTREAM_CODES -> xtreamSyncRepository.progress.map { steps ->
                    toDisplay(CatalogSyncProgressMapper.mapXtream(steps))
                }
                SourceType.M3U -> m3uSyncRepository.progress.map { steps ->
                    toDisplay(CatalogSyncProgressMapper.mapM3u(steps))
                }
                else -> flowOf(CatalogSyncProgressDisplay(0f, appStrings.get(CatalogSyncProgressMapper.milestoneStringRes(SyncMilestone.CONNECTING))))
            }
        }

    fun initialProgress(sourceType: SourceType?): CatalogSyncProgressDisplay {
        val milestone = when (sourceType) {
            SourceType.M3U -> SyncMilestone.M3U_VALID
            else -> SyncMilestone.CONNECTING
        }
        return CatalogSyncProgressDisplay(
            fraction = 0f,
            stepLabel = appStrings.get(CatalogSyncProgressMapper.milestoneStringRes(milestone)),
        )
    }

    private fun toDisplay(progress: CatalogSyncProgress): CatalogSyncProgressDisplay =
        CatalogSyncProgressDisplay(
            fraction = progress.fraction,
            stepLabel = appStrings.get(CatalogSyncProgressMapper.milestoneStringRes(progress.milestone)),
        )
}
