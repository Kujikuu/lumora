package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.mapper.CatalogEntityMapper.toDomain
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toEpgPrograms
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.database.CatalogDaoFacade
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogChannel
import com.iptvcinema.tv.core.model.catalog.CatalogContentType
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.catalog.CatalogSyncState
import com.iptvcinema.tv.core.model.catalog.FeaturedCatalogContent
import com.iptvcinema.tv.core.player.ChannelDirection
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.util.AppStrings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

enum class CatalogLoadState {
    Loading,
    Ready,
    Empty,
    Error,
}

data class CatalogBrowseState<T>(
    val loadState: CatalogLoadState,
    val categories: List<String> = emptyList(),
    val items: List<T> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
)

data class WatchHistoryCardDisplay(
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
)

data class CatalogSearchResults(
    val movies: List<MovieItem>,
    val series: List<SeriesItem>,
    val channels: List<ChannelItem>,
)

enum class CatalogSearchFilter {
    All,
    Movies,
    Series,
    Live,
}

@Singleton
class CatalogRepository @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val catalogDaoFacade: CatalogDaoFacade,
    private val playlistSourcesRepository: PlaylistSourcesRepository,
    private val appStrings: AppStrings,
) {
    fun observeSyncState(): Flow<CatalogSyncState?> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            val sourceId = session.currentSourceId
            if (session.isDemoMode || sourceId == null) {
                flowOf(null)
            } else {
                catalogDaoFacade.syncState.observe(sourceId).map { it?.toDomain() }
            }
        }

    suspend fun purgeSource(sourceId: String) {
        catalogDaoFacade.purgeSource(sourceId)
    }

    suspend fun getTopSeriesIds(sourceId: String, limit: Int): List<String> =
        catalogDaoFacade.series.getTopIds(sourceId, limit)

    fun observeSourceMeta(): Flow<Pair<SourceStatus?, SourceType?>> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            flow {
                when {
                    session.isDemoMode -> emit(null to SourceType.DEMO)
                    session.currentSourceId == null -> emit(null to null)
                    else -> {
                        val source = playlistSourcesRepository.getSources()
                            .find { it.id == session.currentSourceId }
                        emit(source?.status to source?.type)
                    }
                }
            }
        }

    fun observeHomeContent(): Flow<CatalogBrowseState<FeaturedCatalogContent>> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            if (session.isDemoMode) {
                flowOf(
                    CatalogBrowseState(
                        loadState = CatalogLoadState.Ready,
                        items = listOf(buildDemoFeatured()),
                    ),
                )
            } else {
                val sourceId = session.currentSourceId
                if (sourceId == null) {
                    flowOf(CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_source_connected)))
                } else {
                    combine(
                        catalogDaoFacade.movies.observeFeatured(sourceId, 12),
                        catalogDaoFacade.movies.observeRecentlyAdded(sourceId, 5),
                        catalogDaoFacade.series.observeFeatured(sourceId, 8),
                        catalogDaoFacade.channels.observeFeatured(sourceId, 8),
                        catalogDaoFacade.syncState.observe(sourceId),
                    ) { movies, heroMovies, series, channels, syncState ->
                        if (movies.isEmpty() && heroMovies.isEmpty() && series.isEmpty() &&
                            channels.isEmpty() && syncState == null
                        ) {
                            CatalogBrowseState(CatalogLoadState.Loading)
                        } else if (movies.isEmpty() && heroMovies.isEmpty() && series.isEmpty() &&
                            channels.isEmpty()
                        ) {
                            CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_content_synced))
                        } else {
                            val domainMovies = movies.map { it.toDomain() }
                            val domainHeroMovies = heroMovies.map { it.toDomain() }
                            val domainSeries = series.map { it.toDomain() }
                            val domainChannels = channels.map { it.toDomain() }
                            CatalogBrowseState(
                                loadState = CatalogLoadState.Ready,
                                items = listOf(
                                    FeaturedCatalogContent(
                                        heroMovies = domainHeroMovies,
                                        continueWatchingMovies = domainMovies.take(6),
                                        trendingMovies = domainMovies.drop(1).take(8).ifEmpty { domainMovies },
                                        liveChannels = domainChannels,
                                        newReleaseMovies = domainMovies.takeLast(6).ifEmpty { domainMovies },
                                        featuredSeries = domainSeries,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }.distinctUntilChanged().withSourceContext()

    fun observeLiveTv(categoryName: String? = null): Flow<CatalogBrowseState<ChannelItem>> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            if (session.isDemoMode) {
                val categories = FakeDataProvider.liveCategories
                val selected = categoryName?.takeIf { it in categories }
                    ?: categories.firstOrNull().orEmpty()
                flowOf(
                    CatalogBrowseState(
                        loadState = CatalogLoadState.Ready,
                        categories = categories,
                        items = FakeDataProvider.channelsForCategory(selected),
                    ),
                )
            } else {
                observeChannelBrowse(session.currentSourceId, categoryName, CatalogContentType.LIVE)
            }
        }.withSourceContext()

    fun observeMovies(categoryName: String? = null): Flow<CatalogBrowseState<MovieItem>> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            if (session.isDemoMode) {
                val categories = FakeDataProvider.movieCategories
                val index = FakeDataProvider.categoryIndex(categories, categoryName.orEmpty())
                flowOf(
                    CatalogBrowseState(
                        loadState = CatalogLoadState.Ready,
                        categories = categories,
                        items = FakeDataProvider.moviesForCategory(categories[index]),
                    ),
                )
            } else {
                observeMovieBrowse(session.currentSourceId, categoryName)
            }
        }.withSourceContext()

    fun observeRecentlyAddedMovies(limit: Int = 20): Flow<List<MovieItem>> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            if (session.isDemoMode) {
                flowOf(FakeDataProvider.recentlyAddedMovies(limit))
            } else {
                val sourceId = session.currentSourceId
                if (sourceId == null) {
                    flowOf(emptyList())
                } else {
                    catalogDaoFacade.movies.observeRecentlyAdded(sourceId, limit).map { movies ->
                        movies.map { movie ->
                            with(CatalogUiMapper) { movie.toDomain().toMovieItem() }
                        }
                    }
                }
            }
        }

    fun observeFeaturedMovie(): Flow<MovieItem?> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            if (session.isDemoMode) {
                flowOf(FakeDataProvider.featuredHero)
            } else {
                val sourceId = session.currentSourceId
                if (sourceId == null) {
                    flowOf(null)
                } else {
                    catalogDaoFacade.movies.observeFeatured(sourceId, limit = 1).map { movies ->
                        movies.firstOrNull()?.let { movie ->
                            with(CatalogUiMapper) { movie.toDomain().toMovieItem() }
                        }
                    }
                }
            }
        }

    fun observeSeries(categoryName: String? = null): Flow<CatalogBrowseState<SeriesItem>> =
        appSessionRepository.sessionState.flatMapLatest { session ->
            if (session.isDemoMode) {
                val categories = FakeDataProvider.seriesCategories
                val index = FakeDataProvider.categoryIndex(categories, categoryName.orEmpty())
                flowOf(
                    CatalogBrowseState(
                        loadState = CatalogLoadState.Ready,
                        categories = categories,
                        items = FakeDataProvider.seriesForCategory(categories[index]),
                    ),
                )
            } else {
                observeSeriesBrowse(session.currentSourceId, categoryName)
            }
        }.withSourceContext()

    suspend fun searchCatalog(
        query: String,
        filter: CatalogSearchFilter = CatalogSearchFilter.All,
        limit: Int = 20,
    ): CatalogSearchResults {
        val session = appSessionRepository.sessionState.first()
        if (session.isDemoMode) {
            val results = FakeDataProvider.searchResults(query, filter.name)
            return CatalogSearchResults(
                movies = results.movies,
                series = results.series,
                channels = results.channels,
            )
        }
        val sourceId = session.currentSourceId ?: return CatalogSearchResults(emptyList(), emptyList(), emptyList())
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            return CatalogSearchResults(emptyList(), emptyList(), emptyList())
        }
        val movies = if (filter == CatalogSearchFilter.All || filter == CatalogSearchFilter.Movies) {
            catalogDaoFacade.movies.searchByTitle(sourceId, trimmed, limit).map { movie ->
                with(CatalogUiMapper) { movie.toDomain().toMovieItem() }
            }
        } else {
            emptyList()
        }
        val series = if (filter == CatalogSearchFilter.All || filter == CatalogSearchFilter.Series) {
            catalogDaoFacade.series.searchByTitle(sourceId, trimmed, limit).map { item ->
                with(CatalogUiMapper) { item.toDomain().toSeriesItem() }
            }
        } else {
            emptyList()
        }
        val channels = if (filter == CatalogSearchFilter.All || filter == CatalogSearchFilter.Live) {
            val nowMs = System.currentTimeMillis()
            val channelEntities = catalogDaoFacade.channels.searchByName(sourceId, trimmed, limit)
            val currentPrograms = getCurrentProgramsForChannels(sourceId, channelEntities.map { it.id }, nowMs)
            channelEntities.map { channel ->
                with(CatalogUiMapper) {
                    channel.toDomain().toChannelItem(
                        currentProgram = currentPrograms[channel.id],
                        nowMs = nowMs,
                        noProgramInfoTitle = appStrings.get(R.string.msg_no_program_info),
                    )
                }
            }
        } else {
            emptyList()
        }
        return CatalogSearchResults(movies = movies, series = series, channels = channels)
    }

    suspend fun getCategoryNames(contentType: CatalogContentType): List<String> {
        val session = appSessionRepository.sessionState.first()
        val sourceId = session.currentSourceId ?: return emptyList()
        return catalogDaoFacade.categories.getByType(sourceId, contentType.name).map { it.name }
    }

    suspend fun getEpgForChannels(
        sourceId: String,
        channelIds: List<String>,
        windowStartMs: Long,
        windowEndMs: Long,
    ): List<EpgProgram> {
        if (channelIds.isEmpty()) return emptyList()
        val programs = catalogDaoFacade.programs.getProgramsForChannels(
            sourceId = sourceId,
            channelIds = channelIds,
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs,
        ).map { it.toDomain() }
        return programs.toEpgPrograms()
    }

    suspend fun getCurrentProgramsForChannels(
        sourceId: String,
        channelIds: List<String>,
        nowMs: Long = System.currentTimeMillis(),
    ): Map<String, com.iptvcinema.tv.core.model.catalog.CatalogProgram> {
        if (channelIds.isEmpty()) return emptyMap()
        return catalogDaoFacade.programs.getCurrentProgramsForChannels(sourceId, channelIds, nowMs)
            .map { it.toDomain() }
            .associateBy { it.channelId }
    }

    suspend fun getChannel(sourceId: String, channelId: String): CatalogChannel? =
        catalogDaoFacade.channels.getById(sourceId, channelId)?.toDomain()

    suspend fun getOrderedChannels(sourceId: String): List<CatalogChannel> =
        catalogDaoFacade.channels.getAllOrdered(sourceId).map { it.toDomain() }

    suspend fun getAdjacentChannel(
        sourceId: String,
        currentChannelId: String,
        direction: ChannelDirection,
    ): CatalogChannel? {
        val channels = getOrderedChannels(sourceId)
        if (channels.isEmpty()) return null
        val index = channels.indexOfFirst { it.id == currentChannelId }
        val currentIndex = if (index >= 0) index else 0
        val nextIndex = when (direction) {
            ChannelDirection.PREVIOUS -> (currentIndex - 1).coerceAtLeast(0)
            ChannelDirection.NEXT -> (currentIndex + 1).coerceAtMost(channels.lastIndex)
        }
        if (nextIndex == currentIndex && index >= 0) return null
        return channels.getOrNull(nextIndex)
    }

    suspend fun getMovie(sourceId: String, movieId: String): CatalogMovie? =
        catalogDaoFacade.movies.getById(sourceId, movieId)?.toDomain()

    suspend fun getSeries(sourceId: String, seriesId: String): CatalogSeries? =
        catalogDaoFacade.series.getById(sourceId, seriesId)?.toDomain()

    suspend fun getEpisode(sourceId: String, episodeId: String): CatalogEpisode? =
        catalogDaoFacade.episodes.getById(sourceId, episodeId)?.toDomain()

    suspend fun getEpisodesForSeries(sourceId: String, seriesId: String): List<CatalogEpisode> =
        catalogDaoFacade.episodes.getBySeries(sourceId, seriesId).map { it.toDomain() }

    suspend fun getRelatedMovies(
        sourceId: String,
        categoryId: String?,
        excludeMovieId: String,
        limit: Int = 12,
    ): List<CatalogMovie> {
        val fetchLimit = limit + 1
        val movies = if (!categoryId.isNullOrBlank()) {
            catalogDaoFacade.movies.getByCategory(sourceId, categoryId, fetchLimit)
        } else {
            catalogDaoFacade.movies.getFeatured(sourceId, fetchLimit)
        }
        return movies
            .filter { it.id != excludeMovieId }
            .take(limit)
            .map { it.toDomain() }
    }

    suspend fun upsertEpisodes(episodes: List<com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity>) {
        if (episodes.isNotEmpty()) {
            catalogDaoFacade.episodes.upsertAll(episodes)
        }
    }

    suspend fun replaceEpisodesForSeries(
        sourceId: String,
        seriesId: String,
        episodes: List<com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity>,
    ) {
        catalogDaoFacade.episodes.deleteBySeries(sourceId, seriesId)
        if (episodes.isNotEmpty()) {
            catalogDaoFacade.episodes.upsertAll(episodes)
        }
    }

    suspend fun resolveWatchHistoryPosterUrl(
        sourceId: String?,
        item: WatchHistoryItem,
        isDemoMode: Boolean = false,
    ): String? = resolveWatchHistoryCardDisplay(sourceId, item, isDemoMode).posterUrl

    suspend fun resolveWatchHistoryCardDisplay(
        sourceId: String?,
        item: WatchHistoryItem,
        isDemoMode: Boolean = false,
    ): WatchHistoryCardDisplay {
        if (isDemoMode) {
            return resolveDemoWatchHistoryCardDisplay(item)
        }
        return when (item.contentType) {
            WatchHistoryContentType.MOVIE -> {
                val movie = sourceId?.let { getMovie(it, item.contentId) }
                WatchHistoryCardDisplay(
                    title = movie?.title?.takeIf { it.isNotBlank() } ?: item.title,
                    subtitle = null,
                    posterUrl = item.posterUrl?.takeIf { it.isNotBlank() } ?: movie?.posterUrl,
                )
            }
            WatchHistoryContentType.EPISODE -> {
                val episode = sourceId?.let { getEpisode(it, item.contentId) }
                val series = sourceId?.let { sid ->
                    item.seriesId?.let { seriesId -> getSeries(sid, seriesId) }
                        ?: episode?.seriesId?.let { seriesId -> getSeries(sid, seriesId) }
                }
                val episodeTitle = episode?.title?.takeIf { it.isNotBlank() } ?: item.title
                WatchHistoryCardDisplay(
                    title = series?.title?.takeIf { it.isNotBlank() } ?: episodeTitle,
                    subtitle = if (series != null) {
                        formatEpisodeSubtitle(
                            seasonNumber = episode?.seasonNumber,
                            episodeNumber = episode?.episodeNumber,
                            title = episodeTitle,
                        )
                    } else {
                        null
                    },
                    posterUrl = item.posterUrl?.takeIf { it.isNotBlank() }
                        ?: episode?.thumbnailUrl?.takeIf { it.isNotBlank() }
                        ?: series?.posterUrl,
                )
            }
            WatchHistoryContentType.CHANNEL -> {
                val channel = sourceId?.let { getChannel(it, item.contentId) }
                WatchHistoryCardDisplay(
                    title = channel?.name?.takeIf { it.isNotBlank() } ?: item.title,
                    subtitle = null,
                    posterUrl = item.posterUrl?.takeIf { it.isNotBlank() } ?: channel?.logoUrl,
                )
            }
        }
    }

    private fun resolveDemoWatchHistoryCardDisplay(item: WatchHistoryItem): WatchHistoryCardDisplay = when (item.contentType) {
        WatchHistoryContentType.MOVIE -> {
            val movie = FakeDataProvider.movieById(item.contentId)
            WatchHistoryCardDisplay(
                title = movie?.title ?: item.title,
                posterUrl = item.posterUrl?.takeIf { it.isNotBlank() } ?: movie?.imageUrl,
            )
        }
        WatchHistoryContentType.EPISODE -> {
            val series = item.seriesId?.let { FakeDataProvider.seriesById(it) }
                ?: FakeDataProvider.seriesList.firstOrNull { candidate ->
                    candidate.seasons.any { season ->
                        season.episodes.any { episode -> episode.id == item.contentId }
                    }
                }
            val episodeMatch = series?.seasons?.firstNotNullOfOrNull { season ->
                season.episodes.find { it.id == item.contentId }?.let { episode ->
                    season.seasonNumber to episode
                }
            }
            val episode = episodeMatch?.second
            val episodeTitle = episode?.title?.takeIf { it.isNotBlank() } ?: item.title
            WatchHistoryCardDisplay(
                title = series?.title ?: episodeTitle,
                subtitle = if (series != null) {
                    formatEpisodeSubtitle(
                        seasonNumber = episodeMatch?.first,
                        episodeNumber = episode?.episodeNumber,
                        title = episodeTitle,
                    )
                } else {
                    null
                },
                posterUrl = resolveDemoWatchHistoryPosterUrl(item),
            )
        }
        WatchHistoryContentType.CHANNEL -> {
            val channel = FakeDataProvider.channels.find { it.id == item.contentId }
            WatchHistoryCardDisplay(
                title = channel?.name ?: item.title,
                posterUrl = item.posterUrl?.takeIf { it.isNotBlank() } ?: channel?.logoUrl,
            )
        }
    }

    private fun formatEpisodeSubtitle(
        seasonNumber: Int?,
        episodeNumber: Int?,
        title: String,
    ): String {
        val prefix = if (seasonNumber != null && episodeNumber != null && episodeNumber > 0) {
            "S${seasonNumber}E$episodeNumber"
        } else {
            null
        }
        return when {
            prefix != null && title.isNotBlank() -> "$prefix · $title"
            prefix != null -> prefix
            else -> title
        }
    }

    private fun resolveDemoWatchHistoryPosterUrl(item: WatchHistoryItem): String? = when (item.contentType) {
        WatchHistoryContentType.MOVIE -> FakeDataProvider.movieById(item.contentId)?.imageUrl
        WatchHistoryContentType.EPISODE -> {
            item.seriesId?.let { FakeDataProvider.seriesById(it)?.imageUrl }
                ?: FakeDataProvider.seriesList.firstOrNull { series ->
                    series.seasons.any { season ->
                        season.episodes.any { episode -> episode.id == item.contentId }
                    }
                }?.imageUrl
        }
        WatchHistoryContentType.CHANNEL ->
            FakeDataProvider.channels.find { it.id == item.contentId }?.logoUrl
    }

    suspend fun getCurrentProgram(sourceId: String, channelId: String): com.iptvcinema.tv.core.model.catalog.CatalogProgram? {
        val nowMs = System.currentTimeMillis()
        return catalogDaoFacade.programs.getUpcomingForChannel(sourceId, channelId, nowMs)
            .firstOrNull { it.startEpochMs <= nowMs && it.endEpochMs > nowMs }
            ?.toDomain()
            ?: catalogDaoFacade.programs.getUpcomingForChannel(sourceId, channelId, nowMs)
                .firstOrNull()
                ?.toDomain()
    }

    private fun observeChannelBrowse(
        sourceId: String?,
        categoryName: String?,
        contentType: CatalogContentType,
    ): Flow<CatalogBrowseState<ChannelItem>> {
        if (sourceId == null) {
            return flowOf(CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_source_connected)))
        }
        return catalogDaoFacade.categories.observeByType(sourceId, contentType.name).flatMapLatest { categories ->
            val categoryNames = categories.map { it.name }
            val selectedCategory = categories.firstOrNull { it.name == categoryName }
                ?: categories.firstOrNull()
            val channelFlow = if (selectedCategory == null) {
                catalogDaoFacade.channels.observeAll(sourceId)
            } else {
                catalogDaoFacade.channels.observeByCategory(sourceId, selectedCategory.id)
            }
            channelFlow.flatMapLatest { channels ->
                flow {
                    val nowMs = System.currentTimeMillis()
                    val currentPrograms = if (channels.isNotEmpty()) {
                        getCurrentProgramsForChannels(sourceId, channels.map { it.id }, nowMs)
                    } else {
                        emptyMap()
                    }
                    val state = if (categories.isEmpty() && channels.isEmpty()) {
                        CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_live_channels))
                    } else {
                        CatalogBrowseState(
                            loadState = CatalogLoadState.Ready,
                            categories = categoryNames.ifEmpty { listOf("All Channels") },
                            items = channels.map { channel ->
                                with(CatalogUiMapper) {
                                    channel.toDomain().toChannelItem(
                                        currentProgram = currentPrograms[channel.id],
                                        nowMs = nowMs,
                                        noProgramInfoTitle = appStrings.get(R.string.msg_no_program_info),
                                    )
                                }
                            },
                        )
                    }
                    emit(state)
                }
            }
        }
    }

    private fun observeMovieBrowse(
        sourceId: String?,
        categoryName: String?,
    ): Flow<CatalogBrowseState<MovieItem>> {
        if (sourceId == null) {
            return flowOf(CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_source_connected)))
        }
        return catalogDaoFacade.categories.observeByType(sourceId, CatalogContentType.VOD.name).flatMapLatest { categories ->
            val categoryNames = categories.map { it.name }
            val selectedCategory = categoryName?.let { name ->
                categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
            } ?: categories.firstOrNull()
            val movieFlow = when {
                selectedCategory == null -> catalogDaoFacade.movies.observeAllLimited(sourceId, MOVIE_BROWSE_LIMIT)
                isRecentlyAddedCategory(selectedCategory.name) ->
                    catalogDaoFacade.movies.observeRecentlyAdded(sourceId, LAST_ADDED_BROWSE_LIMIT)
                else -> catalogDaoFacade.movies.observeByCategoryLimited(
                    sourceId,
                    selectedCategory.id,
                    MOVIE_BROWSE_LIMIT,
                )
            }
            movieFlow.map { movies ->
                if (categories.isEmpty() && movies.isEmpty()) {
                    CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_movies_synced))
                } else {
                    CatalogBrowseState(
                        loadState = CatalogLoadState.Ready,
                        categories = categoryNames.ifEmpty { listOf("All Movies") },
                        items = movies.map { movie ->
                            with(CatalogUiMapper) { movie.toDomain().toMovieItem() }
                        },
                    )
                }
            }
        }
    }

    private fun observeSeriesBrowse(
        sourceId: String?,
        categoryName: String?,
    ): Flow<CatalogBrowseState<SeriesItem>> {
        if (sourceId == null) {
            return flowOf(CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_source_connected)))
        }
        return catalogDaoFacade.categories.observeByType(sourceId, CatalogContentType.SERIES.name).flatMapLatest { categories ->
            val categoryNames = categories.map { it.name }
            val selectedCategory = categories.firstOrNull { it.name == categoryName }
                ?: categories.firstOrNull()
            val seriesFlow = if (selectedCategory == null) {
                catalogDaoFacade.series.observeAll(sourceId)
            } else {
                catalogDaoFacade.series.observeByCategory(sourceId, selectedCategory.id)
            }
            seriesFlow.map { seriesItems ->
                if (categories.isEmpty() && seriesItems.isEmpty()) {
                    CatalogBrowseState(CatalogLoadState.Empty, message = appStrings.get(R.string.msg_no_series_synced))
                } else {
                    CatalogBrowseState(
                        loadState = CatalogLoadState.Ready,
                        categories = categoryNames.ifEmpty { listOf("All Series") },
                        items = seriesItems.map { series ->
                            with(CatalogUiMapper) { series.toDomain().toSeriesItem() }
                        },
                    )
                }
            }
        }
    }

    private fun buildDemoFeatured(): FeaturedCatalogContent = FeaturedCatalogContent(
        heroMovies = FakeDataProvider.movies.take(5).map { movie ->
            CatalogMovie(
                id = movie.id,
                sourceId = "demo",
                title = movie.title,
                streamUrl = "",
                posterUrl = movie.imageUrl,
                backdropUrl = movie.backdropUrl,
                categoryId = null,
                categoryName = null,
                year = movie.year,
                durationMinutes = movie.runtimeMinutes,
                rating = movie.rating,
                plot = movie.plot,
                genres = movie.genres,
            )
        },
        continueWatchingMovies = FakeDataProvider.continueWatchingMovies().map { movie ->
            CatalogMovie(
                id = movie.id,
                sourceId = "demo",
                title = movie.title,
                streamUrl = "",
                posterUrl = movie.imageUrl,
                backdropUrl = movie.backdropUrl,
                categoryId = null,
                categoryName = null,
                year = movie.year,
                durationMinutes = movie.runtimeMinutes,
                rating = movie.rating,
                plot = movie.plot,
                genres = movie.genres,
            )
        },
        trendingMovies = FakeDataProvider.movies.take(8).map { movie ->
            com.iptvcinema.tv.core.model.catalog.CatalogMovie(
                id = movie.id,
                sourceId = "demo",
                title = movie.title,
                streamUrl = "",
                posterUrl = movie.imageUrl,
                backdropUrl = movie.backdropUrl,
                categoryId = null,
                categoryName = null,
                year = movie.year,
                durationMinutes = movie.runtimeMinutes,
                rating = movie.rating,
                plot = movie.plot,
                genres = movie.genres,
            )
        },
        liveChannels = FakeDataProvider.channels.take(8).map { channel ->
            CatalogChannel(
                id = channel.id,
                sourceId = "demo",
                name = channel.name,
                streamUrl = "",
                logoUrl = channel.logoUrl,
                categoryId = null,
                categoryName = channel.category,
                tvgId = null,
                channelNumber = channel.channelNumber,
            )
        },
        newReleaseMovies = FakeDataProvider.movies.takeLast(6).map { movie ->
            com.iptvcinema.tv.core.model.catalog.CatalogMovie(
                id = movie.id,
                sourceId = "demo",
                title = movie.title,
                streamUrl = "",
                posterUrl = movie.imageUrl,
                backdropUrl = movie.backdropUrl,
                categoryId = null,
                categoryName = null,
                year = movie.year,
                durationMinutes = movie.runtimeMinutes,
                rating = movie.rating,
                plot = movie.plot,
                genres = movie.genres,
            )
        },
        featuredSeries = FakeDataProvider.seriesList.take(8).map { series ->
            CatalogSeries(
                id = series.id,
                sourceId = "demo",
                title = series.title,
                posterUrl = series.imageUrl,
                backdropUrl = series.backdropUrl,
                categoryId = null,
                categoryName = series.genres.firstOrNull(),
                plot = series.plot,
                rating = series.rating,
                year = series.year,
            )
        },
    )

    private fun isRecentlyAddedCategory(name: String): Boolean {
        val normalized = name.trim()
        return normalized.equals("Last Added", ignoreCase = true) ||
            normalized.equals("Recently Added", ignoreCase = true)
    }

    private fun observeSourceMetaInternal(): Flow<Pair<SourceStatus?, SourceType?>> = observeSourceMeta()

    private fun <T> Flow<CatalogBrowseState<T>>.withSourceContext(): Flow<CatalogBrowseState<T>> =
        combine(this, observeSourceMetaInternal()) { state, (status, type) ->
            state.copy(sourceStatus = status, sourceType = type)
        }

    private companion object {
        const val MOVIE_BROWSE_LIMIT = 100
        const val LAST_ADDED_BROWSE_LIMIT = 20
    }
}
