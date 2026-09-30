package com.iptvcinema.tv.core.tvhome

import android.util.Log
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.parental.ParentalPlaybackGuard
import com.iptvcinema.tv.core.util.safeSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

/**
 * Keeps the TV home screen's Watch Next row in step with the current profile's Continue Watching.
 * Progress saves, hides, profile switches and the Continue Watching setting all flow through
 * [WatchHistoryRepository.observeContinueWatching], so there is nothing else to hook.
 */
@Singleton
class WatchNextSync @Inject constructor(
    appSessionRepository: AppSessionRepository,
    userSettingsRepository: UserSettingsRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val catalogRepository: CatalogRepository,
    private val parentalPlaybackGuard: ParentalPlaybackGuard,
    private val publisher: WatchNextPublisher,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    init {
        observe(appSessionRepository, userSettingsRepository).launchIn(applicationScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observe(
        appSessionRepository: AppSessionRepository,
        userSettingsRepository: UserSettingsRepository,
    ) = combine(
        appSessionRepository.sessionState,
        userSettingsRepository.observeSettings().map { it?.continueWatchingEnabled ?: true },
    ) { session, enabled -> session to enabled }
        .distinctUntilChanged()
        .flatMapLatest { (session, enabled) ->
            val profileId = session.currentProfileId
            if (profileId == null || !enabled || session.isDemoMode) {
                flowOf(session to emptyList())
            } else {
                watchHistoryRepository.observeContinueWatching(profileId, LIMIT)
                    .map { items -> session to items }
                    .catch { error ->
                        Log.w(TAG, "Continue Watching unavailable: ${error.safeSummary()}")
                        emit(session to emptyList())
                    }
            }
        }
        // Saves arrive every few seconds during playback; settle before touching the provider.
        .debounce(DEBOUNCE_MS)
        .mapLatest { (session, items) ->
            val profileId = session.currentProfileId
            if (profileId == null || items.isEmpty()) {
                publisher.clear()
            } else {
                publisher.publish(buildEntries(session, profileId, items))
            }
        }
        .catch { error -> Log.w(TAG, "Watch Next sync stopped: ${error.safeSummary()}") }

    private suspend fun buildEntries(
        session: AppSessionState,
        profileId: String,
        items: List<WatchHistoryItem>,
    ): List<WatchNextEntry> = items.mapNotNull { item ->
        try {
            if (parentalPlaybackGuard.isHistoryItemBlocked(session, item)) return@mapNotNull null
            val display = catalogRepository.resolveWatchHistoryCardDisplay(session.currentSourceId, item)
            WatchNextEntryMapper.from(item, display, episodeNumbers(session, item), profileId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Skipped a Watch Next item: ${error.safeSummary()}")
            null
        }
    }

    private suspend fun episodeNumbers(session: AppSessionState, item: WatchHistoryItem): EpisodeNumbers? {
        if (item.contentType != WatchHistoryContentType.EPISODE) return null
        val sourceId = item.sourceId?.takeIf { it.isNotBlank() } ?: session.currentSourceId ?: return null
        val episode = catalogRepository.getEpisode(sourceId, item.contentId) ?: return null
        return EpisodeNumbers(episode.seasonNumber, episode.episodeNumber, episode.title)
    }

    private companion object {
        const val TAG = "WatchNext"
        const val LIMIT = 10
        const val DEBOUNCE_MS = 2_000L
    }
}
