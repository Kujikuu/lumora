package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.data.mapper.CatalogEntityMapper.toDomain
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.util.CastParser
import com.iptvcinema.tv.core.xtream.XtreamNormalizer
import com.iptvcinema.tv.core.xtream.XtreamRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class EpisodeCatalogRepository @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val xtreamRepository: XtreamRepository,
    private val appSessionRepository: AppSessionRepository,
) {
    suspend fun getEpisode(
        sourceId: String,
        episodeId: String,
        seriesId: String? = null,
    ): CatalogEpisode? {
        catalogRepository.getEpisode(sourceId, episodeId)?.let { return it }
        if (seriesId == null) return null
        return getEpisodesForSeries(sourceId, seriesId).find { it.id == episodeId }
    }

    suspend fun getEpisodesForSeries(
        sourceId: String,
        seriesId: String,
        forceRefresh: Boolean = false,
    ): List<CatalogEpisode> =
        getSeriesEpisodeCatalog(sourceId, seriesId, forceRefresh).episodes

    suspend fun getSeriesEpisodeCatalog(
        sourceId: String,
        seriesId: String,
        forceRefresh: Boolean = false,
    ): SeriesEpisodeCatalogResult {
        if (!forceRefresh) {
            val cached = catalogRepository.getEpisodesForSeries(sourceId, seriesId)
            if (cached.isNotEmpty()) {
                return SeriesEpisodeCatalogResult(
                    episodes = cached.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })),
                    seasonNumbers = cached.map { it.seasonNumber }.distinct().sorted(),
                )
            }
        }
        val session = appSessionRepository.sessionState.first()
        if (session.sourceType == SourceType.XTREAM_CODES) {
            fetchAndCacheEpisodes(sourceId, seriesId)?.let { return it }
        }
        val episodes = catalogRepository.getEpisodesForSeries(sourceId, seriesId)
        return SeriesEpisodeCatalogResult(
            episodes = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })),
            seasonNumbers = episodes.map { it.seasonNumber }.distinct().sorted(),
        )
    }

    suspend fun prefetchTopSeriesEpisodes(sourceId: String, limit: Int = 5) {
        val seriesIds = catalogRepository.getTopSeriesIds(sourceId, limit)
        for (seriesId in seriesIds) {
            runCatching {
                getSeriesEpisodeCatalog(sourceId, seriesId, forceRefresh = true)
            }
        }
    }

    private suspend fun fetchAndCacheEpisodes(
        sourceId: String,
        seriesId: String,
    ): SeriesEpisodeCatalogResult? {
        val credentials = xtreamRepository.getCredentials(sourceId) ?: return null
        return runCatching {
            val response = xtreamRepository.fetchSeriesInfo(credentials, seriesId)
            val serverUrl = xtreamRepository.normalizedServer(credentials)
            val normalized = XtreamNormalizer.normalizeSeriesInfo(
                sourceId = sourceId,
                seriesId = seriesId,
                credentials = credentials,
                serverUrl = serverUrl,
                response = response,
            )
            catalogRepository.replaceEpisodesForSeries(sourceId, seriesId, normalized)
            val info = response.info
            SeriesEpisodeCatalogResult(
                episodes = normalized.map { it.toDomain() },
                seasonNumbers = XtreamNormalizer.seasonNumbersFromSeriesInfo(response),
                plot = info?.plot?.trim()?.takeIf { it.isNotBlank() },
                rating = info?.rating?.trim()?.takeIf { it.isNotBlank() },
                cast = CastParser.parseCastMembers(info?.cast),
                genres = CastParser.parseGenres(info?.genre),
            )
        }.getOrNull()
    }
}
