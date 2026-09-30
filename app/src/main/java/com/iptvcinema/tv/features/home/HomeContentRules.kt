package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.player.WatchHistoryResumePolicy

/** Pure selection rules behind the Home sections, kept free of Android so they are unit tested. */
object HomeContentRules {
    const val HERO_COUNT = 5
    const val HERO_RECENCY_POOL = 15
    const val MIN_TOP_RATING = 6.0

    private val leadingNumber = Regex("""^\d+(\.\d+)?""")

    /** The newest movies with artwork, reordered so the best rated lead. Ties keep recency order. */
    fun pickHero(newestWithArtwork: List<CatalogMovie>): List<CatalogMovie> =
        newestWithArtwork
            .take(HERO_RECENCY_POOL)
            .sortedByDescending { ratingValue(it.rating) }
            .take(HERO_COUNT)

    fun ratingValue(rating: String?): Double =
        rating?.trim()?.let { leadingNumber.find(it)?.value?.toDoubleOrNull() } ?: 0.0

    /**
     * Categories the viewer watches most, most-watched first. Ties keep the order they were
     * first seen. Remaining slots are filled from [fallback] (the provider's largest categories).
     */
    fun rankCategories(
        watchedCategories: List<String?>,
        fallback: List<String>,
        limit: Int,
        isBlocked: (String) -> Boolean,
    ): List<String> {
        val watched = watchedCategories
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
        return (watched + fallback)
            .distinct()
            .filterNot(isBlocked)
            .take(limit)
    }

    /**
     * Series whose most recent episode was watched to the end, newest first. A series whose
     * latest episode is still in progress belongs to Continue Watching instead.
     */
    fun nextEpisodeCandidates(history: List<WatchHistoryItem>, limit: Int): List<WatchHistoryItem> =
        history
            .asSequence()
            .filter { it.contentType == WatchHistoryContentType.EPISODE && !it.seriesId.isNullOrBlank() }
            .groupBy { it.seriesId }
            .values
            .map { episodes -> episodes.maxBy { it.lastWatchedAt } }
            .filter { WatchHistoryResumePolicy.isNearEnd(it.positionMs, it.durationMs) }
            .sortedByDescending { it.lastWatchedAt }
            .take(limit)

    fun recentChannelIds(history: List<WatchHistoryItem>, limit: Int): List<String> =
        history
            .filter { it.contentType == WatchHistoryContentType.CHANNEL }
            .sortedByDescending { it.lastWatchedAt }
            .map { it.contentId }
            .distinct()
            .take(limit)

    fun lastWatchedMovie(history: List<WatchHistoryItem>): WatchHistoryItem? =
        history
            .filter { it.contentType == WatchHistoryContentType.MOVIE }
            .maxByOrNull { it.lastWatchedAt }

    /** Series the viewer watched, most recent first. */
    fun watchedSeriesIds(history: List<WatchHistoryItem>): List<String> =
        history
            .filter { it.contentType == WatchHistoryContentType.EPISODE && !it.seriesId.isNullOrBlank() }
            .sortedByDescending { it.lastWatchedAt }
            .mapNotNull { it.seriesId }
            .distinct()

    fun lastWatchedSeriesId(history: List<WatchHistoryItem>): String? = watchedSeriesIds(history).firstOrNull()
}
