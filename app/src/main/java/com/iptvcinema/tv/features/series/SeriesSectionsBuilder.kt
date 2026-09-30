package com.iptvcinema.tv.features.series

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.SeriesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.SeriesPersonalSnapshot
import com.iptvcinema.tv.features.browse.categoriesSection
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import com.iptvcinema.tv.features.home.HomeSectionsBuilder
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard

/**
 * Turns the loaded snapshots into the ordered Series tab rails.
 *
 * Order: Continue Watching (episodes), Next episode, Latest series, Browse by category,
 * Because you watched, Top rated series, More in {category}.
 *
 * A series appears once across the discovery rails (Latest, Because you watched, More in
 * {category}); the first place wins. Top rated keeps its ranking.
 */
object SeriesSectionsBuilder {
    fun build(
        catalog: SeriesCatalogSnapshot,
        personal: SeriesPersonalSnapshot,
        continueWatching: List<HomeContentCard>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
        countLabel: (Int) -> String,
    ): List<HomeSection> {
        fun List<CatalogSeries>.allowed() = filterNot { isBlocked(it.categoryName, it.rating) }
        fun CatalogSeries.card(rank: Int? = null) =
            toHomeContentCard(isFavorite = isFavorite(id, FavoriteContentType.SERIES), rank = rank)

        val usedIds = mutableSetOf<String>()

        fun discovery(id: String, title: HomeRailTitle, series: List<CatalogSeries>): HomeSection? {
            val fresh = series.allowed().distinctBy { it.id }.filterNot { it.id in usedIds }
                .take(HomeSectionsBuilder.MAX_RAIL_ITEMS)
            if (fresh.size < HomeSectionsBuilder.MIN_RAIL_ITEMS) return null
            usedIds += fresh.map { it.id }
            return HomeSection.Rail(id, title, fresh.map { it.card() })
        }

        return buildList {
            continueWatching
                .filter { it.contentType == "episode" }
                .takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.ContinueWatching(it)) }

            personal.nextEpisodes
                .filterNot { isBlocked(it.series?.categoryName, it.series?.rating) }
                .map { it.toHomeContentCard(isFavorite(it.episode.id, FavoriteContentType.EPISODE)) }
                .takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.NextEpisode(it)) }

            discovery(HomeSectionIds.LATEST_SERIES, HomeRailTitle(R.string.series_latest), catalog.latest)
                ?.let(::add)

            categoriesSection(
                categories = catalog.categories,
                cardType = BrowseCardTypes.SERIES_CATEGORY,
                isBlocked = { isBlocked(it, null) },
                countLabel = countLabel,
            )?.let(::add)

            personal.becauseYouWatched?.let { because ->
                discovery(
                    HomeSectionIds.BECAUSE_YOU_WATCHED_SERIES,
                    HomeRailTitle(R.string.home_because_you_watched, because.anchorTitle),
                    because.series.filterNot { it.id in personal.watchedSeriesIds },
                )?.let(::add)
            }

            catalog.topRated.allowed().distinctBy { it.id }.take(HomeSectionsBuilder.TOP_RATED_COUNT)
                .takeIf { it.size >= HomeSectionsBuilder.MIN_RAIL_ITEMS }
                ?.let { top ->
                    add(
                        HomeSection.TopRated(
                            id = HomeSectionIds.TOP_RATED_SERIES,
                            title = HomeRailTitle(R.string.home_top_rated_series),
                            items = top.mapIndexed { index, series -> series.card(rank = index + 1) },
                        ),
                    )
                }

            personal.categoryRails
                .filterNot { isBlocked(it.categoryName, null) }
                .forEach { rail ->
                    discovery(
                        HomeSectionIds.category(rail.categoryName),
                        HomeRailTitle(R.string.home_more_in_category, rail.categoryName),
                        rail.series,
                    )?.let(::add)
                }
        }
    }
}
