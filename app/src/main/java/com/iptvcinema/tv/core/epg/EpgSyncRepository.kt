package com.iptvcinema.tv.core.epg

import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext

data class EpgSyncOutcome(
    val programCount: Int,
    val epgAvailable: Boolean,
    val message: String,
    val isSuccess: Boolean,
)

@Singleton
class EpgSyncRepository @Inject constructor(
    private val catalogDaoFacade: CatalogDaoFacade,
    private val xmltvParser: XmltvParser,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val backgroundJobs = LatestPerSourceJobRunner(applicationScope)

    suspend fun syncEpg(
        sourceId: String,
        channels: List<LocalChannelEntity>,
        fetchXml: suspend () -> String,
    ): EpgSyncOutcome = withContext(Dispatchers.IO) {
        try {
            val xml = fetchXml()
            val nowMs = System.currentTimeMillis()
            val programs = withContext(Dispatchers.Default) {
                xmltvParser.parse(sourceId, xml, channels, nowMs)
            }
            catalogDaoFacade.replacePrograms(sourceId, programs)
            EpgSyncOutcome(
                programCount = programs.size,
                epgAvailable = programs.isNotEmpty(),
                message = if (programs.isEmpty()) "No EPG data" else "${programs.size} programs",
                isSuccess = true,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            EpgSyncOutcome(
                programCount = 0,
                epgAvailable = false,
                message = error.message ?: "EPG unavailable",
                isSuccess = false,
            )
        }
    }

    fun syncEpgInBackground(
        sourceId: String,
        channels: List<LocalChannelEntity>,
        fetchXml: suspend () -> String,
        onComplete: suspend (EpgSyncOutcome) -> Unit = {},
    ): Job = backgroundJobs.launch(sourceId) {
        syncEpgAndRecord(sourceId, channels, fetchXml, onComplete)
    }

    suspend fun syncEpgAndWait(
        sourceId: String,
        channels: List<LocalChannelEntity>,
        fetchXml: suspend () -> String,
        onComplete: suspend (EpgSyncOutcome) -> Unit = {},
    ): EpgSyncOutcome {
        val completion = CompletableDeferred<EpgSyncOutcome>()
        val job = backgroundJobs.launch(sourceId) {
            try {
                completion.complete(syncEpgAndRecord(sourceId, channels, fetchXml, onComplete))
            } catch (error: CancellationException) {
                completion.cancel(error)
                throw error
            } catch (error: Throwable) {
                completion.completeExceptionally(error)
            }
        }
        return try {
            completion.await()
        } catch (error: CancellationException) {
            job.cancelAndJoin()
            throw error
        }
    }

    suspend fun cancelSync(sourceId: String) {
        backgroundJobs.cancelAndJoin(sourceId)
    }

    suspend fun awaitPendingSync(sourceId: String) {
        backgroundJobs.awaitIdle(sourceId)
    }

    private suspend fun updateEpgAvailable(sourceId: String, epgAvailable: Boolean) {
        catalogDaoFacade.syncState.updateEpgAvailable(sourceId, epgAvailable)
    }

    private suspend fun syncEpgAndRecord(
        sourceId: String,
        channels: List<LocalChannelEntity>,
        fetchXml: suspend () -> String,
        onComplete: suspend (EpgSyncOutcome) -> Unit,
    ): EpgSyncOutcome {
        var outcome = syncEpg(sourceId, channels, fetchXml)
        try {
            updateEpgAvailable(sourceId, outcome.epgAvailable)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            outcome = EpgSyncOutcome(
                programCount = outcome.programCount,
                epgAvailable = outcome.epgAvailable,
                message = error.message ?: "Unable to save EPG status",
                isSuccess = false,
            )
        }
        try {
            onComplete(outcome)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Progress reporting must never fail an otherwise successful catalog sync.
        }
        return outcome
    }

    companion object {
        const val TRIM_PAST_MS = 24L * 60 * 60 * 1000
    }
}
