package com.iptvcinema.tv.features.movies

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.MoviesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.MoviesPersonalSnapshot
import com.iptvcinema.tv.features.browse.categoriesSection
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import com.iptvcinema.tv.features.home.HomeSectionsBuilder
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard

/**
 * Turns the loaded snapshots into the ordered Movies tab rails.
 *
 * Order: Continue Watching (movies), New movies, Browse by category, Because you watched,
 * Top rated movies, More in {category}.
 *
 * A movie appears once across the discovery rails (New movies, Because you watched, More in
 * {category}); the first place wins. Top rated keeps its ranking. Discovery and ranked rails need
 * [HomeSectionsBuilder.MIN_RAIL_ITEMS] to show; Continue Watching shows with one card.
 */
object MoviesSectionsBuilder {
    fun build(
        catalog: MoviesCatalogSnapshot,
        personal: MoviesPersonalSnapshot,
        continueWatching: List<HomeContentCard>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
        countLabel: (Int) -> String,
    ): List<HomeSection> {
        fun List<CatalogMovie>.allowed() = filterNot { isBlocked(it.categoryName, it.rating) }
        fun CatalogMovie.card(rank: Int? = null) =
            toHomeContentCard(isFavorite = isFavorite(id, FavoriteContentType.MOVIE), rank = rank)

        val usedIds = mutableSetOf<String>()

        // Takes unused movies; only a rail that will be shown claims its ids.
        fun discovery(id: String, title: HomeRailTitle, movies: List<CatalogMovie>): HomeSection? {
            val fresh = movies.allowed().distinctBy { it.id }.filterNot { it.id in usedIds }
                .take(HomeSectionsBuilder.MAX_RAIL_ITEMS)
            if (fresh.size < HomeSectionsBuilder.MIN_RAIL_ITEMS) return null
            usedIds += fresh.map { it.id }
            return HomeSection.Rail(id, title, fresh.map { it.card() })
        }

        return buildList {
            continueWatching
                .filter { it.contentType == "movie" }
                .takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.ContinueWatching(it)) }

            discovery(HomeSectionIds.NEW_MOVIES, HomeRailTitle(R.string.home_new_movies), catalog.newest)
                ?.let(::add)

            categoriesSection(
                categories = catalog.categories,
                cardType = BrowseCardTypes.MOVIE_CATEGORY,
                isBlocked = { isBlocked(it, null) },
                countLabel = countLabel,
            )?.let(::add)

            personal.becauseYouWatched?.let { because ->
                discovery(
                    HomeSectionIds.BECAUSE_YOU_WATCHED,
                    HomeRailTitle(R.string.home_because_you_watched, because.anchorTitle),
                    because.movies.filterNot { it.id in personal.watchedMovieIds },
                )?.let(::add)
            }

            catalog.topRated.allowed().distinctBy { it.id }.take(HomeSectionsBuilder.TOP_RATED_COUNT)
                .takeIf { it.size >= HomeSectionsBuilder.MIN_RAIL_ITEMS }
                ?.let { top ->
                    add(
                        HomeSection.TopRated(
                            id = HomeSectionIds.TOP_RATED_MOVIES,
                            title = HomeRailTitle(R.string.home_top_rated_movies),
                            items = top.mapIndexed { index, movie -> movie.card(rank = index + 1) },
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
        }
    }
}
