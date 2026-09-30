package com.iptvcinema.tv.core.parental

import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toMovieItem
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.player.PlaybackRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalPlaybackGuard @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
) {
    suspend fun isPlaybackBlocked(session: AppSessionState, request: PlaybackRequest): Boolean =
        isContentBlocked(session, request.contentType, request.contentId, request.seriesId, request.sourceId)

    /** Same check for a history item, e.g. before showing it outside the app on the TV home screen. */
    suspend fun isHistoryItemBlocked(session: AppSessionState, item: WatchHistoryItem): Boolean =
        isContentBlocked(session, item.contentType, item.contentId, item.seriesId, item.sourceId)

    private suspend fun isContentBlocked(
        session: AppSessionState,
        contentType: WatchHistoryContentType,
        contentId: String,
        seriesId: String?,
        itemSourceId: String?,
    ): Boolean {
        if (session.isDemoMode) return false
        val profileId = session.currentProfileId ?: return false
        val sourceId = itemSourceId ?: session.currentSourceId ?: return false
        val controls = parentalControlsRepository.getControls(profileId) ?: return false
        return when (contentType) {
            WatchHistoryContentType.MOVIE -> {
                val movie = catalogRepository.getMovie(sourceId, contentId)?.toMovieItem()
                    ?: return false
                parentalGate.isContentBlocked(
                    // Blocks are set per provider category; the genre is only a fallback.
                    categoryName = movie.categoryName ?: movie.genres.firstOrNull(),
                    contentRating = movie.rating,
                    controls = controls,
                )
            }
            WatchHistoryContentType.EPISODE -> {
                val series = catalogRepository.getSeries(sourceId, seriesId ?: return false) ?: return false
                parentalGate.isContentBlocked(
                    categoryName = series.categoryName,
                    contentRating = series.rating,
                    controls = controls,
                )
            }
            WatchHistoryContentType.CHANNEL -> {
                val channel = catalogRepository.getChannel(sourceId, contentId) ?: return false
                parentalGate.isContentBlocked(
                    categoryName = channel.categoryName,
                    contentRating = null,
                    controls = controls,
                )
            }
        }
    }

    /** Which channels may play for the current profile, read once so zapping can skip the rest. */
    suspend fun channelFilter(session: AppSessionState): (CatalogChannel) -> Boolean {
        if (session.isDemoMode) return { true }
        val profileId = session.currentProfileId ?: return { true }
        val controls = parentalControlsRepository.getControls(profileId) ?: return { true }
        return { channel -> !parentalGate.isContentBlocked(channel.categoryName, null, controls) }
    }
}
