package com.iptvcinema.tv.core.model.home

import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries

/** Catalog-wide Home content. Changes only when the source syncs. */
data class HomeCatalogSnapshot(
    val heroCandidates: List<CatalogMovie> = emptyList(),
    val newestMovies: List<CatalogMovie> = emptyList(),
)

/** Home content derived from the profile's watch history and saved channels. */
data class HomePersonalSnapshot(
    val nextEpisodes: List<HomeNextEpisode> = emptyList(),
    val becauseYouWatched: HomeBecauseYouWatched? = null,
    val categoryRails: List<HomeCategoryRail> = emptyList(),
    /** Shortcut tiles, most relevant first: the viewer's categories, then the largest ones. */
    val categoryTiles: List<BrowseCategory> = emptyList(),
    /** Recently watched channels, then favorite ones. */
    val liveChannels: List<CatalogChannel> = emptyList(),
    /** What each of [liveChannels] is showing now, by channel id, when the guide knows. */
    val nowPlaying: Map<String, String> = emptyMap(),
    val watchedMovieIds: Set<String> = emptySet(),
)

data class HomeNextEpisode(
    val series: CatalogSeries?,
    val episode: CatalogEpisode,
)

data class HomeBecauseYouWatched(
    val anchorTitle: String,
    val movies: List<CatalogMovie>,
)

data class HomeCategoryRail(
    val categoryName: String,
    val movies: List<CatalogMovie>,
)
