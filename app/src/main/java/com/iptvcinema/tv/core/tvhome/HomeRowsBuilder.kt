package com.iptvcinema.tv.core.tvhome

import android.util.Log
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogProgram
import com.iptvcinema.tv.core.parental.ParentalPlaybackGuard
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.safeSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Builds the home rows (Continue watching, Favorite channels, Live now, New this week) from the
 * profile's data. One code path feeds both the Android TV home screen channels and the launcher
 * snapshot, so the two never disagree. Parental controls are applied here, before rows leave the app.
 */
@Singleton
class HomeRowsBuilder @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val catalogDaoFacade: CatalogDaoFacade,
    private val parentalPlaybackGuard: ParentalPlaybackGuard,
    private val appStrings: AppStrings,
) {
    /**
     * [continueWatching] is the repository's Continue watching list (movies and episodes);
     * [recentHistory] is the plain history, used for recently watched channels.
     */
    suspend fun build(
        session: AppSessionState,
        sourceId: String,
        favorites: List<FavoriteItem>,
        continueWatching: List<WatchHistoryItem>,
        recentHistory: List<WatchHistoryItem>,
        nowMs: Long = System.currentTimeMillis(),
    ): List<HomeChannelSpec> = buildList {
        // Rows stay even when empty: deleting and recreating a channel loses the viewer's
        // ordering and asks the system's "add channel" question again.
        add(row(HomeRowKeys.CONTINUE, R.string.tv_channel_continue) { continueWatching(session, continueWatching) })
        add(row(HomeRowKeys.FAVORITES, R.string.tv_channel_favorites) { favoriteChannels(session, sourceId, favorites) })
        add(row(HomeRowKeys.LIVE, R.string.tv_channel_live) { liveNow(session, sourceId, favorites, recentHistory, nowMs) })
        add(row(HomeRowKeys.NEW, R.string.tv_channel_new) { newestTitles(session, sourceId) })
    }

    private suspend fun row(key: String, titleRes: Int, block: suspend () -> List<HomeProgramEntry>): HomeChannelSpec =
        HomeChannelSpec(key, appStrings.get(titleRes), safely(key, block).orEmpty())

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
            progress = HomeRowRules.progressOf(item.positionMs, item.durationMs),
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
                channel.toEntry(session, description = channel.categoryName)
            }
    }

    /** Favourite channels first, then channels watched recently, each with what is on now. */
    private suspend fun liveNow(
        session: AppSessionState,
        sourceId: String,
        favorites: List<FavoriteItem>,
        history: List<WatchHistoryItem>,
        nowMs: Long,
    ): List<HomeProgramEntry> {
        val channelIds = HomeRowRules.liveNowChannelIds(
            favoriteChannelIds = favorites.filter { it.contentType == FavoriteContentType.CHANNEL }.map { it.contentId },
            recentChannelIds = history.filter { it.contentType == WatchHistoryContentType.CHANNEL }.map { it.contentId },
            limit = ROW_LIMIT,
        )
        if (channelIds.isEmpty()) return emptyList()
        val allowed = parentalPlaybackGuard.channelFilter(session)
        val channels = catalogRepository.getChannelsByIds(sourceId, channelIds).associateBy { it.id }
        val programs = catalogRepository.getCurrentProgramsForChannels(sourceId, channelIds, nowMs)
        return channelIds.mapNotNull { id ->
            val channel = channels[id] ?: return@mapNotNull null
            if (!allowed(channel)) return@mapNotNull null
            val program: CatalogProgram? = programs[id]
            channel.toEntry(
                session,
                description = program?.title ?: channel.categoryName,
                validUntilMs = program?.endEpochMs,
            )
        }
    }

    private fun CatalogChannel.toEntry(
        session: AppSessionState,
        description: String?,
        validUntilMs: Long? = null,
    ) = HomeProgramEntry(
        key = "live:$id",
        kind = HomeProgramKind.Channel,
        title = name,
        description = description,
        posterUrl = logoUrl,
        isWidePoster = true,
        intentUri = PlaybackDeepLink("live", id, profileId = session.currentProfileId).toUri(),
        validUntilMs = validUntilMs,
    )

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

    companion object {
        private const val TAG = "HomeChannels"
        const val ROW_LIMIT = 12
    }
}
