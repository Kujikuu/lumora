package com.iptvcinema.tv.features.mylist

import android.util.Log
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.mapper.CatalogEntityMapper.toDomain
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.HomeNextEpisode
import com.iptvcinema.tv.core.util.safeSummary
import com.iptvcinema.tv.features.home.HomeUiMapper.toHomeContentCard
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** A saved title with what parental controls check. */
data class MyListEntry(
    val favorite: FavoriteItem,
    val card: HomeContentCard,
    val categoryName: String? = null,
    val rating: String? = null,
)

/** My List, resolved against the local catalog and grouped by type in the order saved. */
data class MyListSaved(
    val movies: List<MyListEntry> = emptyList(),
    val series: List<MyListEntry> = emptyList(),
    val episodes: List<MyListEntry> = emptyList(),
    val channels: List<MyListEntry> = emptyList(),
) {
    val all: List<MyListEntry> get() = movies + series + episodes + channels
}

/**
 * Turns favorites into rich cards (backdrop, plot, rating, what is on now) from the local
 * catalog. Only local data is read; a favorite missing from the catalog keeps its saved title and
 * poster, so nothing the viewer saved disappears.
 */
@Singleton
class MyListContentLoader @Inject constructor(
    private val catalogDaoFacade: CatalogDaoFacade,
    private val catalogRepository: CatalogRepository,
) {
    suspend fun load(favorites: List<FavoriteItem>, currentSourceId: String?, isDemoMode: Boolean): MyListSaved {
        val byType = favorites.distinctBy { it.contentType to it.contentId }.groupBy { it.contentType }
        return MyListSaved(
            movies = byType[FavoriteContentType.MOVIE].orEmpty()
                .resolve(currentSourceId, isDemoMode) { sourceId, items -> movies(sourceId, items) },
            series = byType[FavoriteContentType.SERIES].orEmpty()
                .resolve(currentSourceId, isDemoMode) { sourceId, items -> series(sourceId, items) },
            episodes = byType[FavoriteContentType.EPISODE].orEmpty()
                .resolve(currentSourceId, isDemoMode) { sourceId, items -> episodes(sourceId, items) },
            channels = byType[FavoriteContentType.CHANNEL].orEmpty()
                .resolve(currentSourceId, isDemoMode) { sourceId, items -> channels(sourceId, items) },
        )
    }

    /** Resolves per source (a favorite remembers the playlist it came from), keeping saved order. */
    private suspend fun List<FavoriteItem>.resolve(
        currentSourceId: String?,
        isDemoMode: Boolean,
        lookup: suspend (sourceId: String, items: List<FavoriteItem>) -> Map<String, MyListEntry>,
    ): List<MyListEntry> {
        if (isEmpty()) return emptyList()
        if (isDemoMode) return map(::demoEntry)
        val resolved = mutableMapOf<String, MyListEntry>()
        groupBy { it.sourceId?.takeIf(String::isNotBlank) ?: currentSourceId }.forEach { (sourceId, items) ->
            if (sourceId == null) return@forEach
            try {
                resolved += lookup(sourceId, items)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Couldn't resolve saved titles: ${error.safeSummary()}")
            }
        }
        return map { favorite -> resolved[favorite.contentId] ?: fallbackEntry(favorite) }
    }

    private suspend fun movies(sourceId: String, items: List<FavoriteItem>): Map<String, MyListEntry> {
        val byId = items.associateBy { it.contentId }
        return catalogDaoFacade.movies.getByIds(sourceId, byId.keys.toList()).mapNotNull { entity ->
            val movie = entity.toDomain()
            val favorite = byId[movie.id] ?: return@mapNotNull null
            movie.id to MyListEntry(favorite, movie.toHomeContentCard(isFavorite = true), movie.categoryName, movie.rating)
        }.toMap()
    }

    private suspend fun series(sourceId: String, items: List<FavoriteItem>): Map<String, MyListEntry> {
        val byId = items.associateBy { it.contentId }
        return catalogDaoFacade.series.getByIds(sourceId, byId.keys.toList()).mapNotNull { entity ->
            val series = entity.toDomain()
            val favorite = byId[series.id] ?: return@mapNotNull null
            series.id to MyListEntry(favorite, series.toHomeContentCard(isFavorite = true), series.categoryName, series.rating)
        }.toMap()
    }

    private suspend fun episodes(sourceId: String, items: List<FavoriteItem>): Map<String, MyListEntry> =
        items.mapNotNull { favorite ->
            val episode = catalogRepository.getEpisode(sourceId, favorite.contentId) ?: return@mapNotNull null
            val series = catalogRepository.getSeries(sourceId, episode.seriesId)
            val card = HomeNextEpisode(series, episode).toHomeContentCard(isFavorite = true)
            favorite.contentId to MyListEntry(favorite, card, series?.categoryName, series?.rating)
        }.toMap()

    private suspend fun channels(sourceId: String, items: List<FavoriteItem>): Map<String, MyListEntry> {
        val byId = items.associateBy { it.contentId }
        val ids = byId.keys.toList()
        val nowPlaying = runCatchingNonCancellation { catalogRepository.getCurrentProgramsForChannels(sourceId, ids) }
            .orEmpty()
        return catalogDaoFacade.channels.getByIds(sourceId, ids).mapNotNull { entity ->
            val channel = entity.toDomain()
            val favorite = byId[channel.id] ?: return@mapNotNull null
            val card = channel.toHomeContentCard(isFavorite = true).let { card ->
                nowPlaying[channel.id]?.title?.takeIf { it.isNotBlank() }?.let { card.copy(subtitle = it) } ?: card
            }
            channel.id to MyListEntry(favorite, card, channel.categoryName)
        }.toMap()
    }

    private fun demoEntry(favorite: FavoriteItem): MyListEntry = when (favorite.contentType) {
        FavoriteContentType.CHANNEL -> FakeDataProvider.channels.find { it.id == favorite.contentId }
            ?.let { channel ->
                MyListEntry(
                    favorite = favorite,
                    card = HomeContentCard(
                        contentId = channel.id,
                        contentType = "channel",
                        title = channel.name,
                        subtitle = channel.currentProgram,
                        imageUrl = channel.logoUrl,
                        isFavorite = true,
                    ),
                    categoryName = channel.category,
                )
            }
        FavoriteContentType.MOVIE -> FakeDataProvider.movies.find { it.id == favorite.contentId }
            ?.let { movie ->
                MyListEntry(
                    favorite = favorite,
                    card = HomeContentCard(
                        contentId = movie.id,
                        contentType = "movie",
                        title = movie.title,
                        imageUrl = movie.imageUrl,
                        backdropUrl = movie.backdropUrl ?: movie.imageUrl,
                        year = movie.year.takeIf { it > 0 }?.toString(),
                        genres = movie.genres,
                        plot = movie.plot.takeIf { it.isNotBlank() },
                        rating = movie.rating,
                        isFavorite = true,
                    ),
                    categoryName = movie.genres.firstOrNull(),
                    rating = movie.rating,
                )
            }
        else -> null
    } ?: fallbackEntry(favorite)

    private fun fallbackEntry(favorite: FavoriteItem) = MyListEntry(
        favorite = favorite,
        card = HomeContentCard(
            contentId = favorite.contentId,
            contentType = favorite.contentType.cardType(),
            title = favorite.title,
            imageUrl = favorite.posterUrl,
            isFavorite = true,
        ),
    )

    private suspend fun <T> runCatchingNonCancellation(block: suspend () -> T): T? =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Couldn't load what is on now: ${error.safeSummary()}")
            null
        }

    private companion object {
        const val TAG = "MyListContent"
    }
}

internal fun FavoriteContentType.cardType(): String = when (this) {
    FavoriteContentType.MOVIE -> "movie"
    FavoriteContentType.SERIES -> "series"
    FavoriteContentType.EPISODE -> "episode"
    FavoriteContentType.CHANNEL -> "channel"
}
