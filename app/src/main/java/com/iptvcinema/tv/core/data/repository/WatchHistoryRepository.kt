package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun observeHistory(profileId: String, limit: Int = 20): Flow<List<WatchHistoryItem>>
    fun observeContinueWatching(profileId: String, limit: Int = 10): Flow<List<WatchHistoryItem>>
    suspend fun getProgress(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
    ): WatchHistoryItem?
    suspend fun upsertProgress(
        profileId: String,
        contentId: String,
        contentType: WatchHistoryContentType,
        title: String,
        posterUrl: String?,
        positionMs: Long,
        durationMs: Long?,
        sourceId: String? = null,
        seriesId: String? = null,
    )
    suspend fun getDistinctSeriesIds(profileId: String): List<String>
    suspend fun getEpisodeHistoryForSeries(
        profileId: String,
        sourceId: String,
        seriesId: String,
    ): List<WatchHistoryItem>
    suspend fun remove(profileId: String, contentId: String, contentType: WatchHistoryContentType)
    fun invalidate()
}

internal fun filterEpisodeHistoryForSeries(
    items: List<WatchHistoryItem>,
    sourceId: String,
    seriesId: String,
): List<WatchHistoryItem> = items
    .asSequence()
    .filter { it.contentType == WatchHistoryContentType.EPISODE }
    .filter { it.sourceId == sourceId && it.seriesId == seriesId }
    .sortedByDescending { it.lastWatchedAt }
    .toList()
