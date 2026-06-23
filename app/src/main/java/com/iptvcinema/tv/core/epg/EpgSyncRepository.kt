package com.iptvcinema.tv.core.epg

import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    suspend fun syncEpg(
        sourceId: String,
        channels: List<LocalChannelEntity>,
        fetchXml: suspend () -> String,
    ): EpgSyncOutcome = withContext(Dispatchers.IO) {
        runCatching {
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
        }.getOrElse { error ->
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
    ): Job = applicationScope.launch(Dispatchers.IO) {
        val outcome = syncEpg(sourceId, channels, fetchXml)
        updateEpgAvailable(sourceId, outcome.epgAvailable)
        onComplete(outcome)
    }

    private suspend fun updateEpgAvailable(sourceId: String, epgAvailable: Boolean) {
        val existing = catalogDaoFacade.syncState.get(sourceId) ?: return
        if (existing.epgAvailable != epgAvailable) {
            catalogDaoFacade.syncState.upsert(existing.copy(epgAvailable = epgAvailable))
        }
    }

    companion object {
        const val TRIM_PAST_MS = 24L * 60 * 60 * 1000
    }
}
