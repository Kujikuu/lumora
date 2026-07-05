package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class PlaybackRepository @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val appSessionRepository: AppSessionRepository,
    private val localCredentialsStore: LocalCredentialsStore,
    private val episodeCatalogRepository: EpisodeCatalogRepository,
) {
    suspend fun resolve(
        contentId: String,
        contentType: String,
        seriesId: String? = null,
    ): PlaybackResolveResult {
        val session = appSessionRepository.sessionState.first()
        if (session.isDemoMode) {
            return resolveDemo(contentId, contentType)
        }

        val sourceId = session.currentSourceId
            ?: return PlaybackResolveResult.Error("No source connected", "NO_SOURCE")

        val headers = buildHeaders(sourceId, session.sourceType)

        return when (contentType.lowercase()) {
            "live" -> resolveLive(sourceId, contentId, headers)
            "movie" -> resolveMovie(sourceId, contentId, headers)
            "episode" -> resolveEpisode(sourceId, contentId, headers, seriesId)
            else -> PlaybackResolveResult.Error("Unknown content type", "INVALID_TYPE")
        }
    }

    suspend fun resolveChannel(sourceId: String, channelId: String): PlaybackResolveResult {
        val session = appSessionRepository.sessionState.first()
        val headers = buildHeaders(sourceId, session.sourceType)
        return resolveLive(sourceId, channelId, headers)
    }

    suspend fun resolveEpisode(sourceId: String, episode: CatalogEpisode): PlaybackResolveResult {
        val session = appSessionRepository.sessionState.first()
        if (session.isDemoMode) {
            return resolveDemo(episode.id, "episode")
        }
        val headers = buildHeaders(sourceId, session.sourceType)
        val posterUrl = episode.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: catalogRepository.getSeries(sourceId, episode.seriesId)?.posterUrl
        return episode.toPlaybackRequest(headers, posterUrl)
    }

    private suspend fun resolveLive(
        sourceId: String,
        contentId: String,
        headers: PlaybackHeaders,
    ): PlaybackResolveResult {
        val channel = catalogRepository.getChannel(sourceId, contentId)
            ?: return PlaybackResolveResult.Error("Channel not found", "NOT_FOUND")
        val programTitle = catalogRepository.getCurrentProgram(sourceId, contentId)?.title
        return channel.toPlaybackRequest(headers, programTitle)
    }

    private suspend fun resolveMovie(
        sourceId: String,
        contentId: String,
        headers: PlaybackHeaders,
    ): PlaybackResolveResult {
        val movie = catalogRepository.getMovie(sourceId, contentId)
            ?: return PlaybackResolveResult.Error("Movie not found", "NOT_FOUND")
        return movie.toPlaybackRequest(headers)
    }

    private suspend fun resolveEpisode(
        sourceId: String,
        contentId: String,
        headers: PlaybackHeaders,
        seriesId: String?,
    ): PlaybackResolveResult {
        val episode = episodeCatalogRepository.getEpisode(sourceId, contentId, seriesId)
            ?: return PlaybackResolveResult.Error("Episode not found", "NOT_FOUND")
        val posterUrl = episode.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: catalogRepository.getSeries(sourceId, episode.seriesId)?.posterUrl
        return episode.toPlaybackRequest(headers, posterUrl)
    }

    private fun resolveDemo(contentId: String, contentType: String): PlaybackResolveResult {
        val movie = FakeDataProvider.movieById(contentId)
        val channel = FakeDataProvider.channels.find { it.id == contentId }
        val episode = FakeDataProvider.seriesList
            .flatMap { it.seasons }
            .flatMap { it.episodes }
            .find { it.id == contentId }

        val title = movie?.title ?: channel?.name ?: episode?.title ?: "Demo Stream"
        val posterUrl = movie?.imageUrl ?: channel?.logoUrl
        val isLive = contentType.equals("live", ignoreCase = true)
        val historyType = when {
            isLive -> WatchHistoryContentType.CHANNEL
            contentType.equals("episode", ignoreCase = true) -> WatchHistoryContentType.EPISODE
            else -> WatchHistoryContentType.MOVIE
        }
        val metadata = when {
            movie != null -> listOf(
                movie.year.toString(),
                movie.genres.joinToString(" "),
                "${movie.runtimeMinutes / 60}h ${movie.runtimeMinutes % 60}m",
            )
            channel != null -> listOf("LIVE", channel.currentProgram)
            else -> listOf("DEMO")
        }
        return PlaybackResolveResult.Success(
            PlaybackRequest(
                contentId = contentId,
                contentType = historyType,
                sourceId = null,
                title = title,
                posterUrl = posterUrl,
                streamUrl = DEMO_HLS_URL,
                durationMs = movie?.runtimeMinutes?.times(60_000L)?.toLong()
                    ?: episode?.durationMinutes?.times(60_000L)?.toLong(),
                isLive = isLive,
                metadata = metadata,
            ),
        )
    }

    private fun buildHeaders(sourceId: String, sourceType: SourceType?): PlaybackHeaders {
        if (sourceType != SourceType.M3U) return PlaybackHeaders()
        val credentials = localCredentialsStore.getM3uCredentials(sourceId) ?: return PlaybackHeaders()
        return PlaybackHeaders(
            userAgent = credentials.userAgent,
            referer = credentials.referer,
            customHeaders = credentials.customHeaders,
        )
    }

    private fun CatalogChannel.toPlaybackRequest(
        headers: PlaybackHeaders,
        programTitle: String? = null,
    ): PlaybackResolveResult {
        if (streamUrl.isBlank()) {
            return PlaybackResolveResult.Error("Stream unavailable", "EMPTY_URL")
        }
        val meta = buildList {
            add("LIVE")
            categoryName?.takeIf { it.isNotBlank() }?.let { add(it) }
            programTitle?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        return PlaybackResolveResult.Success(
            PlaybackRequest(
                contentId = id,
                contentType = WatchHistoryContentType.CHANNEL,
                sourceId = sourceId,
                title = name,
                posterUrl = logoUrl,
                streamUrl = streamUrl,
                durationMs = null,
                isLive = true,
                headers = headers,
                metadata = meta,
            ),
        )
    }

    private fun CatalogMovie.toPlaybackRequest(headers: PlaybackHeaders): PlaybackResolveResult {
        if (streamUrl.isBlank()) {
            return PlaybackResolveResult.Error("Stream unavailable", "EMPTY_URL")
        }
        val meta = buildList {
            year?.let { add(it.toString()) }
            if (genres.isNotEmpty()) add(genres.joinToString(" "))
            durationMinutes?.let { add("${it / 60}h ${it % 60}m") }
        }
        return PlaybackResolveResult.Success(
            PlaybackRequest(
                contentId = id,
                contentType = WatchHistoryContentType.MOVIE,
                sourceId = sourceId,
                title = title,
                posterUrl = posterUrl,
                streamUrl = streamUrl,
                durationMs = durationMinutes?.times(60_000L)?.toLong(),
                isLive = false,
                headers = headers,
                metadata = meta,
            ),
        )
    }

    private fun CatalogEpisode.toPlaybackRequest(
        headers: PlaybackHeaders,
        posterUrl: String? = thumbnailUrl,
    ): PlaybackResolveResult {
        if (streamUrl.isBlank()) {
            return PlaybackResolveResult.Error("Stream unavailable", "EMPTY_URL")
        }
        val meta = buildList {
            add("S${seasonNumber}E$episodeNumber")
            durationMinutes?.let { add("${it}m") }
        }
        return PlaybackResolveResult.Success(
            PlaybackRequest(
                contentId = id,
                contentType = WatchHistoryContentType.EPISODE,
                sourceId = sourceId,
                title = title,
                posterUrl = posterUrl,
                streamUrl = streamUrl,
                durationMs = durationMinutes?.times(60_000L)?.toLong(),
                isLive = false,
                headers = headers,
                metadata = meta,
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
            ),
        )
    }

    companion object {
        const val DEMO_HLS_URL =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"
    }
}
