package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity
import com.iptvcinema.tv.core.database.entity.LocalMovieEntity
import com.iptvcinema.tv.core.database.entity.LocalSeriesEntity
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.model.catalog.CatalogCategory
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogContentType
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries

object XtreamNormalizer {
    fun normalizeLiveCategories(
        sourceId: String,
        dtos: List<XtreamCategoryDto>,
    ): Pair<List<LocalCategoryEntity>, List<CatalogCategory>> {
        val entities = dtos.mapIndexedNotNull { index, dto ->
            val id = dto.categoryId.asIdString() ?: return@mapIndexedNotNull null
            val name = dto.categoryName?.trim().orEmpty().ifBlank { "Uncategorized" }
            LocalCategoryEntity(
                id = id,
                sourceId = sourceId,
                name = name,
                contentType = CatalogContentType.LIVE.name,
                sortOrder = index,
            ) to CatalogCategory(
                id = id,
                sourceId = sourceId,
                name = name,
                contentType = CatalogContentType.LIVE,
                sortOrder = index,
            )
        }
        return entities.map { it.first } to entities.map { it.second }
    }

    fun normalizeLiveStreams(
        sourceId: String,
        credentials: XtreamCredentials,
        serverUrl: String,
        dtos: List<XtreamLiveStreamDto>,
        categoryNames: Map<String, String>,
    ): List<LocalChannelEntity> = dtos.mapIndexedNotNull { index, dto ->
        val streamId = dto.streamId.asIdString() ?: return@mapIndexedNotNull null
        val categoryId = dto.categoryId.asIdString()
        val name = dto.name?.trim().orEmpty().ifBlank { "Channel $streamId" }
        val directSource = dto.directSource?.trim().orEmpty()
        val streamUrl = directSource.ifBlank {
            XtreamStreamUrlBuilder.liveStreamUrl(
                serverUrl = serverUrl,
                username = credentials.username,
                password = credentials.password,
                streamId = streamId,
            )
        }
        LocalChannelEntity(
            id = streamId,
            sourceId = sourceId,
            name = name,
            streamUrl = streamUrl,
            logoUrl = dto.streamIcon?.trim()?.takeIf { it.isNotBlank() },
            categoryId = categoryId,
            categoryName = categoryId?.let { categoryNames[it] } ?: dto.categoryName,
            tvgId = dto.epgChannelId?.trim()?.takeIf { it.isNotBlank() },
            channelNumber = dto.num.asIntOrNull(),
            isAdult = dto.isAdult.asBooleanOrFalse(),
            sortOrder = index,
        )
    }

    fun normalizeVodCategories(
        sourceId: String,
        dtos: List<XtreamCategoryDto>,
    ): List<LocalCategoryEntity> = dtos.mapIndexedNotNull { index, dto ->
        val id = dto.categoryId.asIdString() ?: return@mapIndexedNotNull null
        LocalCategoryEntity(
            id = id,
            sourceId = sourceId,
            name = dto.categoryName?.trim().orEmpty().ifBlank { "Uncategorized" },
            contentType = CatalogContentType.VOD.name,
            sortOrder = index,
        )
    }

    fun normalizeVodStreams(
        sourceId: String,
        credentials: XtreamCredentials,
        serverUrl: String,
        dtos: List<XtreamVodStreamDto>,
        categoryNames: Map<String, String>,
    ): List<LocalMovieEntity> = dtos.mapIndexedNotNull { index, dto ->
        val streamId = dto.streamId.asIdString() ?: return@mapIndexedNotNull null
        val categoryId = dto.categoryId.asIdString()
        val extension = dto.containerExtension?.trim().orEmpty().ifBlank { "mp4" }
        val directSource = dto.directSource?.trim().orEmpty()
        val streamUrl = directSource.ifBlank {
            XtreamStreamUrlBuilder.vodStreamUrl(
                serverUrl = serverUrl,
                username = credentials.username,
                password = credentials.password,
                streamId = streamId,
                extension = extension,
            )
        }
        LocalMovieEntity(
            id = streamId,
            sourceId = sourceId,
            title = dto.name?.trim().orEmpty().ifBlank { "Movie $streamId" },
            streamUrl = streamUrl,
            posterUrl = dto.streamIcon?.trim()?.takeIf { it.isNotBlank() },
            backdropUrl = dto.backdropPath.firstStringOrNull(),
            categoryId = categoryId,
            categoryName = categoryId?.let { categoryNames[it] } ?: dto.categoryName,
            year = parseYear(dto.releaseDate),
            durationMinutes = parseDurationMinutes(dto.durationSecs?.asIntOrNull(), dto.duration),
            rating = dto.rating?.trim()?.takeIf { it.isNotBlank() },
            plot = dto.plot?.trim()?.takeIf { it.isNotBlank() },
            genres = dto.genre?.trim()?.takeIf { it.isNotBlank() },
            cast = dto.cast?.trim()?.takeIf { it.isNotBlank() },
            youtubeTrailer = dto.youtubeTrailer?.trim()?.takeIf { it.isNotBlank() },
            sortOrder = index,
            addedAt = parseAddedTimestamp(dto.added),
        )
    }

    fun normalizeSeriesCategories(
        sourceId: String,
        dtos: List<XtreamCategoryDto>,
    ): List<LocalCategoryEntity> = dtos.mapIndexedNotNull { index, dto ->
        val id = dto.categoryId.asIdString() ?: return@mapIndexedNotNull null
        LocalCategoryEntity(
            id = id,
            sourceId = sourceId,
            name = dto.categoryName?.trim().orEmpty().ifBlank { "Uncategorized" },
            contentType = CatalogContentType.SERIES.name,
            sortOrder = index,
        )
    }

    fun normalizeSeries(
        sourceId: String,
        dtos: List<XtreamSeriesDto>,
        categoryNames: Map<String, String>,
    ): List<LocalSeriesEntity> = dtos.mapIndexedNotNull { index, dto ->
        val seriesId = dto.seriesId.asIdString() ?: return@mapIndexedNotNull null
        val categoryId = dto.categoryId.asIdString()
        LocalSeriesEntity(
            id = seriesId,
            sourceId = sourceId,
            title = dto.name?.trim().orEmpty().ifBlank { "Series $seriesId" },
            posterUrl = dto.cover?.trim()?.takeIf { it.isNotBlank() },
            backdropUrl = dto.backdropPath.firstStringOrNull(),
            categoryId = categoryId,
            categoryName = categoryId?.let { categoryNames[it] } ?: dto.categoryName,
            plot = dto.plot?.trim()?.takeIf { it.isNotBlank() },
            rating = dto.rating?.trim()?.takeIf { it.isNotBlank() },
            year = parseYear(dto.releaseDate),
            cast = dto.cast?.trim()?.takeIf { it.isNotBlank() },
            youtubeTrailer = dto.youtubeTrailer?.trim()?.takeIf { it.isNotBlank() },
            sortOrder = index,
        )
    }

    fun normalizeSeriesInfo(
        sourceId: String,
        seriesId: String,
        credentials: XtreamCredentials,
        serverUrl: String,
        response: XtreamSeriesInfoResponse,
    ): List<LocalEpisodeEntity> {
        val episodes = response.episodes.orEmpty()
        return episodes.flatMap { (seasonKey, episodeList) ->
            val seasonFromKey = seasonKey.toIntOrNull()
            episodeList.mapNotNull { dto ->
                val episodeId = dto.id.asIdString() ?: return@mapNotNull null
                val extension = dto.containerExtension?.trim().orEmpty().ifBlank { "mp4" }
                val directSource = dto.directSource?.trim().orEmpty()
                val streamUrl = directSource.ifBlank {
                    XtreamStreamUrlBuilder.seriesStreamUrl(
                        serverUrl = serverUrl,
                        username = credentials.username,
                        password = credentials.password,
                        episodeId = episodeId,
                        extension = extension,
                    )
                }
                LocalEpisodeEntity(
                    id = episodeId,
                    sourceId = sourceId,
                    seriesId = seriesId,
                    seasonNumber = dto.season.asIntOrNull() ?: seasonFromKey ?: 1,
                    episodeNumber = dto.episodeNum.asIntOrNull() ?: 0,
                    title = dto.title?.trim().orEmpty().ifBlank { "Episode ${dto.episodeNum.asIdString() ?: episodeId}" },
                    streamUrl = streamUrl,
                    durationMinutes = parseDurationMinutes(
                        dto.info?.durationSecs?.asIntOrNull(),
                        dto.info?.duration,
                    ),
                    plot = dto.info?.plot?.trim()?.takeIf { it.isNotBlank() },
                    thumbnailUrl = dto.info?.movieImage?.trim()?.takeIf { it.isNotBlank() },
                )
            }
        }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
    }

    fun seasonNumbersFromSeriesInfo(response: XtreamSeriesInfoResponse): List<Int> {
        val fromSeasons = response.seasons.orEmpty().mapNotNull { it.seasonNumber.asIntOrNull() }
        val fromEpisodes = response.episodes.orEmpty().flatMap { (seasonKey, episodeList) ->
            val seasonFromKey = seasonKey.toIntOrNull()
            episodeList.mapNotNull { dto -> dto.season.asIntOrNull() ?: seasonFromKey }
        }
        return (fromSeasons + fromEpisodes).distinct().sorted()
    }

    private fun parseYear(value: String?): Int? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.length >= 4) {
            trimmed.take(4).toIntOrNull()?.let { return it }
        }
        return trimmed.toIntOrNull()
    }

    private fun parseDurationMinutes(durationSecs: Int?, durationText: String?): Int? {
        durationSecs?.let { if (it > 0) return (it + 59) / 60 }
        val trimmed = durationText?.trim().orEmpty()
        if (trimmed.contains(":")) {
            val parts = trimmed.split(":").mapNotNull { it.toIntOrNull() }
            return when (parts.size) {
                3 -> parts[0] * 60 + parts[1] + if (parts[2] > 0) 1 else 0
                2 -> parts[0] + if (parts[1] > 0) 1 else 0
                else -> null
            }
        }
        return trimmed.toIntOrNull()
    }

    internal fun parseAddedTimestamp(value: String?): Long? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val numeric = trimmed.toLongOrNull()
            ?: return runCatching { java.time.Instant.parse(trimmed).toEpochMilli() }.getOrNull()
        return if (numeric > 1_000_000_000_000L) numeric else numeric * 1000L
    }
}
