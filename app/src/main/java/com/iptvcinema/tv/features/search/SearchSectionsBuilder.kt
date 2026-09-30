package com.iptvcinema.tv.features.search

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import com.iptvcinema.tv.features.home.HomeSectionsBuilder
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard

object SearchSectionIds {
    const val MOVIES = "search_movies"
    const val SERIES = "search_series"
    const val CHANNELS = "search_channels"
    const val RECENT = "search_recent"
}

/** Search results and, before the viewer types, suggestions, as Home-style rails. */
object SearchSectionsBuilder {
    fun results(
        movies: List<MovieItem>,
        series: List<SeriesItem>,
        channels: List<ChannelItem>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
    ): List<HomeSection> = buildList {
        movies
            .filterNot { isBlocked(it.categoryName ?: it.genres.firstOrNull(), it.rating) }
            .map { it.toCard(isFavorite(it.id, FavoriteContentType.MOVIE)) }
            .takeIf { it.isNotEmpty() }
            ?.let { add(HomeSection.Rail(SearchSectionIds.MOVIES, HomeRailTitle(R.string.search_rail_movies), it)) }
        series
            .filterNot { isBlocked(it.categoryName ?: it.genres.firstOrNull(), it.rating) }
            .map { it.toCard(isFavorite(it.id, FavoriteContentType.SERIES)) }
            .takeIf { it.isNotEmpty() }
            ?.let { add(HomeSection.Rail(SearchSectionIds.SERIES, HomeRailTitle(R.string.search_rail_series), it)) }
        channels
            .filterNot { isBlocked(it.category, null) }
            .map { it.toCard(isFavorite(it.id, FavoriteContentType.CHANNEL)) }
            .takeIf { it.isNotEmpty() }
            ?.let { add(HomeSection.Channels(SearchSectionIds.CHANNELS, HomeRailTitle(R.string.search_rail_channels), it)) }
    }

    /** Before typing: recent searches, then the best rated movies and series as ideas. */
    fun suggestions(
        recentSearches: List<String>,
        topMovies: List<CatalogMovie>,
        topSeries: List<CatalogSeries>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
    ): List<HomeSection> = buildList {
        recentSearches
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .map { term -> HomeContentCard(contentId = term, contentType = BrowseCardTypes.SEARCH_TERM, title = term) }
            .takeIf { it.isNotEmpty() }
            ?.let {
                add(HomeSection.Categories(HomeRailTitle(R.string.search_rail_recent), it, id = SearchSectionIds.RECENT))
            }
        topMovies
            .filterNot { isBlocked(it.categoryName, it.rating) }
            .take(HomeSectionsBuilder.TOP_RATED_COUNT)
            .takeIf { it.size >= HomeSectionsBuilder.MIN_RAIL_ITEMS }
            ?.let { top ->
                add(
                    HomeSection.TopRated(
                        id = HomeSectionIds.TOP_RATED_MOVIES,
                        title = HomeRailTitle(R.string.home_top_rated_movies),
                        items = top.mapIndexed { index, movie ->
                            movie.toHomeContentCard(isFavorite(movie.id, FavoriteContentType.MOVIE), rank = index + 1)
                        },
                    ),
                )
            }
        topSeries
            .filterNot { isBlocked(it.categoryName, it.rating) }
            .take(HomeSectionsBuilder.TOP_RATED_COUNT)
            .takeIf { it.size >= HomeSectionsBuilder.MIN_RAIL_ITEMS }
            ?.let { top ->
                add(
                    HomeSection.TopRated(
                        id = HomeSectionIds.TOP_RATED_SERIES,
                        title = HomeRailTitle(R.string.home_top_rated_series),
                        items = top.mapIndexed { index, series ->
                            series.toHomeContentCard(isFavorite(series.id, FavoriteContentType.SERIES), rank = index + 1)
                        },
                    ),
                )
            }
    }

    private fun MovieItem.toCard(isFavorite: Boolean) = HomeContentCard(
        contentId = id,
        contentType = "movie",
        title = title,
        imageUrl = imageUrl,
        backdropUrl = backdropUrl ?: imageUrl,
        year = year.takeIf { it > 0 }?.toString(),
        genres = genres.ifEmpty { listOfNotNull(categoryName?.takeIf { it.isNotBlank() }) },
        plot = plot.takeIf { it.isNotBlank() },
        runtimeOrEpisodes = runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
        rating = rating.takeIf { it.isNotBlank() },
        isFavorite = isFavorite,
    )

    private fun SeriesItem.toCard(isFavorite: Boolean) = HomeContentCard(
        contentId = id,
        contentType = "series",
        title = title,
        imageUrl = imageUrl,
        backdropUrl = backdropUrl ?: imageUrl,
        year = year.takeIf { it > 0 }?.toString(),
        genres = genres,
        plot = plot.takeIf { it.isNotBlank() },
        rating = rating.takeIf { it.isNotBlank() },
        isFavorite = isFavorite,
    )

    private fun ChannelItem.toCard(isFavorite: Boolean) = HomeContentCard(
        contentId = id,
        contentType = "channel",
        title = name,
        subtitle = currentProgram.takeIf { it.isNotBlank() } ?: category.takeIf { it.isNotBlank() },
        imageUrl = logoUrl,
        isFavorite = isFavorite,
    )
}
