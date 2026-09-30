package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.HomeContentCard

object HomeSpotlightMapper {
    fun fromHero(movie: MovieItem): HomeSpotlight = HomeSpotlight(
        key = "hero:${movie.id}",
        kind = HomeSpotlightKind.Movie,
        title = movie.title,
        backdropUrl = movie.backdropUrl ?: movie.imageUrl,
        ratingBadge = HeroCarouselLogic.ratingBadge(movie.rating),
        metadata = HeroCarouselLogic.metadata(movie),
        plot = movie.plot.takeIf { it.isNotBlank() },
        is4K = movie.is4K,
    )

    fun fromCard(card: HomeContentCard): HomeSpotlight {
        val kind = when (card.contentType) {
            "series" -> HomeSpotlightKind.Series
            "episode" -> HomeSpotlightKind.Episode
            "channel" -> HomeSpotlightKind.Channel
            BrowseCardTypes.MOVIE_CATEGORY, BrowseCardTypes.SERIES_CATEGORY -> HomeSpotlightKind.Category
            else -> HomeSpotlightKind.Movie
        }
        return HomeSpotlight(
            key = "${card.contentType}:${card.contentId}",
            kind = kind,
            title = card.title,
            backdropUrl = card.backdropUrl ?: card.imageUrl,
            ratingBadge = HeroCarouselLogic.ratingBadge(card.rating),
            metadata = cardMetadata(card, kind),
            plot = card.plot,
            progress = card.progress,
        )
    }

    private fun cardMetadata(card: HomeContentCard, kind: HomeSpotlightKind): List<String> {
        val personal = listOfNotNull(card.subtitle, card.remainingTimeLabel).filter { it.isNotBlank() }
        if (kind == HomeSpotlightKind.Channel || kind == HomeSpotlightKind.Category || personal.isNotEmpty()) {
            return personal
        }
        return listOfNotNull(
            card.year,
            card.genres.take(2).joinToString(" · ").takeIf { it.isNotBlank() },
            card.runtimeOrEpisodes,
        ).filter { it.isNotBlank() }.distinct()
    }
}
