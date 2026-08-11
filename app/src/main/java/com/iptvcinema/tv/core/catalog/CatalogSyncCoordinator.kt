package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.epg.EpgSyncRepository
import com.iptvcinema.tv.core.m3u.M3uSyncRepository
import com.iptvcinema.tv.core.m3u.M3uSyncResult
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository
import com.iptvcinema.tv.core.xtream.XtreamSyncResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class CatalogSyncCoordinator @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val localCredentialsStore: LocalCredentialsStore,
    private val catalogDaoFacade: CatalogDaoFacade,
    private val xtreamSyncRepository: XtreamSyncRepository,
    private val m3uSyncRepository: M3uSyncRepository,
    private val epgSyncRepository: EpgSyncRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val inFlightGuard = Mutex()
    private val inFlight = mutableMapOf<String, InFlightSync>()

    suspend fun cancel(sourceId: String) {
        val active = inFlightGuard.withLock { inFlight.remove(sourceId) }
        active?.deferred?.cancelAndJoin()
    }

    suspend fun syncCurrent(trigger: CatalogSyncTrigger): CatalogSyncCoordinatorResult {
        val session = appSessionRepository.sessionState.first()
        val sourceId = session.currentSourceId
            ?: return CatalogSyncCoordinatorResult.Failed(
                "No active catalog source",
                retryable = false,
                reason = CatalogSyncFailureReason.NO_SOURCE,
            )
        val sourceType = session.sourceType
            ?: return CatalogSyncCoordinatorResult.Failed(
                "Catalog source type is missing",
                retryable = false,
                reason = CatalogSyncFailureReason.NO_SOURCE,
            )
        if (session.isDemoMode || sourceType == SourceType.DEMO) {
            return CatalogSyncCoordinatorResult.Failed(
                "Demo catalogs do not require synchronization",
                retryable = false,
                reason = CatalogSyncFailureReason.DEMO_UNAVAILABLE,
            )
        }
        return sync(sourceId, sourceType, trigger)
    }

    suspend fun sync(
        sourceId: String,
        sourceType: SourceType,
        trigger: CatalogSyncTrigger,
    ): CatalogSyncCoordinatorResult {
        val active = inFlightGuard.withLock {
            inFlight[sourceId] ?: applicationScope.async(Dispatchers.IO) {
                performSync(sourceId, sourceType, trigger)
            }.also { created ->
                inFlight[sourceId] = InFlightSync(trigger, created)
                created.invokeOnCompletion {
                    applicationScope.launch {
                        inFlightGuard.withLock {
                            if (inFlight[sourceId]?.deferred === created) inFlight.remove(sourceId)
                        }
                    }
                }
            }.let { InFlightSync(trigger, it) }
        }
        val result = active.deferred.await()
        if (requiresEpgJoin(trigger, active.trigger, result)) {
            epgSyncRepository.awaitPendingSync(sourceId)
        }
        val needsForcedFollowUp = requiresForcedFollowUp(
            requestedTrigger = trigger,
            activeTrigger = active.trigger,
            result = result,
        )
        if (needsForcedFollowUp) {
            inFlightGuard.withLock {
                if (inFlight[sourceId]?.deferred === active.deferred) inFlight.remove(sourceId)
            }
            return sync(sourceId, sourceType, trigger)
        }
        return result
    }

    private suspend fun performSync(
        sourceId: String,
        sourceType: SourceType,
        trigger: CatalogSyncTrigger,
    ): CatalogSyncCoordinatorResult {
        val state = catalogDaoFacade.syncState.get(sourceId)
        if (!trigger.force && !CatalogSyncPolicy.isStale(state?.lastSyncedAtEpochMs, System.currentTimeMillis())) {
            return CatalogSyncCoordinatorResult.Success(
                sourceType = sourceType,
                liveChannelCount = state?.liveChannelCount ?: 0,
                movieCount = state?.movieCount ?: 0,
                seriesCount = state?.seriesCount ?: 0,
                changes = CatalogChangeSummary(),
                checkedProvider = false,
                epgAvailable = state?.epgAvailable ?: false,
            )
        }

        return when (sourceType) {
            SourceType.XTREAM_CODES -> {
                val credentials = localCredentialsStore.getXtreamCredentials(sourceId)
                    ?: return CatalogSyncCoordinatorResult.Failed(
                        "Credentials not found for this source",
                        false,
                        CatalogSyncFailureReason.CREDENTIALS_MISSING,
                    )
                when (val result = xtreamSyncRepository.syncSource(
                    sourceId = sourceId,
                    credentials = credentials,
                    refreshEpgWhenUnchanged = trigger.force,
                    useConditionalRequests = trigger != CatalogSyncTrigger.INITIAL_CONNECTION,
                    awaitEpgCompletion = trigger.awaitEpgCompletion,
                )) {
                    is XtreamSyncResult.Success -> CatalogSyncCoordinatorResult.Success(
                        sourceType,
                        result.liveChannelCount,
                        result.movieCount,
                        result.seriesCount,
                        result.changes,
                        result.checkedProvider,
                    )
                    is XtreamSyncResult.AuthFailed -> CatalogSyncCoordinatorResult.Failed(
                        result.message,
                        retryable = result.retryable,
                        reason = if (result.status == com.iptvcinema.tv.core.model.SourceStatus.NEEDS_ATTENTION) {
                            CatalogSyncFailureReason.NETWORK
                        } else {
                            CatalogSyncFailureReason.AUTHENTICATION
                        },
                    )
                    is XtreamSyncResult.Failed -> CatalogSyncCoordinatorResult.Failed(
                        result.message,
                        result.retryable,
                        if (result.retryable) CatalogSyncFailureReason.NETWORK else CatalogSyncFailureReason.PROVIDER,
                    )
                }
            }
            SourceType.M3U -> {
                val credentials = localCredentialsStore.getM3uCredentials(sourceId)
                    ?: return CatalogSyncCoordinatorResult.Failed(
                        "Credentials not found for this source",
                        false,
                        CatalogSyncFailureReason.CREDENTIALS_MISSING,
                    )
                when (val result = m3uSyncRepository.syncSource(
                    sourceId = sourceId,
                    credentials = credentials,
                    refreshEpgWhenUnchanged = trigger.force,
                    useConditionalRequests = trigger != CatalogSyncTrigger.INITIAL_CONNECTION,
                    awaitEpgCompletion = trigger.awaitEpgCompletion,
                )) {
                    is M3uSyncResult.Success -> CatalogSyncCoordinatorResult.Success(
                        sourceType,
                        result.liveChannelCount,
                        movieCount = 0,
                        seriesCount = 0,
                        changes = result.changes,
                        checkedProvider = result.checkedProvider,
                        epgAvailable = result.epgAvailable,
                    )
                    is M3uSyncResult.Unreachable -> CatalogSyncCoordinatorResult.Failed(
                        result.message,
                        true,
                        CatalogSyncFailureReason.NETWORK,
                    )
                    is M3uSyncResult.Failed -> CatalogSyncCoordinatorResult.Failed(
                        result.message,
                        result.retryable,
                        if (result.retryable) CatalogSyncFailureReason.NETWORK else CatalogSyncFailureReason.PROVIDER,
                    )
                }
            }
            SourceType.DEMO -> CatalogSyncCoordinatorResult.Failed(
                "Demo catalogs do not require synchronization",
                false,
                CatalogSyncFailureReason.DEMO_UNAVAILABLE,
            )
        }
    }

    private data class InFlightSync(
        val trigger: CatalogSyncTrigger,
        val deferred: Deferred<CatalogSyncCoordinatorResult>,
    )
}
