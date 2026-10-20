package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Status and type of [sourceId], local copy first. [loadCached] reads the last saved sources
 * list (no network), so screens never wait on auth or the network to show, say, an expired
 * source. [fetchRemote] returns the server list, or null when it cannot be reached; then the
 * cached value stays, and without one the metadata is unknown, as before.
 */
internal fun sourceMetaFlow(
    sourceId: String,
    loadCached: suspend () -> PlaylistSourceRecord?,
    fetchRemote: suspend () -> List<PlaylistSourceRecord>?,
): Flow<Pair<SourceStatus?, SourceType?>> = flow {
    val cached = loadCached()
    cached?.let { emit(it.status to it.type) }
    val remote = fetchRemote()
    when {
        remote != null -> {
            val source = remote.find { it.id == sourceId }
            emit(source?.status to source?.type)
        }
        cached == null -> emit(null to null)
    }
}
