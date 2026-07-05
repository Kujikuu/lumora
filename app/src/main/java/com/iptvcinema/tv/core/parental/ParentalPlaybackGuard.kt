package com.iptvcinema.tv.core.parental

import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toMovieItem
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.player.PlaybackRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalPlaybackGuard @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
) {
    suspend fun isPlaybackBlocked(session: AppSessionState, request: PlaybackRequest): Boolean {
        if (session.isDemoMode) return false
        val profileId = session.currentProfileId ?: return false
        val sourceId = request.sourceId ?: session.currentSourceId ?: return false
        val controls = parentalControlsRepository.getControls(profileId) ?: return false
        return when (request.contentType) {
            WatchHistoryContentType.MOVIE -> {
                val movie = catalogRepository.getMovie(sourceId, request.contentId)?.toMovieItem()
                    ?: return false
                parentalGate.isContentBlocked(
                    categoryName = movie.genres.firstOrNull(),
                    contentRating = movie.rating,
                    controls = controls,
                )
            }
            WatchHistoryContentType.EPISODE -> {
                val seriesId = request.seriesId ?: return false
                val series = catalogRepository.getSeries(sourceId, seriesId) ?: return false
                parentalGate.isContentBlocked(
                    categoryName = series.categoryName,
                    contentRating = series.rating,
                    controls = controls,
                )
            }
            WatchHistoryContentType.CHANNEL -> {
                val channel = catalogRepository.getChannel(sourceId, request.contentId) ?: return false
                parentalGate.isContentBlocked(
                    categoryName = channel.categoryName,
                    contentRating = null,
                    controls = controls,
                )
            }
        }
    }
}
