package com.iptvcinema.tv.core.data.repository

import android.util.Log
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.mapper.CatalogEntityMapper.toDomain
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.database.dao.CategorySummary
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.home.BrowseCategory
import com.iptvcinema.tv.core.model.home.HomeCategoryRail
import com.iptvcinema.tv.core.model.home.MoviesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.MoviesPersonalSnapshot
import com.iptvcinema.tv.core.model.home.RelatedItem
import com.iptvcinema.tv.core.model.home.RelatedSnapshot
import com.iptvcinema.tv.core.model.home.SeriesBecauseYouWatched
import com.iptvcinema.tv.core.model.home.SeriesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.SeriesCategoryRail
import com.iptvcinema.tv.core.model.home.SeriesPersonalSnapshot
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.features.home.HomeContentRules
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Loads the Movies and Series tabs with one-shot local queries, like [HomeContentRepository]
 * does for Home. A failing section is logged and left out; it never fails the whole tab.
 */
@Singleton
class BrowseContentRepository @Inject constructor(
    private val catalogDaoFacade: CatalogDaoFacade,
    private val catalogRepository: CatalogRepository,
    private val homeContentRepository: HomeContentRepository,
) {
    suspend fun loadMoviesCatalog(sourceId: String?, isDemoMode: Boolean): MoviesCatalogSnapshot = when {
        isDemoMode -> demoMoviesCatalog()
        sourceId == null -> MoviesCatalogSnapshot()
        else -> {
            val movies = catalogDaoFacade.movies
            MoviesCatalogSnapshot(
                newest = safely("newestMovies", emptyList()) {
                    movies.getNewest(sourceId, RAIL_FETCH_LIMIT).map { it.toDomain() }
                },
                topRated = safely("topRatedMovies", emptyList()) {
                    movies.getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT).map { it.toDomain() }
                },
                categories = safely("movieCategories", emptyList()) {
                    movies.getCategorySummaries(sourceId, CATEGORY_TILE_LIMIT).map { it.toBrowseCategory() }
                },
            )
        }
    }

    suspend fun loadMoviesPersonal(
        sourceId: String?,
        isDemoMode: Boolean,
        history: List<WatchHistoryItem>,
        isCategoryBlocked: (String) -> Boolean,
    ): MoviesPersonalSnapshot {
        if (isDemoMode) return demoMoviesPersonal()
        if (sourceId == null) return MoviesPersonalSnapshot()
        val watchedMovieIds = history
            .filter { it.contentType == WatchHistoryContentType.MOVIE }
            .mapTo(mutableSetOf()) { it.contentId }
        val because = safely("becauseMovies", null) {
            homeContentRepository.loadBecauseYouWatched(sourceId, history, watchedMovieIds)
        }
        return MoviesPersonalSnapshot(
            becauseYouWatched = because?.first,
            categoryRails = safely("movieCategoryRails", emptyList()) {
                homeContentRepository.loadCategoryRails(
                    sourceId = sourceId,
                    watchedMovieIds = watchedMovieIds.toList(),
                    excludedCategory = because?.second,
                    isCategoryBlocked = isCategoryBlocked,
                    railCount = CATEGORY_RAIL_COUNT,
                )
            },
            watchedMovieIds = watchedMovieIds,
        )
    }

    suspend fun loadSeriesCatalog(sourceId: String?, isDemoMode: Boolean): SeriesCatalogSnapshot = when {
        isDemoMode -> demoSeriesCatalog()
        sourceId == null -> SeriesCatalogSnapshot()
        else -> {
            val series = catalogDaoFacade.series
            SeriesCatalogSnapshot(
                latest = safely("latestSeries", emptyList()) {
                    series.getLatest(sourceId, RAIL_FETCH_LIMIT).map { it.toDomain() }
                },
                topRated = safely("topRatedSeries", emptyList()) {
                    series.getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT).map { it.toDomain() }
                },
                categories = safely("seriesCategories", emptyList()) {
                    series.getCategorySummaries(sourceId, CATEGORY_TILE_LIMIT).map { it.toBrowseCategory() }
                },
            )
        }
    }

    suspend fun loadSeriesPersonal(
        sourceId: String?,
        isDemoMode: Boolean,
        history: List<WatchHistoryItem>,
        isCategoryBlocked: (String) -> Boolean,
    ): SeriesPersonalSnapshot {
        if (isDemoMode) return demoSeriesPersonal()
        if (sourceId == null) return SeriesPersonalSnapshot()
        val watchedSeriesIds = HomeContentRules.watchedSeriesIds(history)
        val because = safely("becauseSeries", null) { loadBecauseYouWatchedSeries(sourceId, history, watchedSeriesIds) }
        return SeriesPersonalSnapshot(
            nextEpisodes = safely("nextEpisodes", emptyList()) {
                homeContentRepository.loadNextEpisodes(sourceId, history)
            },
            becauseYouWatched = because?.first,
            categoryRails = safely("seriesCategoryRails", emptyList()) {
                loadSeriesCategoryRails(sourceId, watchedSeriesIds, because?.second, isCategoryBlocked)
            },
            watchedSeriesIds = watchedSeriesIds.toSet(),
        )
    }

    /** The best rated movies and series, suggested on Search before the viewer types. */
    suspend fun loadSearchSuggestions(sourceId: String?, isDemoMode: Boolean): Pair<List<CatalogMovie>, List<CatalogSeries>> {
        if (isDemoMode) return demoMoviesCatalog().topRated to demoSeriesCatalog().topRated
        sourceId ?: return emptyList<CatalogMovie>() to emptyList()
        val movies = safely("suggestedMovies", emptyList()) {
            catalogDaoFacade.movies.getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT).map { it.toDomain() }
        }
        val series = safely("suggestedSeries", emptyList()) {
            catalogDaoFacade.series.getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT).map { it.toDomain() }
        }
        return movies to series
    }

    /** Titles like one movie: its category first, then top rated and new movies. */
    suspend fun loadMovieRelated(sourceId: String?, movieId: String, isDemoMode: Boolean): RelatedSnapshot? {
        if (isDemoMode) {
            val movies = FakeDataProvider.movies.map { it.toDemoCatalogMovie() }
            val anchor = movies.find { it.id == movieId } ?: return null
            return RelatedSnapshot(
                anchorId = anchor.id,
                anchorTitle = anchor.title,
                anchorCategory = anchor.categoryName,
                similar = movies.filter { it.categoryName == anchor.categoryName }.map { it.toRelatedItem() },
                topRated = movies.sortedByDescending { HomeContentRules.ratingValue(it.rating) }.map { it.toRelatedItem() },
                newest = movies.map { it.toRelatedItem() },
            )
        }
        sourceId ?: return null
        val anchor = catalogRepository.getMovie(sourceId, movieId) ?: return null
        val movies = catalogDaoFacade.movies
        return RelatedSnapshot(
            anchorId = anchor.id,
            anchorTitle = anchor.title,
            anchorCategory = anchor.categoryName,
            similar = safely("similarMovies", emptyList()) {
                anchor.categoryName?.takeIf { it.isNotBlank() }
                    ?.let { movies.getByCategoryNameLimited(sourceId, it, RAIL_FETCH_LIMIT) }
                    .orEmpty()
                    .map { it.toDomain().toRelatedItem() }
            },
            topRated = safely("topRatedMovies", emptyList()) {
                movies.getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT).map { it.toDomain().toRelatedItem() }
            },
            newest = safely("newestMovies", emptyList()) {
                movies.getNewest(sourceId, RAIL_FETCH_LIMIT).map { it.toDomain().toRelatedItem() }
            },
        )
    }

    /** Titles like one series: its category first, then top rated and latest series. */
    suspend fun loadSeriesRelated(sourceId: String?, seriesId: String, isDemoMode: Boolean): RelatedSnapshot? {
        if (isDemoMode) {
            val series = FakeDataProvider.seriesList.map { it.toDemoCatalogSeries() }
            val anchor = series.find { it.id == seriesId } ?: return null
            return RelatedSnapshot(
                anchorId = anchor.id,
                anchorTitle = anchor.title,
                anchorCategory = anchor.categoryName,
                similar = series.filter { it.categoryName == anchor.categoryName }.map { it.toRelatedItem() },
                topRated = series.sortedByDescending { HomeContentRules.ratingValue(it.rating) }.map { it.toRelatedItem() },
                newest = series.sortedByDescending { it.year ?: 0 }.map { it.toRelatedItem() },
            )
        }
        sourceId ?: return null
        val anchor = catalogRepository.getSeries(sourceId, seriesId) ?: return null
        val seriesDao = catalogDaoFacade.series
        return RelatedSnapshot(
            anchorId = anchor.id,
            anchorTitle = anchor.title,
            anchorCategory = anchor.categoryName,
            similar = safely("similarSeries", emptyList()) {
                anchor.categoryName?.takeIf { it.isNotBlank() }
                    ?.let { seriesDao.getByCategoryNameLimited(sourceId, it, RAIL_FETCH_LIMIT) }
                    .orEmpty()
                    .map { it.toDomain().toRelatedItem() }
            },
            topRated = safely("topRatedSeries", emptyList()) {
                seriesDao.getTopRated(sourceId, HomeContentRules.MIN_TOP_RATING, RAIL_FETCH_LIMIT).map { it.toDomain().toRelatedItem() }
            },
            newest = safely("latestSeries", emptyList()) {
                seriesDao.getLatest(sourceId, RAIL_FETCH_LIMIT).map { it.toDomain().toRelatedItem() }
            },
        )
    }

    private fun CatalogMovie.toRelatedItem() = RelatedItem(toHomeContentCard(), categoryName, rating)

    private fun CatalogSeries.toRelatedItem() = RelatedItem(toHomeContentCard(), categoryName, rating)

    /** Series in the same category as the series watched last, plus that category's name. */
    private suspend fun loadBecauseYouWatchedSeries(
        sourceId: String,
        history: List<WatchHistoryItem>,
        watchedSeriesIds: List<String>,
    ): Pair<SeriesBecauseYouWatched, String?>? {
        val seriesId = HomeContentRules.lastWatchedSeriesId(history) ?: return null
        val anchor = catalogRepository.getSeries(sourceId, seriesId) ?: return null
        val categoryId = anchor.categoryId?.takeIf { it.isNotBlank() } ?: return null
        val watched = watchedSeriesIds.toSet()
        val related = catalogRepository
            .getRelatedSeries(sourceId, categoryId, anchor.id, RAIL_FETCH_LIMIT)
            .filterNot { it.id in watched }
        return SeriesBecauseYouWatched(anchorTitle = anchor.title, series = related) to anchor.categoryName
    }

    private suspend fun loadSeriesCategoryRails(
        sourceId: String,
        watchedSeriesIds: List<String>,
        excludedCategory: String?,
        isCategoryBlocked: (String) -> Boolean,
    ): List<SeriesCategoryRail> {
        val seriesDao = catalogDaoFacade.series
        val watchedCategories = watchedSeriesIds
            .take(AFFINITY_HISTORY_LIMIT)
            .takeIf { it.isNotEmpty() }
            ?.let { ids -> seriesDao.getByIds(sourceId, ids).map { it.categoryName } }
            .orEmpty()
        val fallback = seriesDao.getCategorySummaries(sourceId, CATEGORY_RAIL_COUNT * 2).map { it.name }
        val categories = HomeContentRules.rankCategories(
            watchedCategories = watchedCategories,
            fallback = fallback,
            limit = CATEGORY_RAIL_COUNT,
            isBlocked = { it == excludedCategory || isCategoryBlocked(it) },
        )
        return categories.map { name ->
            SeriesCategoryRail(
                categoryName = name,
                series = seriesDao.getByCategoryNameLimited(sourceId, name, RAIL_FETCH_LIMIT).map { it.toDomain() },
            )
        }
    }

    private fun demoMoviesCatalog(): MoviesCatalogSnapshot {
        val movies = FakeDataProvider.movies.map { it.toDemoCatalogMovie() }
        return MoviesCatalogSnapshot(
            newest = movies,
            topRated = movies.sortedByDescending { HomeContentRules.ratingValue(it.rating) },
            categories = movies.demoCategories({ it.categoryName }, { it.backdropUrl }, { it.posterUrl }),
        )
    }

    private fun demoMoviesPersonal(): MoviesPersonalSnapshot {
        val movies = FakeDataProvider.movies.map { it.toDemoCatalogMovie() }
        return MoviesPersonalSnapshot(
            categoryRails = movies
                .groupBy { it.categoryName.orEmpty() }
                .filterKeys { it.isNotBlank() }
                .map { (name, items) -> HomeCategoryRail(name, items) }
                .take(CATEGORY_RAIL_COUNT),
        )
    }

    private fun demoSeriesCatalog(): SeriesCatalogSnapshot {
        val series = FakeDataProvider.seriesList.map { it.toDemoCatalogSeries() }
        return SeriesCatalogSnapshot(
            latest = series.sortedByDescending { it.year ?: 0 },
            topRated = series.sortedByDescending { HomeContentRules.ratingValue(it.rating) },
            categories = series.demoCategories({ it.categoryName }, { it.backdropUrl }, { it.posterUrl }),
        )
    }

    private fun demoSeriesPersonal(): SeriesPersonalSnapshot {
        val series = FakeDataProvider.seriesList.map { it.toDemoCatalogSeries() }
        return SeriesPersonalSnapshot(
            categoryRails = series
                .groupBy { it.categoryName.orEmpty() }
                .filterKeys { it.isNotBlank() }
                .map { (name, items) -> SeriesCategoryRail(name, items) }
                .take(CATEGORY_RAIL_COUNT),
        )
    }

    private fun <T> List<T>.demoCategories(
        name: (T) -> String?,
        backdrop: (T) -> String?,
        poster: (T) -> String?,
    ): List<BrowseCategory> =
        groupBy { name(it).orEmpty() }
            .filterKeys { it.isNotBlank() }
            .map { (category, items) ->
                BrowseCategory(
                    name = category,
                    itemCount = items.size,
                    backdropUrl = items.firstNotNullOfOrNull(backdrop),
                    posterUrl = items.firstNotNullOfOrNull(poster),
                )
            }
            .sortedByDescending { it.itemCount }

    private suspend fun <T> safely(section: String, fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Browse section '$section' failed to load: ${error.safeSummary()}")
            fallback
        }

    private fun CategorySummary.toBrowseCategory() = BrowseCategory(
        name = name,
        itemCount = itemCount,
        backdropUrl = backdropUrl,
        posterUrl = posterUrl,
    )

    private companion object {
        const val TAG = "BrowseContent"
        /** Fetch more than a rail shows: de-duplication and parental filters remove some. */
        const val RAIL_FETCH_LIMIT = 30
        const val CATEGORY_RAIL_COUNT = 6
        const val CATEGORY_TILE_LIMIT = 80
        const val AFFINITY_HISTORY_LIMIT = 50
    }
}
