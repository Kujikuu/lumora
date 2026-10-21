package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toMovieItem
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.HomeCatalogSnapshot
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.HomePersonalSnapshot
import com.iptvcinema.tv.core.util.CatalogLabels
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard

/**
 * Turns the loaded snapshots into the ordered Home sections.
 *
 * Order, most personal and most used first: Continue Watching, Next episode, Live now, Browse
 * by category, Recently added, Because you watched, two category rails, My List.
 *
 * A movie appears once across the hero and the discovery rails (Recently added, Because you
 * watched, category rails); the first place wins. Discovery rails need [MIN_RAIL_ITEMS] to show;
 * personal rows show with one. Titles and category names are shown cleaned (see [CatalogLabels]);
 * ids and raw names stay as stored, since navigation and parental filters use them.
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
        myList: List<FavoriteItem> = emptyList(),
        categoryLabel: (String) -> String = { it },
    ): HomeContent {
        fun List<CatalogMovie>.allowed() = filterNot { isBlocked(it.categoryName, it.rating) }
        fun CatalogMovie.card() = toHomeContentCard(isFavorite = isFavorite(id, FavoriteContentType.MOVIE))
            .let { card -> card.copy(title = CatalogLabels.cleanTitle(card.title), genres = card.genres.map(categoryLabel)) }

        val newest = catalog.newestMovies.allowed()
        val heroMovies = HomeContentRules.pickHero(catalog.heroCandidates.allowed())
            .ifEmpty {
                newest
                    .filter { !it.posterUrl.isNullOrBlank() && HomeContentRules.isHeroWorthy(it.title, it.categoryName) }
                    .take(HomeContentRules.HERO_COUNT)
            }
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

            liveNow(personal, isBlocked, isFavorite, categoryLabel)?.let(::add)
            categoryTiles(personal, isBlocked, categoryLabel)?.let(::add)

            discovery(HomeSectionIds.NEW_MOVIES, HomeRailTitle(R.string.home_recently_added), newest)
                ?.let(::add)

            personal.becauseYouWatched?.let { because ->
                discovery(
                    HomeSectionIds.BECAUSE_YOU_WATCHED,
                    HomeRailTitle(R.string.home_because_you_watched, CatalogLabels.cleanTitle(because.anchorTitle)),
                    because.movies.filterNot { it.id in personal.watchedMovieIds },
                )?.let(::add)
            }

            personal.categoryRails
                .filterNot { isBlocked(it.categoryName, null) }
                .forEach { rail ->
                    discovery(
                        HomeSectionIds.category(rail.categoryName),
                        HomeRailTitle(R.string.home_more_in_category, text = categoryLabel(rail.categoryName)),
                        rail.movies,
                    )?.let(::add)
                }

            myListSection(myList)?.let(::add)
        }

        val hero = heroMovies.map { movie ->
            movie.copy(title = CatalogLabels.cleanTitle(movie.title))
                .toMovieItem(isFavorite = isFavorite(movie.id, FavoriteContentType.MOVIE))
        }
        return HomeContent(hero = hero, sections = sections)
    }

    /** Channels the viewer watched lately or saved, each with what is on now when known. */
    private fun liveNow(
        personal: HomePersonalSnapshot,
        isBlocked: (String?, String?) -> Boolean,
        isFavorite: (String, FavoriteContentType) -> Boolean,
        categoryLabel: (String) -> String,
    ): HomeSection? =
        personal.liveChannels
            .filterNot { isBlocked(it.categoryName, null) }
            .map { channel ->
                channel.toHomeContentCard(isFavorite(channel.id, FavoriteContentType.CHANNEL)).copy(
                    subtitle = personal.nowPlaying[channel.id]
                        ?: channel.categoryName?.takeIf { it.isNotBlank() }?.let(categoryLabel),
                )
            }
            .takeIf { it.isNotEmpty() }
            ?.let { HomeSection.LiveNow(it) }

    /** Shortcut tiles that open one category's grid. The raw name is the id the grid needs. */
    private fun categoryTiles(
        personal: HomePersonalSnapshot,
        isBlocked: (String?, String?) -> Boolean,
        categoryLabel: (String) -> String,
    ): HomeSection? =
        personal.categoryTiles
            .filter { it.name.isNotBlank() && !isBlocked(it.name, null) }
            .distinctBy { it.name }
            .map { category ->
                HomeContentCard(
                    contentId = category.name,
                    contentType = BrowseCardTypes.MOVIE_CATEGORY,
                    title = categoryLabel(category.name),
                    imageUrl = category.posterUrl,
                    backdropUrl = category.backdropUrl ?: category.posterUrl,
                )
            }
            .takeIf { it.isNotEmpty() }
            ?.let { HomeSection.Categories(HomeRailTitle(R.string.browse_by_category), it) }

    /** Saved movies and series. Channels have Live now; episodes open from their series. */
    private fun myListSection(myList: List<FavoriteItem>): HomeSection? =
        myList
            .mapNotNull { favorite ->
                val type = when (favorite.contentType) {
                    FavoriteContentType.MOVIE -> "movie"
                    FavoriteContentType.SERIES -> "series"
                    else -> return@mapNotNull null
                }
                HomeContentCard(
                    contentId = favorite.contentId,
                    contentType = type,
                    title = CatalogLabels.cleanTitle(favorite.title),
                    imageUrl = favorite.posterUrl,
                    backdropUrl = favorite.posterUrl,
                    isFavorite = true,
                )
            }
            .distinctBy { "${it.contentType}:${it.contentId}" }
            .take(MAX_RAIL_ITEMS)
            .takeIf { it.isNotEmpty() }
            ?.let { HomeSection.Rail(HomeSectionIds.MY_LIST, HomeRailTitle(R.string.nav_my_list), it) }
}
