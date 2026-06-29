package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.home.HomeCardAction
import com.iptvcinema.tv.core.model.home.HomeContentCard

object HomeUiMapper {
    fun CatalogMovie.toHomeContentCard(
        isFavorite: Boolean = false,
        showTop10Badge: Boolean = false,
        primaryAction: HomeCardAction = HomeCardAction.WatchNow,
    ): HomeContentCard = HomeContentCard(
        contentId = id,
        contentType = "movie",
        title = title,
        imageUrl = posterUrl,
        backdropUrl = backdropUrl ?: posterUrl,
        year = year?.takeIf { it > 0 }?.toString(),
        genres = genres,
        plot = plot?.takeIf { it.isNotBlank() },
        runtimeOrEpisodes = durationMinutes?.takeIf { it > 0 }?.let { "${it}m" },
        highlightText = plot?.takeIf { it.isNotBlank() },
        isFavorite = isFavorite,
        showTop10Badge = showTop10Badge,
        primaryAction = primaryAction,
    )

    fun CatalogSeries.toHomeContentCard(
        isFavorite: Boolean = false,
        showTop10Badge: Boolean = false,
        primaryAction: HomeCardAction = HomeCardAction.WatchNow,
    ): HomeContentCard = HomeContentCard(
        contentId = id,
        contentType = "series",
        title = title,
        imageUrl = posterUrl,
        backdropUrl = backdropUrl ?: posterUrl,
        year = year?.takeIf { it > 0 }?.toString(),
        genres = listOfNotNull(categoryName?.takeIf { it.isNotBlank() }),
        plot = plot?.takeIf { it.isNotBlank() },
        runtimeOrEpisodes = categoryName?.takeIf { it.isNotBlank() },
        highlightText = plot?.takeIf { it.isNotBlank() },
        isFavorite = isFavorite,
        showTop10Badge = showTop10Badge,
        primaryAction = primaryAction,
    )
}
