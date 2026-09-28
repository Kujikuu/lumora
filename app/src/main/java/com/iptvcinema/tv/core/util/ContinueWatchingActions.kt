package com.iptvcinema.tv.core.util

import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.home.HomeContentCard

suspend fun WatchHistoryRepository.removeContinueWatching(
    profileId: String,
    card: HomeContentCard,
) {
    val seriesId = card.seriesId?.takeIf { it.isNotBlank() }
    when {
        // Continue Watching shows one card per series (its latest episode). Removing just that
        // episode would bring the previous one back, so the whole series goes.
        card.contentType == "episode" && seriesId != null -> removeSeries(profileId, seriesId)
        card.contentType == "episode" -> remove(profileId, card.contentId, WatchHistoryContentType.EPISODE)
        card.contentType == "movie" -> remove(profileId, card.contentId, WatchHistoryContentType.MOVIE)
        else -> return
    }
    invalidate()
}

/** Key shared by a card and anything else shown for the same title (series or movie). */
fun HomeContentCard.continueWatchingKey(): String =
    seriesId?.takeIf { contentType == "episode" && it.isNotBlank() }?.let { "series:$it" } ?: "$contentType:$contentId"
