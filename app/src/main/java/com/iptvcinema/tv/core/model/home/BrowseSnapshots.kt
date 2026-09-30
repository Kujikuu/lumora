package com.iptvcinema.tv.core.model.home

import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries

/** One catalog category with its size and a sample artwork for its tile. */
data class BrowseCategory(
    val name: String,
    val itemCount: Int,
    val backdropUrl: String? = null,
    val posterUrl: String? = null,
)

/** Catalog-wide content of the Movies tab. Changes only when the source syncs. */
data class MoviesCatalogSnapshot(
    val newest: List<CatalogMovie> = emptyList(),
    val topRated: List<CatalogMovie> = emptyList(),
    val categories: List<BrowseCategory> = emptyList(),
)

/** Movies tab content derived from the profile's watch history. */
data class MoviesPersonalSnapshot(
    val becauseYouWatched: HomeBecauseYouWatched? = null,
    val categoryRails: List<HomeCategoryRail> = emptyList(),
    val watchedMovieIds: Set<String> = emptySet(),
)

/** Catalog-wide content of the Series tab. */
data class SeriesCatalogSnapshot(
    val latest: List<CatalogSeries> = emptyList(),
    val topRated: List<CatalogSeries> = emptyList(),
    val categories: List<BrowseCategory> = emptyList(),
)

data class SeriesBecauseYouWatched(
    val anchorTitle: String,
    val series: List<CatalogSeries>,
)

data class SeriesCategoryRail(
    val categoryName: String,
    val series: List<CatalogSeries>,
)

/** Series tab content derived from the profile's watch history. */
data class SeriesPersonalSnapshot(
    val nextEpisodes: List<HomeNextEpisode> = emptyList(),
    val becauseYouWatched: SeriesBecauseYouWatched? = null,
    val categoryRails: List<SeriesCategoryRail> = emptyList(),
    val watchedSeriesIds: Set<String> = emptySet(),
)
