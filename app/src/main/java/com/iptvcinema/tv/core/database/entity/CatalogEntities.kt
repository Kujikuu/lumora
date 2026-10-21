package com.iptvcinema.tv.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

// The primary keys start with id, so every "WHERE sourceId = ?" query needs its own index.
// Each index below matches a real query shape (see CatalogDaos); keep them few, since every
// index slows the bulk inserts of a catalog sync. DatabaseModule.CATALOG_INDEXES_V10 creates the
// same indexes for upgraded installs; change both together.

@Entity(
    tableName = "categories",
    primaryKeys = ["id", "sourceId"],
    indices = [Index(value = ["sourceId", "contentType", "sortOrder", "name"], name = "idx_categories_type_order")],
)
data class LocalCategoryEntity(
    val id: String,
    val sourceId: String,
    val name: String,
    val contentType: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "channels",
    primaryKeys = ["id", "sourceId"],
    indices = [
        Index(value = ["sourceId", "sortOrder"], name = "idx_channels_order"),
        Index(value = ["sourceId", "categoryId", "sortOrder"], name = "idx_channels_category"),
    ],
)
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
    val archiveDays: Int = 0,
)

@Entity(
    tableName = "movies",
    primaryKeys = ["id", "sourceId"],
    indices = [
        Index(value = ["sourceId", "sortOrder"], name = "idx_movies_order"),
        Index(value = ["sourceId", "categoryId", "sortOrder"], name = "idx_movies_category"),
        Index(value = ["sourceId", "categoryName", "addedAt", "sortOrder"], name = "idx_movies_category_name"),
        Index(value = ["sourceId", "addedAt", "sortOrder"], name = "idx_movies_added"),
        Index(value = ["sourceId", "ratingValue", "year"], name = "idx_movies_rating"),
        Index(value = ["sourceId", "hasHeroArt", "addedAt", "sortOrder"], name = "idx_movies_hero"),
    ],
)
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
    val youtubeTrailer: String? = null,
    val sortOrder: Int = 0,
    val addedAt: Long? = null,
    // Derived from rating so top-rated rails can use an index. Keep it last and never pass it
    // by hand; a copy(rating = ...) must also recompute it.
    val ratingValue: Double? = RatingValue.of(rating),
    // Derived from backdropUrl and plot so the Home hero query can use an index. Same rules as
    // ratingValue: never pass it by hand, and recompute it on a copy that changes either field.
    @ColumnInfo(defaultValue = "0")
    val hasHeroArt: Boolean = HeroArt.of(backdropUrl, plot),
)

@Entity(
    tableName = "series",
    primaryKeys = ["id", "sourceId"],
    indices = [
        Index(value = ["sourceId", "sortOrder"], name = "idx_series_order"),
        Index(value = ["sourceId", "categoryId", "sortOrder"], name = "idx_series_category"),
        Index(value = ["sourceId", "categoryName", "year", "sortOrder"], name = "idx_series_category_name"),
        Index(value = ["sourceId", "year", "sortOrder"], name = "idx_series_year"),
        Index(value = ["sourceId", "ratingValue", "year"], name = "idx_series_rating"),
    ],
)
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
    val youtubeTrailer: String? = null,
    val sortOrder: Int = 0,
    // Derived from rating so top-rated rails can use an index (see LocalMovieEntity).
    val ratingValue: Double? = RatingValue.of(rating),
)

@Entity(
    tableName = "episodes",
    primaryKeys = ["id", "sourceId"],
    indices = [
        Index(value = ["sourceId", "seriesId", "seasonNumber", "episodeNumber"], name = "idx_episodes_series"),
    ],
)
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

@Entity(tableName = "catalog_sync_metadata", primaryKeys = ["sourceId", "resourceKey"])
data class CatalogSyncMetadataEntity(
    val sourceId: String,
    val resourceKey: String,
    val etag: String? = null,
    val lastModified: String? = null,
    val contentFingerprint: String? = null,
    val lastCheckedAtEpochMs: Long,
)
