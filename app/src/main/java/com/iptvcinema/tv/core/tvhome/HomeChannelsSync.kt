package com.iptvcinema.tv.core.tvhome

import android.util.Log
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.parental.ParentalPlaybackGuard
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.R
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
 * Keeps two rows on the Android TV home screen current: the profile's favourite channels and
 * the newest titles in the catalog. Favourites, profile switches and catalog syncs all re-run it.
 */
@Singleton
class HomeChannelsSync @Inject constructor(
    appSessionRepository: AppSessionRepository,
    private val favoritesRepository: FavoritesRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val catalogRepository: CatalogRepository,
    private val catalogDaoFacade: CatalogDaoFacade,
    private val parentalPlaybackGuard: ParentalPlaybackGuard,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val publisher: PreviewChannelPublisher,
    private val appStrings: AppStrings,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    init {
        observe(appSessionRepository).launchIn(applicationScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observe(appSessionRepository: AppSessionRepository) =
        appSessionRepository.sessionState
            .distinctUntilChanged()
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
                        watchHistoryRepository.observeContinueWatching(profileId, ROW_LIMIT),
                    ) { favorites, syncedAt, controls, history -> Input(session, favorites, syncedAt, controls, history) }
                        .catch { error ->
                            Log.w(TAG, "Home channel inputs unavailable: ${error.safeSummary()}")
                            emit(Input(session, emptyList(), null))
                        }
                }
            }
            .debounce(DEBOUNCE_MS)
            .mapLatest { input ->
                val sourceId = input.session.currentSourceId
                if (input.session.currentProfileId == null || sourceId == null || input.session.isDemoMode) {
                    publisher.clear()
                } else {
                    publisher.publish(buildSpecs(input.session, sourceId, input.favorites, input.history))
                }
            }
            .catch { error -> Log.w(TAG, "Home channel sync stopped: ${error.safeSummary()}") }

    private suspend fun buildSpecs(
        session: AppSessionState,
        sourceId: String,
        favorites: List<FavoriteItem>,
        history: List<WatchHistoryItem>,
    ): List<HomeChannelSpec> = buildList {
        // First row is the one the launcher shows without asking, like YouTube's own rows.
        val continueRow = safely("continue") { continueWatching(session, history) }.orEmpty()
        add(HomeChannelSpec(CONTINUE_KEY, appStrings.get(R.string.tv_channel_continue), continueRow))
        // Rows stay even when empty: deleting and recreating a channel loses the viewer's
        // ordering and asks the system's "add channel" question again.
        val favoriteRow = safely("favorites") { favoriteChannels(session, sourceId, favorites) }.orEmpty()
        add(HomeChannelSpec(FAVORITES_KEY, appStrings.get(R.string.tv_channel_favorites), favoriteRow))
        val newRow = safely("newest") { newestTitles(session, sourceId) }.orEmpty()
        add(HomeChannelSpec(NEW_KEY, appStrings.get(R.string.tv_channel_new), newRow))
    }

    private suspend fun continueWatching(
        session: AppSessionState,
        history: List<WatchHistoryItem>,
    ): List<HomeProgramEntry> = history.mapNotNull { item ->
        if (item.contentType == WatchHistoryContentType.CHANNEL) return@mapNotNull null
        if (parentalPlaybackGuard.isHistoryItemBlocked(session, item)) return@mapNotNull null
        val display = catalogRepository.resolveWatchHistoryCardDisplay(session.currentSourceId, item)
        val isEpisode = item.contentType == WatchHistoryContentType.EPISODE
        val seriesId = item.seriesId?.takeIf { it.isNotBlank() }
        HomeProgramEntry(
            key = if (isEpisode && seriesId != null) "series:$seriesId" else "movie:${item.contentId}",
            kind = if (isEpisode) HomeProgramKind.Series else HomeProgramKind.Movie,
            title = display.title,
            description = display.subtitle,
            posterUrl = display.backdropUrl?.takeIf { it.isNotBlank() } ?: display.posterUrl,
            isWidePoster = !display.backdropUrl.isNullOrBlank(),
            intentUri = PlaybackDeepLink(
                contentType = if (isEpisode) "episode" else "movie",
                contentId = item.contentId,
                seriesId = seriesId,
                profileId = session.currentProfileId,
            ).toUri(),
        )
    }.distinctBy { it.key }

    private suspend fun favoriteChannels(
        session: AppSessionState,
        sourceId: String,
        favorites: List<FavoriteItem>,
    ): List<HomeProgramEntry> {
        val allowed = parentalPlaybackGuard.channelFilter(session)
        return favorites
            .filter { it.contentType == FavoriteContentType.CHANNEL }
            .take(ROW_LIMIT)
            .mapNotNull { favorite ->
                val channel = catalogRepository.getChannel(sourceId, favorite.contentId) ?: return@mapNotNull null
                if (!allowed(channel)) return@mapNotNull null
                HomeProgramEntry(
                    key = "live:${channel.id}",
                    kind = HomeProgramKind.Channel,
                    title = channel.name,
                    description = channel.categoryName,
                    posterUrl = channel.logoUrl,
                    isWidePoster = true,
                    intentUri = PlaybackDeepLink("live", channel.id, profileId = session.currentProfileId).toUri(),
                )
            }
    }

    private suspend fun newestTitles(session: AppSessionState, sourceId: String): List<HomeProgramEntry> {
        val allowed = parentalPlaybackGuard.catalogFilter(session)
        val movies = catalogDaoFacade.movies.getNewest(sourceId, ROW_LIMIT)
            .filter { allowed(it.categoryName, it.rating) }
            .map { movie ->
                HomeProgramEntry(
                    key = "movie:${movie.id}",
                    kind = HomeProgramKind.Movie,
                    title = movie.title,
                    description = movie.plot,
                    posterUrl = movie.posterUrl ?: movie.backdropUrl,
                    isWidePoster = movie.posterUrl.isNullOrBlank(),
                    intentUri = PlaybackDeepLink("movie", movie.id, profileId = session.currentProfileId).toUri(),
                )
            }
        val series = catalogDaoFacade.series.getLatest(sourceId, ROW_LIMIT / 2)
            .filter { allowed(it.categoryName, it.rating) }
            .map { show ->
                HomeProgramEntry(
                    key = "series:${show.id}",
                    kind = HomeProgramKind.Series,
                    title = show.title,
                    description = show.plot,
                    posterUrl = show.posterUrl ?: show.backdropUrl,
                    isWidePoster = show.posterUrl.isNullOrBlank(),
                    intentUri = PlaybackDeepLink("series", show.id, profileId = session.currentProfileId).toUri(),
                )
            }
        return HomeChannelRules.interleave(movies, series).take(ROW_LIMIT)
    }

    private suspend fun <T> safely(label: String, block: suspend () -> T): T? = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Log.w(TAG, "Skipped the $label row: ${error.safeSummary()}")
        null
    }

    private data class Input(
        val session: AppSessionState,
        val favorites: List<FavoriteItem>,
        val lastSyncedAtMs: Long?,
        val controls: ParentalControls? = null,
        val history: List<WatchHistoryItem> = emptyList(),
    )

    private companion object {
        const val TAG = "HomeChannels"
        const val CONTINUE_KEY = "continue"
        const val FAVORITES_KEY = "favorites"
        const val NEW_KEY = "new"
        const val ROW_LIMIT = 12
        const val DEBOUNCE_MS = 2_000L
    }
}
