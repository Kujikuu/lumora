package com.iptvcinema.tv.core.tvhome

import android.content.Context
import android.util.Log
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.tvhome.launcher.LauncherContentProvider
import com.iptvcinema.tv.core.tvhome.launcher.LauncherRowsSnapshot
import com.iptvcinema.tv.core.util.safeSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update

/**
 * Keeps the home rows current in two places: the Android TV home screen (through the TV provider)
 * and the Lumora Home launcher (through a snapshot file served by [LauncherContentProvider]).
 * Favourites, watch history, profile switches, parental changes and catalog syncs all re-run it.
 */
@Singleton
class HomeChannelsSync @Inject constructor(
    @ApplicationContext private val context: Context,
    appSessionRepository: AppSessionRepository,
    private val favoritesRepository: FavoritesRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val catalogRepository: CatalogRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val rowsBuilder: HomeRowsBuilder,
    private val publisher: PreviewChannelPublisher,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    // Bumped by the launcher's refresh request; each bump re-subscribes the inputs and rebuilds.
    private val refreshRequests = MutableStateFlow(0)

    init {
        observe(appSessionRepository).launchIn(applicationScope)
    }

    /** Rebuilds the rows soon (after the usual debounce), for example when the launcher asks. */
    fun refreshNow() {
        refreshRequests.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observe(appSessionRepository: AppSessionRepository) =
        refreshRequests
            .flatMapLatest { appSessionRepository.sessionState.distinctUntilChanged() }
            .flatMapLatest { session ->
                val profileId = session.currentProfileId
                val sourceId = session.currentSourceId
                if (profileId == null || sourceId == null || session.isDemoMode) {
                    flowOf(Input(session, emptyList(), null))
                } else {
                    combine(
                        favoritesRepository.observeFavorites(profileId),
                        catalogRepository.observeSyncState().map { it?.lastSyncedAtEpochMs },
                        // A changed block list or rating limit must leave the launcher too.
                        parentalControlsRepository.observeControls(profileId),
                        watchHistoryRepository.observeContinueWatching(profileId, HomeRowsBuilder.ROW_LIMIT),
                        watchHistoryRepository.observeHistory(profileId, HISTORY_LIMIT),
                    ) { favorites, syncedAt, controls, continueWatching, history ->
                        Input(session, favorites, syncedAt, controls, continueWatching, history)
                    }
                        .catch { error ->
                            Log.w(TAG, "Home channel inputs unavailable: ${error.safeSummary()}")
                            emit(Input(session, emptyList(), null))
                        }
                }
            }
            .debounce(DEBOUNCE_MS)
            .mapLatest { input -> publish(input) }
            .catch { error -> Log.w(TAG, "Home channel sync stopped: ${error.safeSummary()}") }

    private suspend fun publish(input: Input) {
        val session = input.session
        val sourceId = session.currentSourceId
        if (session.currentProfileId == null || sourceId == null || session.isDemoMode) {
            publisher.clear()
            LauncherContentProvider.snapshotStore(context).clear()
        } else {
            val rows = rowsBuilder.build(session, sourceId, input.favorites, input.continueWatching, input.history)
            publisher.publish(rows.filter { it.key in HomeRowKeys.TV_PROVIDER_ROWS })
            LauncherContentProvider.snapshotStore(context)
                .write(LauncherRowsSnapshot(System.currentTimeMillis(), rows))
        }
        LauncherContentProvider.notifyRowsChanged(context)
    }

    private data class Input(
        val session: AppSessionState,
        val favorites: List<FavoriteItem>,
        val lastSyncedAtMs: Long?,
        val controls: ParentalControls? = null,
        val continueWatching: List<WatchHistoryItem> = emptyList(),
        val history: List<WatchHistoryItem> = emptyList(),
    )

    private companion object {
        const val TAG = "HomeChannels"
        const val HISTORY_LIMIT = 24
        const val DEBOUNCE_MS = 2_000L
    }
}
