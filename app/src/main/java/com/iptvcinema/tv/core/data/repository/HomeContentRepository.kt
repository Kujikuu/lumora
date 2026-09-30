package com.iptvcinema.tv.core.data.repository

import android.util.Log
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.mapper.CatalogEntityMapper.toDomain
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.home.HomeBecauseYouWatched
import com.iptvcinema.tv.core.model.home.HomeCatalogSnapshot
import com.iptvcinema.tv.core.model.home.HomeCategoryRail
import com.iptvcinema.tv.core.model.home.HomeNextEpisode
import com.iptvcinema.tv.core.model.home.HomePersonalSnapshot
import com.iptvcinema.tv.core.player.EpisodeSequenceHelper
import com.iptvcinema.tv.features.home.HomeContentRules
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Loads the data behind each Home section with one-shot queries. Only local data is read:
 * Home must never wait on the network, so a series whose episodes were never opened simply
 * has no "Next episode" card.
 */
@Singleton
class HomeContentRepository @Inject constructor(
    private val catalogDaoFacade: CatalogDaoFacade,
    private val catalogRepository: CatalogRepository,
) {
    suspend fun loadCatalog(sourceId: String?, isDemoMode: Boolean): HomeCatalogSnapshot = when {
        isDemoMode -> demoCatalog()
        sourceId == null -> HomeCatalogSnapshot()
        else -> safely("catalog", HomeCatalogSnapshot()) {
            val movies = catalogDaoFacade.movies
            HomeCatalogSnapshot(
                heroCandidates = movies.getNewestWithArtwork(sourceId, HomeContentRules.HERO_RECENCY_POOL)
                    .map { it.toDomain() },
                newestMovies = movies.getNewest(sourceId, RAIL_FETCH_LIMIT).map { it.toDomain() },
                topRatedMovies = movies.getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT)
                    .map { it.toDomain() },
                topRatedSeries = catalogDaoFacade.series
                    .getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT)
                    .map { it.toDomain() },
            )
        }
    }

    suspend fun loadPersonal(
        sourceId: String?,
        isDemoMode: Boolean,
        history: List<WatchHistoryItem>,
        isCategoryBlocked: (String) -> Boolean,
    ): HomePersonalSnapshot {
        if (isDemoMode) return demoPersonal(history)
        if (sourceId == null) return HomePersonalSnapshot()
        val watchedMovieIds = history
            .filter { it.contentType == WatchHistoryContentType.MOVIE }
            .mapTo(mutableSetOf()) { it.contentId }
        val becauseYouWatched = safely("because", null) { loadBecauseYouWatched(sourceId, history, watchedMovieIds) }
        return HomePersonalSnapshot(
            nextEpisodes = safely("nextEpisodes", emptyList()) { loadNextEpisodes(sourceId, history) },
            becauseYouWatched = becauseYouWatched?.first,
            categoryRails = safely("categories", emptyList()) {
                loadCategoryRails(sourceId, watchedMovieIds.toList(), becauseYouWatched?.second, isCategoryBlocked)
            },
            recentChannels = safely("channels", emptyList()) { loadRecentChannels(sourceId, history) },
            watchedMovieIds = watchedMovieIds,
        )
    }

    suspend fun loadNextEpisodes(sourceId: String, history: List<WatchHistoryItem>): List<HomeNextEpisode> =
        HomeContentRules.nextEpisodeCandidates(history, NEXT_EPISODE_LIMIT).mapNotNull { finished ->
            val seriesId = finished.seriesId ?: return@mapNotNull null
            val sid = finished.sourceId?.takeIf { it.isNotBlank() } ?: sourceId
            val episodes = catalogRepository.getEpisodesForSeries(sid, seriesId)
            val next = EpisodeSequenceHelper.nextEpisode(episodes, finished.contentId) ?: return@mapNotNull null
            HomeNextEpisode(series = catalogRepository.getSeries(sid, seriesId), episode = next)
        }

    /** Returns the rail plus the anchor movie's category, so a category rail does not repeat it. */
    suspend fun loadBecauseYouWatched(
        sourceId: String,
        history: List<WatchHistoryItem>,
        watchedMovieIds: Set<String>,
    ): Pair<HomeBecauseYouWatched, String?>? {
        val last = HomeContentRules.lastWatchedMovie(history) ?: return null
        val anchor = catalogRepository.getMovie(sourceId, last.contentId) ?: return null
        val categoryId = anchor.categoryId?.takeIf { it.isNotBlank() } ?: return null
        val related = catalogRepository
            .getRelatedMovies(sourceId, categoryId, anchor.id, RAIL_FETCH_LIMIT)
            .filterNot { it.id in watchedMovieIds }
        return HomeBecauseYouWatched(anchorTitle = anchor.title, movies = related) to anchor.categoryName
    }

    suspend fun loadCategoryRails(
        sourceId: String,
        watchedMovieIds: List<String>,
        excludedCategory: String?,
        isCategoryBlocked: (String) -> Boolean,
        railCount: Int = CATEGORY_RAIL_COUNT,
    ): List<HomeCategoryRail> {
        val movieDao = catalogDaoFacade.movies
        val watchedCategories = watchedMovieIds
            .take(AFFINITY_HISTORY_LIMIT)
            .takeIf { it.isNotEmpty() }
            ?.let { ids -> movieDao.getByIds(sourceId, ids).map { it.categoryName } }
            .orEmpty()
        val fallback = movieDao.getLargestCategoryNames(sourceId, CATEGORY_FALLBACK_LIMIT.coerceAtLeast(railCount * 2))
        val categories = HomeContentRules.rankCategories(
            watchedCategories = watchedCategories,
            fallback = fallback,
            limit = railCount,
            isBlocked = { it == excludedCategory || isCategoryBlocked(it) },
        )
        return categories.map { name ->
            HomeCategoryRail(
                categoryName = name,
                movies = movieDao.getByCategoryNameLimited(sourceId, name, RAIL_FETCH_LIMIT).map { it.toDomain() },
            )
        }
    }

    private suspend fun loadRecentChannels(sourceId: String, history: List<WatchHistoryItem>): List<CatalogChannel> {
        val ids = HomeContentRules.recentChannelIds(history, RECENT_CHANNEL_LIMIT)
        if (ids.isEmpty()) return emptyList()
        val byId = catalogDaoFacade.channels.getByIds(sourceId, ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    private fun demoCatalog(): HomeCatalogSnapshot {
        val movies = FakeDataProvider.movies.map { it.toDemoCatalogMovie() }
        return HomeCatalogSnapshot(
            heroCandidates = movies.filter { !it.backdropUrl.isNullOrBlank() },
            newestMovies = movies,
            topRatedMovies = movies.sortedByDescending { HomeContentRules.ratingValue(it.rating) },
            topRatedSeries = FakeDataProvider.seriesList
                .map { it.toDemoCatalogSeries() }
                .sortedByDescending { HomeContentRules.ratingValue(it.rating) },
        )
    }

    private fun demoPersonal(history: List<WatchHistoryItem>): HomePersonalSnapshot {
        val movies = FakeDataProvider.movies.map { it.toDemoCatalogMovie() }
        val channelIds = HomeContentRules.recentChannelIds(history, RECENT_CHANNEL_LIMIT)
        return HomePersonalSnapshot(
            categoryRails = movies
                .groupBy { it.categoryName.orEmpty() }
                .filterKeys { it.isNotBlank() }
                .map { (name, items) -> HomeCategoryRail(name, items) }
                .take(CATEGORY_RAIL_COUNT),
            recentChannels = channelIds.mapNotNull { id ->
                FakeDataProvider.channels.find { it.id == id }?.toDemoCatalogChannel()
            },
        )
    }

    private suspend fun <T> safely(section: String, fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Home section '$section' failed to load: ${error.safeSummary()}")
            fallback
        }

    private companion object {
        const val TAG = "HomeContent"
        /** Fetch more than a rail shows: de-duplication and parental filters remove some. */
        const val RAIL_FETCH_LIMIT = 30
        const val NEXT_EPISODE_LIMIT = 8
        const val RECENT_CHANNEL_LIMIT = 12
        const val AFFINITY_HISTORY_LIMIT = 50
        const val CATEGORY_FALLBACK_LIMIT = 6
        const val CATEGORY_RAIL_COUNT = 3
    }
}
