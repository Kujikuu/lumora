package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import java.time.Instant

internal fun movie(
    id: String,
    rating: String? = null,
    category: String? = null,
    backdrop: String? = "https://img/$id-bg.jpg",
) = CatalogMovie(
    id = id,
    sourceId = "src",
    title = "Movie $id",
    streamUrl = "",
    posterUrl = "https://img/$id.jpg",
    backdropUrl = backdrop,
    categoryId = category,
    categoryName = category,
    year = 2024,
    durationMinutes = 100,
    rating = rating,
    plot = "Plot $id",
    genres = emptyList(),
)

internal fun series(id: String, rating: String? = null, category: String? = null) = CatalogSeries(
    id = id,
    sourceId = "src",
    title = "Series $id",
    posterUrl = "https://img/$id.jpg",
    backdropUrl = null,
    categoryId = category,
    categoryName = category,
    plot = null,
    rating = rating,
    year = 2023,
)

internal fun episode(id: String, seriesId: String, season: Int = 1, number: Int = 1) = CatalogEpisode(
    id = id,
    sourceId = "src",
    seriesId = seriesId,
    seasonNumber = season,
    episodeNumber = number,
    title = "Episode $number",
    streamUrl = "",
    durationMinutes = 40,
    plot = null,
    thumbnailUrl = null,
)

internal fun channel(id: String, category: String? = null) = CatalogChannel(
    id = id,
    sourceId = "src",
    name = "Channel $id",
    streamUrl = "",
    logoUrl = null,
    categoryId = category,
    categoryName = category,
    tvgId = null,
    channelNumber = null,
)

internal fun history(
    contentId: String,
    type: WatchHistoryContentType,
    watchedAtSeconds: Long,
    positionMs: Long = 50_000,
    durationMs: Long? = 100_000,
    seriesId: String? = null,
) = WatchHistoryItem(
    id = "h-$contentId",
    profileId = "p",
    sourceId = "src",
    contentId = contentId,
    contentType = type,
    seriesId = seriesId,
    title = "Title $contentId",
    posterUrl = null,
    positionMs = positionMs,
    durationMs = durationMs,
    lastWatchedAt = Instant.ofEpochSecond(watchedAtSeconds),
)

internal fun movies(vararg ids: String) = ids.map { movie(it) }
