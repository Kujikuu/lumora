package com.iptvcinema.tv.features.details

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.home.RelatedItem
import com.iptvcinema.tv.core.model.home.RelatedSnapshot
import com.iptvcinema.tv.core.model.home.toFavoriteContentType
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeSectionIds
import com.iptvcinema.tv.features.home.HomeSectionsBuilder

enum class RelatedKind { Movie, Series }

object RelatedSectionIds {
    const val SIMILAR = "related_similar"
}

/**
 * The rails of a "More like this" page: titles from the same category, then top rated and new
 * ones. The title itself never shows, each title shows once, and blocked titles are left out.
 * The similar rail shows with a single title; the others need [HomeSectionsBuilder.MIN_RAIL_ITEMS].
 */
object RelatedSectionsBuilder {
    fun build(
        kind: RelatedKind,
        snapshot: RelatedSnapshot,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
    ): List<HomeSection> {
        val used = mutableSetOf(snapshot.anchorId)

        fun rail(id: String, title: HomeRailTitle, items: List<RelatedItem>, minItems: Int): HomeSection? {
            val fresh = items
                .filterNot { isBlocked(it.categoryName, it.rating) || it.card.contentId in used }
                .distinctBy { it.card.contentId }
                .take(HomeSectionsBuilder.MAX_RAIL_ITEMS)
            if (fresh.size < minItems) return null
            used += fresh.map { it.card.contentId }
            val cards = fresh.map { it.card.copy(isFavorite = isFavorite(it.card.contentId, it.card.toFavoriteContentType())) }
            return HomeSection.Rail(id, title, cards)
        }

        return listOfNotNull(
            rail(RelatedSectionIds.SIMILAR, HomeRailTitle(R.string.related_more_like_this), snapshot.similar, minItems = 1),
            when (kind) {
                RelatedKind.Movie -> rail(
                    HomeSectionIds.TOP_RATED_MOVIES,
                    HomeRailTitle(R.string.home_top_rated_movies),
                    snapshot.topRated,
                    HomeSectionsBuilder.MIN_RAIL_ITEMS,
                )
                RelatedKind.Series -> rail(
                    HomeSectionIds.TOP_RATED_SERIES,
                    HomeRailTitle(R.string.home_top_rated_series),
                    snapshot.topRated,
                    HomeSectionsBuilder.MIN_RAIL_ITEMS,
                )
            },
            when (kind) {
                RelatedKind.Movie -> rail(
                    HomeSectionIds.NEW_MOVIES,
                    HomeRailTitle(R.string.home_new_movies),
                    snapshot.newest,
                    HomeSectionsBuilder.MIN_RAIL_ITEMS,
                )
                RelatedKind.Series -> rail(
                    HomeSectionIds.LATEST_SERIES,
                    HomeRailTitle(R.string.series_latest),
                    snapshot.newest,
                    HomeSectionsBuilder.MIN_RAIL_ITEMS,
                )
            },
        )
    }
}
