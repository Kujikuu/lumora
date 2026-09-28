package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.home.HomeCardAction
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.HomeNextEpisode

object HomeUiMapper {
    fun CatalogMovie.toHomeContentCard(
        isFavorite: Boolean = false,
        rank: Int? = null,
        primaryAction: HomeCardAction = HomeCardAction.WatchNow,
    ): HomeContentCard = HomeContentCard(
        contentId = id,
        contentType = "movie",
        title = title,
        imageUrl = posterUrl,
        backdropUrl = backdropUrl ?: posterUrl,
        year = year?.takeIf { it > 0 }?.toString(),
        genres = genres.ifEmpty { listOfNotNull(categoryName?.takeIf { it.isNotBlank() }) },
        plot = plot?.takeIf { it.isNotBlank() },
        runtimeOrEpisodes = durationMinutes?.takeIf { it > 0 }?.let { "${it}m" },
        highlightText = plot?.takeIf { it.isNotBlank() },
        isFavorite = isFavorite,
        rank = rank,
        rating = rating,
        primaryAction = primaryAction,
    )

    fun CatalogSeries.toHomeContentCard(
        isFavorite: Boolean = false,
        rank: Int? = null,
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
        rank = rank,
        rating = rating,
        primaryAction = primaryAction,
    )

    fun HomeNextEpisode.toHomeContentCard(isFavorite: Boolean = false): HomeContentCard {
        val seriesBackdrop = series?.backdropUrl?.takeIf { it.isNotBlank() }
        val seriesPoster = series?.posterUrl?.takeIf { it.isNotBlank() }
        val thumbnail = episode.thumbnailUrl?.takeIf { it.isNotBlank() }
        return HomeContentCard(
            contentId = episode.id,
            contentType = "episode",
            seriesId = episode.seriesId,
            title = series?.title?.takeIf { it.isNotBlank() } ?: episode.title,
            subtitle = episodeSubtitle(episode.seasonNumber, episode.episodeNumber, episode.title),
            imageUrl = thumbnail ?: seriesBackdrop ?: seriesPoster,
            backdropUrl = seriesBackdrop ?: thumbnail ?: seriesPoster,
            plot = episode.plot?.takeIf { it.isNotBlank() } ?: series?.plot?.takeIf { it.isNotBlank() },
            rating = series?.rating,
            isFavorite = isFavorite,
        )
    }

    fun CatalogChannel.toHomeContentCard(isFavorite: Boolean = false): HomeContentCard = HomeContentCard(
        contentId = id,
        contentType = "channel",
        title = name,
        subtitle = categoryName?.takeIf { it.isNotBlank() },
        imageUrl = logoUrl,
        isFavorite = isFavorite,
    )

    fun episodeSubtitle(seasonNumber: Int, episodeNumber: Int, title: String): String {
        val prefix = "S${seasonNumber}E$episodeNumber".takeIf { episodeNumber > 0 }
        return listOfNotNull(prefix, title.takeIf { it.isNotBlank() }).joinToString(" · ")
    }
}
