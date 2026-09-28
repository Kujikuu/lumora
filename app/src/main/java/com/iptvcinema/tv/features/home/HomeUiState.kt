package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.util.continueWatchingKey
import androidx.annotation.StringRes
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.toFavoriteContentType

data class HomeUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val hero: List<MovieItem> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
    val syncBannerText: String? = null,
    val refreshState: CatalogRefreshState = CatalogRefreshState.Idle,
)

/** What [HomeSectionsBuilder] produces: the hero slides and the rails below them, in order. */
data class HomeContent(
    val hero: List<MovieItem>,
    val sections: List<HomeSection>,
)

data class HomeRailTitle(
    @StringRes val resId: Int,
    val argument: String? = null,
)

object HomeSectionIds {
    const val CONTINUE = "continue"
    const val NEXT_EPISODE = "next_episode"
    const val NEW_MOVIES = "new_movies"
    const val BECAUSE_YOU_WATCHED = "because_you_watched"
    const val TOP_RATED_MOVIES = "top_rated_movies"
    const val TOP_RATED_SERIES = "top_rated_series"
    const val RECENT_CHANNELS = "recent_channels"

    fun category(name: String): String = "category:$name"
}

sealed interface HomeSection {
    val id: String
    val title: HomeRailTitle
    val items: List<HomeContentCard>

    fun withItems(items: List<HomeContentCard>): HomeSection

    data class ContinueWatching(override val items: List<HomeContentCard>) : HomeSection {
        override val id = HomeSectionIds.CONTINUE
        override val title = HomeRailTitle(R.string.home_continue_watching)
        override fun withItems(items: List<HomeContentCard>) = copy(items = items)
    }

    data class NextEpisode(override val items: List<HomeContentCard>) : HomeSection {
        override val id = HomeSectionIds.NEXT_EPISODE
        override val title = HomeRailTitle(R.string.home_next_episode)
        override fun withItems(items: List<HomeContentCard>) = copy(items = items)
    }

    data class Rail(
        override val id: String,
        override val title: HomeRailTitle,
        override val items: List<HomeContentCard>,
    ) : HomeSection {
        override fun withItems(items: List<HomeContentCard>) = copy(items = items)
    }

    /** Ranked rail: cards carry [HomeContentCard.rank]. */
    data class TopRated(
        override val id: String,
        override val title: HomeRailTitle,
        override val items: List<HomeContentCard>,
    ) : HomeSection {
        override fun withItems(items: List<HomeContentCard>) = copy(items = items)
    }

    data class RecentChannels(override val items: List<HomeContentCard>) : HomeSection {
        override val id = HomeSectionIds.RECENT_CHANNELS
        override val title = HomeRailTitle(R.string.home_recent_channels)
        override fun withItems(items: List<HomeContentCard>) = copy(items = items)
    }
}

fun List<HomeSection>.withFavorite(
    contentId: String,
    type: FavoriteContentType,
    isFavorite: Boolean,
): List<HomeSection> = map { section ->
    if (section.items.none { it.contentId == contentId && it.toFavoriteContentType() == type }) {
        section
    } else {
        section.withItems(
            section.items.map { card ->
                if (card.contentId == contentId && card.toFavoriteContentType() == type) {
                    card.copy(isFavorite = isFavorite)
                } else {
                    card
                }
            },
        )
    }
}

enum class HomeSpotlightKind { Movie, Series, Episode, Channel }

/** What the top panel and backdrop show for the hero slide or the focused card. */
data class HomeSpotlight(
    val key: String,
    val kind: HomeSpotlightKind,
    val title: String,
    val backdropUrl: String?,
    val ratingBadge: String? = null,
    val metadata: List<String> = emptyList(),
    val plot: String? = null,
    val progress: Float? = null,
    val is4K: Boolean = false,
)

/** Drops Continue Watching and Next episode cards whose title the user just removed. */
fun HomeUiState.withoutContinueWatching(hiddenKeys: Set<String>): HomeUiState {
    if (hiddenKeys.isEmpty()) return this
    val filtered = sections.mapNotNull { section ->
        when (section) {
            is HomeSection.ContinueWatching, is HomeSection.NextEpisode ->
                section.withItems(section.items.filterNot { it.continueWatchingKey() in hiddenKeys })
                    .takeIf { it.items.isNotEmpty() }
            else -> section
        }
    }
    return copy(sections = filtered)
}
