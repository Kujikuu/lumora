package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toMovieItem
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.home.HomeCatalogSnapshot
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.HomePersonalSnapshot
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard

/**
 * Turns the loaded snapshots into the ordered Home sections.
 *
 * Order: Continue Watching, Next episode, New movies, Because you watched, Top rated movies,
 * Top rated series, More in {category}, Recently watched channels.
 *
 * A movie appears once across the hero and the discovery rails (New movies, Because you
 * watched, More in {category}); the first place wins. Ranked and personal rails keep their
 * items. Discovery and ranked rails need [MIN_RAIL_ITEMS] to show; personal rails show with one.
 */
object HomeSectionsBuilder {
    const val MIN_RAIL_ITEMS = 4
    const val MAX_RAIL_ITEMS = 20
    const val TOP_RATED_COUNT = 10

    fun build(
        catalog: HomeCatalogSnapshot,
        personal: HomePersonalSnapshot,
        continueWatching: List<HomeContentCard>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
    ): HomeContent {
        fun List<CatalogMovie>.allowed() = filterNot { isBlocked(it.categoryName, it.rating) }
        fun CatalogMovie.card(rank: Int? = null) =
            toHomeContentCard(isFavorite = isFavorite(id, FavoriteContentType.MOVIE), rank = rank)

        val newest = catalog.newestMovies.allowed()
        val heroMovies = HomeContentRules.pickHero(catalog.heroCandidates.allowed())
            .ifEmpty { newest.filterNot { it.posterUrl.isNullOrBlank() }.take(HomeContentRules.HERO_COUNT) }
        val usedIds = heroMovies.mapTo(mutableSetOf()) { it.id }

        // Takes unused movies; only a rail that will be shown claims its ids.
        fun discovery(id: String, title: HomeRailTitle, movies: List<CatalogMovie>): HomeSection? {
            val fresh = movies.allowed().filterNot { it.id in usedIds }.take(MAX_RAIL_ITEMS)
            if (fresh.size < MIN_RAIL_ITEMS) return null
            usedIds += fresh.map { it.id }
            return HomeSection.Rail(id, title, fresh.map { it.card() })
        }

        val sections = buildList {
            if (continueWatching.isNotEmpty()) add(HomeSection.ContinueWatching(continueWatching))

            personal.nextEpisodes
                .filterNot { isBlocked(it.series?.categoryName, it.series?.rating) }
                .map { it.toHomeContentCard(isFavorite(it.episode.id, FavoriteContentType.EPISODE)) }
                .takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.NextEpisode(it)) }

            discovery(HomeSectionIds.NEW_MOVIES, HomeRailTitle(R.string.home_new_movies), newest)
                ?.let(::add)

            personal.becauseYouWatched?.let { because ->
                discovery(
                    HomeSectionIds.BECAUSE_YOU_WATCHED,
                    HomeRailTitle(R.string.home_because_you_watched, because.anchorTitle),
                    because.movies.filterNot { it.id in personal.watchedMovieIds },
                )?.let(::add)
            }

            catalog.topRatedMovies.allowed().take(TOP_RATED_COUNT)
                .takeIf { it.size >= MIN_RAIL_ITEMS }
                ?.let { top ->
                    add(
                        HomeSection.TopRated(
                            id = HomeSectionIds.TOP_RATED_MOVIES,
                            title = HomeRailTitle(R.string.home_top_rated_movies),
                            items = top.mapIndexed { index, movie -> movie.card(rank = index + 1) },
                        ),
                    )
                }

            catalog.topRatedSeries.filterNot { isBlocked(it.categoryName, it.rating) }.take(TOP_RATED_COUNT)
                .takeIf { it.size >= MIN_RAIL_ITEMS }
                ?.let { top ->
                    add(
                        HomeSection.TopRated(
                            id = HomeSectionIds.TOP_RATED_SERIES,
                            title = HomeRailTitle(R.string.home_top_rated_series),
                            items = top.mapIndexed { index, series ->
                                series.toHomeContentCard(
                                    isFavorite = isFavorite(series.id, FavoriteContentType.SERIES),
                                    rank = index + 1,
                                )
                            },
                        ),
                    )
                }

            personal.categoryRails
                .filterNot { isBlocked(it.categoryName, null) }
                .forEach { rail ->
                    discovery(
                        HomeSectionIds.category(rail.categoryName),
                        HomeRailTitle(R.string.home_more_in_category, rail.categoryName),
                        rail.movies,
                    )?.let(::add)
                }

            personal.recentChannels
                .filterNot { isBlocked(it.categoryName, null) }
                .map { it.toHomeContentCard(isFavorite(it.id, FavoriteContentType.CHANNEL)) }
                .takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.RecentChannels(it)) }
        }

        val hero = heroMovies.map { it.toMovieItem(isFavorite = isFavorite(it.id, FavoriteContentType.MOVIE)) }
        return HomeContent(hero = hero, sections = sections)
    }
}
