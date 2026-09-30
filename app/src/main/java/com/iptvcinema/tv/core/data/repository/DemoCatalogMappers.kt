package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries

/** Demo-mode catalog: the fake content mapped to the catalog types the browse screens read. */
internal const val DEMO_SOURCE_ID = "demo"

internal fun MovieItem.toDemoCatalogMovie() = CatalogMovie(
    id = id,
    sourceId = DEMO_SOURCE_ID,
    title = title,
    streamUrl = "",
    posterUrl = imageUrl,
    backdropUrl = backdropUrl,
    categoryId = genres.firstOrNull(),
    categoryName = genres.firstOrNull(),
    year = year,
    durationMinutes = runtimeMinutes,
    rating = rating,
    plot = plot,
    genres = genres,
)

internal fun SeriesItem.toDemoCatalogSeries() = CatalogSeries(
    id = id,
    sourceId = DEMO_SOURCE_ID,
    title = title,
    posterUrl = imageUrl,
    backdropUrl = backdropUrl,
    categoryId = genres.firstOrNull(),
    categoryName = genres.firstOrNull(),
    plot = plot,
    rating = rating,
    year = year,
)

internal fun ChannelItem.toDemoCatalogChannel() = CatalogChannel(
    id = id,
    sourceId = DEMO_SOURCE_ID,
    name = name,
    streamUrl = "",
    logoUrl = logoUrl,
    categoryId = category,
    categoryName = category,
    tvgId = null,
    channelNumber = channelNumber,
)
