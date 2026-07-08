package com.iptvcinema.tv.core.util

import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.home.HomeContentCard

suspend fun WatchHistoryRepository.removeContinueWatching(
    profileId: String,
    card: HomeContentCard,
) {
    val contentType = when (card.contentType) {
        "movie" -> WatchHistoryContentType.MOVIE
        "episode" -> WatchHistoryContentType.EPISODE
        else -> return
    }
    remove(profileId, card.contentId, contentType)
    invalidate()
}
