package com.iptvcinema.tv.core.data.mapper

import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity
import com.iptvcinema.tv.core.database.entity.LocalMovieEntity
import com.iptvcinema.tv.core.database.entity.LocalProgramEntity
import com.iptvcinema.tv.core.database.entity.LocalSeriesEntity
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.catalog.CatalogCategory
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogContentType
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogProgram
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.catalog.CatalogSyncState
import com.iptvcinema.tv.core.util.RatingFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CatalogEntityMapper {
    fun LocalCategoryEntity.toDomain(): CatalogCategory = CatalogCategory(
        id = id,
        sourceId = sourceId,
        name = name,
        contentType = runCatching { CatalogContentType.valueOf(contentType) }
            .getOrDefault(CatalogContentType.LIVE),
        sortOrder = sortOrder,
    )

    fun LocalChannelEntity.toDomain(): CatalogChannel = CatalogChannel(
        id = id,
        sourceId = sourceId,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        categoryId = categoryId,
        categoryName = categoryName,
        tvgId = tvgId,
        channelNumber = channelNumber,
        isAdult = isAdult,
        sortOrder = sortOrder,
    )

    fun LocalMovieEntity.toDomain(): CatalogMovie = CatalogMovie(
        id = id,
        sourceId = sourceId,
        title = title,
        streamUrl = streamUrl,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        categoryId = categoryId,
        categoryName = categoryName,
        year = year,
        durationMinutes = durationMinutes,
        rating = rating,
        plot = plot,
        genres = genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty(),
        cast = cast,
        youtubeTrailer = youtubeTrailer,
        sortOrder = sortOrder,
        addedAt = addedAt,
    )

    fun LocalSeriesEntity.toDomain(): CatalogSeries = CatalogSeries(
        id = id,
        sourceId = sourceId,
        title = title,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        categoryId = categoryId,
        categoryName = categoryName,
        plot = plot,
        rating = rating,
        year = year,
        cast = cast,
        youtubeTrailer = youtubeTrailer,
        sortOrder = sortOrder,
    )

    fun LocalEpisodeEntity.toDomain(): CatalogEpisode = CatalogEpisode(
        id = id,
        sourceId = sourceId,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title,
        streamUrl = streamUrl,
        durationMinutes = durationMinutes,
        plot = plot,
        thumbnailUrl = thumbnailUrl,
    )

    fun LocalProgramEntity.toDomain(): CatalogProgram = CatalogProgram(
        id = id,
        sourceId = sourceId,
        channelId = channelId,
        title = title,
        description = description,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
    )

    fun LocalSourceSyncStateEntity.toDomain(): CatalogSyncState = CatalogSyncState(
        sourceId = sourceId,
        lastSyncedAtEpochMs = lastSyncedAtEpochMs,
        liveChannelCount = liveChannelCount,
        movieCount = movieCount,
        seriesCount = seriesCount,
        epgAvailable = epgAvailable,
        lastError = lastError,
    )
}

object CatalogUiMapper {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun CatalogChannel.toChannelItem(
        currentProgram: CatalogProgram? = null,
        nowMs: Long = System.currentTimeMillis(),
        noProgramInfoTitle: String = "No program info",
    ): ChannelItem {
        val program = currentProgram ?: placeholderProgram(nowMs, noProgramInfoTitle)
        val durationMs = (program.endEpochMs - program.startEpochMs).coerceAtLeast(1)
        val elapsedMs = (nowMs - program.startEpochMs).coerceIn(0, durationMs)
        return ChannelItem(
            id = id,
            name = name,
            logoUrl = logoUrl,
            channelNumber = channelNumber ?: 0,
            category = categoryName.orEmpty(),
            currentProgram = program.title,
            programDescription = program.description.orEmpty(),
            programStart = formatTime(program.startEpochMs),
            programEnd = formatTime(program.endEpochMs),
            programProgress = elapsedMs.toFloat() / durationMs.toFloat(),
        )
    }

    fun CatalogMovie.toMovieItem(isFavorite: Boolean = false): MovieItem = MovieItem(
        id = id,
        title = title,
        year = year ?: 0,
        runtimeMinutes = durationMinutes ?: 0,
        rating = RatingFormatter.formatForDisplay(rating).orEmpty(),
        plot = plot.orEmpty(),
        genres = genres,
        is4K = title.contains("4k", ignoreCase = true) ||
            streamUrl.contains("4k", ignoreCase = true),
        isFavorite = isFavorite,
        imageUrl = posterUrl,
        backdropUrl = backdropUrl,
        sortOrder = sortOrder,
        addedAt = addedAt,
    )

    fun CatalogSeries.toSeriesItem(
        isFavorite: Boolean = false,
        seasonCount: Int = 1,
        hasNewEpisode: Boolean = false,
        is4K: Boolean = false,
    ): SeriesItem = SeriesItem(
        id = id,
        title = title,
        year = year ?: 0,
        rating = RatingFormatter.formatForDisplay(rating).orEmpty(),
        plot = plot.orEmpty(),
        genres = listOfNotNull(categoryName?.takeIf { it.isNotBlank() }),
        seasonCount = seasonCount,
        is4K = is4K || title.contains("4k", ignoreCase = true),
        hasNewEpisode = hasNewEpisode,
        isFavorite = isFavorite,
        imageUrl = posterUrl,
        backdropUrl = backdropUrl,
        sortOrder = sortOrder,
    )

    fun CatalogMovie.toPosterCardData(): PosterCardData = PosterCardData(
        title = title,
        year = year?.takeIf { it > 0 }?.toString(),
        runtime = durationMinutes?.takeIf { it > 0 }?.let { "${it}m" },
        imageUrl = posterUrl,
        contentId = id,
        is4K = title.contains("4k", ignoreCase = true) ||
            streamUrl.contains("4k", ignoreCase = true),
    )

    fun CatalogSeries.toPosterCardData(
        seasonCount: Int? = null,
        hasNewEpisode: Boolean = false,
    ): PosterCardData = PosterCardData(
        title = title,
        year = year?.takeIf { it > 0 }?.toString(),
        imageUrl = posterUrl,
        contentId = id,
        is4K = title.contains("4k", ignoreCase = true),
        seasonCount = seasonCount,
        hasNewEpisode = hasNewEpisode,
    )

    fun MovieItem.toPosterCardData(): PosterCardData = PosterCardData(
        title = title,
        year = year.takeIf { it > 0 }?.toString(),
        runtime = runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
        imageUrl = imageUrl,
        contentId = id,
        is4K = is4K,
        progress = progress,
        isFavorite = isFavorite,
    )

    fun SeriesItem.toPosterCardData(): PosterCardData = PosterCardData(
        title = title,
        year = year.takeIf { it > 0 }?.toString(),
        imageUrl = imageUrl,
        contentId = id,
        is4K = is4K,
        seasonCount = seasonCount.takeIf { it > 0 },
        hasNewEpisode = hasNewEpisode,
        progress = progress,
        isFavorite = isFavorite,
    )

    fun List<CatalogProgram>.toEpgPrograms(nowMs: Long = System.currentTimeMillis()): List<EpgProgram> {
        val zone = ZoneId.systemDefault()
        return map { program ->
            val start = Instant.ofEpochMilli(program.startEpochMs).atZone(zone)
            val durationMinutes = ((program.endEpochMs - program.startEpochMs) / 60_000L).toInt().coerceAtLeast(1)
            EpgProgram(
                id = program.id,
                channelId = program.channelId,
                title = program.title,
                startHour = start.hour,
                startMinute = start.minute,
                durationMinutes = durationMinutes,
                startEpochMs = program.startEpochMs,
                endEpochMs = program.endEpochMs,
                description = program.description.orEmpty(),
            )
        }
    }

    fun ChannelItem.toChannelTileData(nowPlayingChannelId: String? = null): ChannelTileData = ChannelTileData(
        id = id,
        channelName = name,
        logoUrl = logoUrl,
        currentProgram = currentProgram,
        isLive = true,
        isNowPlaying = nowPlayingChannelId != null && id == nowPlayingChannelId,
        qualityBadge = qualityBadge,
        programProgress = programProgress,
    )

    private fun placeholderProgram(nowMs: Long, title: String): CatalogProgram = CatalogProgram(
        id = "placeholder",
        sourceId = "",
        channelId = "",
        title = title,
        description = "",
        startEpochMs = nowMs,
        endEpochMs = nowMs + 3_600_000,
    )

    private fun formatTime(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(timeFormatter)
}
