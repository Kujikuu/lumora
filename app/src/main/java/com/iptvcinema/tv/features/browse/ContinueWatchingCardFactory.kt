package com.iptvcinema.tv.features.browse

import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.home.HomeCardAction
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.RemainingWatchTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Builds Continue Watching cards from watch history, the same on every screen. */
@Singleton
class ContinueWatchingCardFactory @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val appStrings: AppStrings,
) {
    /** A card for a movie or episode in progress; channels have no Continue Watching card. */
    suspend fun create(
        item: WatchHistoryItem,
        sourceId: String?,
        isDemoMode: Boolean,
        favorites: List<FavoriteItem>,
    ): HomeContentCard? {
        val (contentType, favoriteType) = when (item.contentType) {
            WatchHistoryContentType.MOVIE -> "movie" to FavoriteContentType.MOVIE
            WatchHistoryContentType.EPISODE -> "episode" to FavoriteContentType.EPISODE
            WatchHistoryContentType.CHANNEL -> return null
        }
        val display = catalogRepository.resolveWatchHistoryCardDisplay(
            sourceId = sourceId,
            item = item,
            isDemoMode = isDemoMode,
        )
        return HomeContentCard(
            contentId = item.contentId,
            contentType = contentType,
            seriesId = item.seriesId,
            title = display.title,
            subtitle = display.subtitle,
            imageUrl = display.posterUrl,
            backdropUrl = display.backdropUrl,
            progress = item.durationMs?.takeIf { it > 0 }?.let { (item.positionMs.toFloat() / it).coerceIn(0f, 1f) },
            remainingTimeLabel = RemainingWatchTimeFormatter.formatFromWatchHistory(item, appStrings),
            isFavorite = favorites.any { it.contentId == item.contentId && it.contentType == favoriteType },
            primaryAction = HomeCardAction.ContinueWatching,
        )
    }
}
