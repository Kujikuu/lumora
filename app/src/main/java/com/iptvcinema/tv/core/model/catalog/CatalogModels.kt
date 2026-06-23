package com.iptvcinema.tv.core.model.catalog

enum class CatalogContentType {
    LIVE,
    VOD,
    SERIES,
}

data class CatalogCategory(
    val id: String,
    val sourceId: String,
    val name: String,
    val contentType: CatalogContentType,
    val sortOrder: Int = 0,
)

data class CatalogChannel(
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

data class CatalogMovie(
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
    val genres: List<String>,
    val cast: String? = null,
    val sortOrder: Int = 0,
    val addedAt: Long? = null,
)

data class CatalogSeries(
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

data class CatalogEpisode(
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

data class CatalogProgram(
    val id: String,
    val sourceId: String,
    val channelId: String,
    val title: String,
    val description: String?,
    val startEpochMs: Long,
    val endEpochMs: Long,
)

data class CatalogSyncState(
    val sourceId: String,
    val lastSyncedAtEpochMs: Long?,
    val liveChannelCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val epgAvailable: Boolean,
    val lastError: String?,
)

data class FeaturedCatalogContent(
    val heroMovies: List<CatalogMovie>,
    val continueWatchingMovies: List<CatalogMovie>,
    val trendingMovies: List<CatalogMovie>,
    val liveChannels: List<CatalogChannel>,
    val newReleaseMovies: List<CatalogMovie>,
    val featuredSeries: List<CatalogSeries>,
)
