package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.XtreamCredentials
import java.time.Instant

interface PlaylistSourcesRepository {
    suspend fun getSources(): List<PlaylistSourceRecord>
    suspend fun saveXtreamSource(credentials: XtreamCredentials): PlaylistSourceRecord
    suspend fun findMatchingXtreamSource(credentials: XtreamCredentials): PlaylistSourceRecord?
    suspend fun activateXtreamSource(sourceId: String, credentials: XtreamCredentials): PlaylistSourceRecord
    suspend fun saveM3uSource(credentials: M3uCredentials): PlaylistSourceRecord
    suspend fun saveDemoSource(): PlaylistSourceRecord
    suspend fun setActiveSource(sourceId: String)
    suspend fun deleteSource(sourceId: String)
    suspend fun updateSyncStatus(sourceId: String, status: SourceStatus, lastSyncedAt: Instant? = null)
    suspend fun ensureLocalCredentials(source: PlaylistSourceRecord)
}
