package com.iptvcinema.tv.features.mylist

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection

object MyListSectionIds {
    const val MOVIES = "mylist_movies"
    const val SERIES = "mylist_series"
    const val EPISODES = "mylist_episodes"
    const val CHANNELS = "mylist_channels"
}

/**
 * Turns My List into rails: Movies, Series, Episodes, Channels (in the order saved), then
 * Continue Watching. Titles parental controls block are left out, and so are favorites the viewer
 * just removed ([hiddenKeys], see [favoriteKey]) while the server catches up.
 */
object MyListSectionsBuilder {
    fun build(
        saved: MyListSaved,
        continueWatching: List<HomeContentCard>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        hiddenKeys: Set<String> = emptySet(),
    ): List<HomeSection> {
        fun List<MyListEntry>.visible() =
            filterNot { isBlocked(it.categoryName, it.rating) || it.card.favoriteKey() in hiddenKeys }.map { it.card }

        return buildList {
            saved.movies.visible().takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.Rail(MyListSectionIds.MOVIES, HomeRailTitle(R.string.mylist_filter_movies), it)) }
            saved.series.visible().takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.Rail(MyListSectionIds.SERIES, HomeRailTitle(R.string.mylist_filter_series), it)) }
            saved.episodes.visible().takeIf { it.isNotEmpty() }
                ?.let { add(HomeSection.Rail(MyListSectionIds.EPISODES, HomeRailTitle(R.string.mylist_episodes), it)) }
            saved.channels.visible().takeIf { it.isNotEmpty() }
                ?.let {
                    add(HomeSection.Channels(MyListSectionIds.CHANNELS, HomeRailTitle(R.string.mylist_favorite_channels), it))
                }
            if (continueWatching.isNotEmpty()) add(HomeSection.ContinueWatching(continueWatching))
        }
    }
}

/** Identifies a saved title across rails: the same id can be a movie and a channel. */
fun HomeContentCard.favoriteKey(): String = "$contentType:$contentId"
