package com.iptvcinema.tv.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "categories", primaryKeys = ["id", "sourceId"])
data class LocalCategoryEntity(
    val id: String,
    val sourceId: String,
    val name: String,
    val contentType: String,
    val sortOrder: Int = 0,
)

@Entity(tableName = "channels", primaryKeys = ["id", "sourceId"])
data class LocalChannelEntity(
    val id: String,
    val sourceId: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val tvgId: String?,
    val channelNumber: Int?,
    val isAdult: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "movies", primaryKeys = ["id", "sourceId"])
data class LocalMovieEntity(
    val id: String,
    val sourceId: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val year: Int?,
    val durationMinutes: Int?,
    val rating: String?,
    val plot: String?,
    val genres: String?,
    val cast: String? = null,
    val sortOrder: Int = 0,
    val addedAt: Long? = null,
)

@Entity(tableName = "series", primaryKeys = ["id", "sourceId"])
data class LocalSeriesEntity(
    val id: String,
    val sourceId: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val plot: String?,
    val rating: String?,
    val year: Int?,
    val cast: String? = null,
    val sortOrder: Int = 0,
)

@Entity(tableName = "episodes", primaryKeys = ["id", "sourceId"])
data class LocalEpisodeEntity(
    val id: String,
    val sourceId: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val durationMinutes: Int?,
    val plot: String?,
    val thumbnailUrl: String?,
)

@Entity(
    tableName = "programs",
    primaryKeys = ["id", "sourceId"],
    indices = [Index(value = ["sourceId", "channelId", "startEpochMs"], name = "idx_programs_window")],
)
data class LocalProgramEntity(
    val id: String,
    val sourceId: String,
    val channelId: String,
    val title: String,
    val description: String?,
    val startEpochMs: Long,
    val endEpochMs: Long,
)

@Entity(tableName = "source_sync_state", primaryKeys = ["sourceId"])
data class LocalSourceSyncStateEntity(
    val sourceId: String,
    val lastSyncedAtEpochMs: Long?,
    val liveChannelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val epgAvailable: Boolean = false,
    val lastError: String? = null,
)
