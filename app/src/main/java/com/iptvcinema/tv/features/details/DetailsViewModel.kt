package com.iptvcinema.tv.features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.CastMember
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.catalog.CatalogMovie
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository
import com.iptvcinema.tv.core.player.WatchHistoryResumePolicy
import com.iptvcinema.tv.core.util.AppStrings
import com.iptvcinema.tv.core.util.CastParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class DetailsLoadState {
    Loading,
    Ready,
    Error,
}

data class DetailsContinueWatching(
    val contentId: String,
    val resumePositionMs: Long = 0L,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

data class MovieDetailsUiState(
    val loadState: DetailsLoadState = DetailsLoadState.Loading,
    val movie: MovieItem? = null,
    val catalogMovie: CatalogMovie? = null,
    val cast: List<CastMember> = emptyList(),
    val relatedMovies: List<MovieItem> = emptyList(),
    val continueWatching: DetailsContinueWatching? = null,
    val isDemoMode: Boolean = false,
    val message: String? = null,
    val playbackBlocked: Boolean = false,
)

data class SeriesDetailsUiState(
    val loadState: DetailsLoadState = DetailsLoadState.Loading,
    val series: SeriesItem? = null,
    val catalogSeries: CatalogSeries? = null,
    val seasons: List<SeasonItem> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val relatedSeries: List<SeriesItem> = emptyList(),
    val continueWatching: DetailsContinueWatching? = null,
    val episodesLoading: Boolean = false,
    val isDemoMode: Boolean = false,
    val message: String? = null,
    val playbackBlocked: Boolean = false,
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val favoritesRepository: FavoritesRepository,
    private val catalogRepository: CatalogRepository,
    private val episodeCatalogRepository: EpisodeCatalogRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
    private val appStrings: AppStrings,
) : ViewModel() {
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _movieUiState = MutableStateFlow(MovieDetailsUiState())
    val movieUiState: StateFlow<MovieDetailsUiState> = _movieUiState.asStateFlow()

    private val _seriesUiState = MutableStateFlow(SeriesDetailsUiState())
    val seriesUiState: StateFlow<SeriesDetailsUiState> = _seriesUiState.asStateFlow()

    fun loadMovieDetails(movieId: String) {
        viewModelScope.launch {
            _movieUiState.value = MovieDetailsUiState(loadState = DetailsLoadState.Loading)
            val session = appSessionRepository.sessionState.first()
            if (session.isDemoMode) {
                val movie = FakeDataProvider.movieById(movieId) ?: FakeDataProvider.movies.firstOrNull()
                _movieUiState.value = if (movie == null) {
                    MovieDetailsUiState(loadState = DetailsLoadState.Error, message = appStrings.get(R.string.error_movie_not_found))
                } else {
                    MovieDetailsUiState(
                        loadState = DetailsLoadState.Ready,
                        movie = movie,
                        isDemoMode = true,
                    )
                }
                return@launch
            }
            val sourceId = session.currentSourceId
            if (sourceId == null) {
                _movieUiState.value = MovieDetailsUiState(
                    loadState = DetailsLoadState.Error,
                    message = appStrings.get(R.string.msg_no_source_connected),
                )
                return@launch
            }
            val catalogMovie = catalogRepository.getMovie(sourceId, movieId)
            if (catalogMovie == null) {
                _movieUiState.value = MovieDetailsUiState(
                    loadState = DetailsLoadState.Error,
                    message = appStrings.get(R.string.error_movie_not_found),
                )
                return@launch
            }
            val related = catalogRepository.getRelatedMovies(
                sourceId = sourceId,
                categoryId = catalogMovie.categoryId,
                excludeMovieId = movieId,
            ).map { with(CatalogUiMapper) { it.toMovieItem() } }
            val controls = session.currentProfileId?.let { parentalControlsRepository.getControls(it) }
            val movieItem = catalogMovie.toMovieItem()
            val filteredRelated = if (controls != null) {
                related.filter { movie ->
                    !parentalGate.isContentBlocked(movie.genres.firstOrNull(), movie.rating, controls)
                }
            } else {
                related
            }
            val continueWatching = session.currentProfileId?.let { profileId ->
                loadMovieContinueWatching(profileId, movieId, catalogMovie)
            }
            _movieUiState.value = MovieDetailsUiState(
                loadState = DetailsLoadState.Ready,
                movie = movieItem,
                catalogMovie = catalogMovie,
                cast = CastParser.parseCastMembers(catalogMovie.cast),
                relatedMovies = filteredRelated,
                continueWatching = continueWatching,
                playbackBlocked = controls != null &&
                    parentalGate.isContentBlocked(
                        movieItem.genres.firstOrNull(),
                        movieItem.rating,
                        controls,
                    ),
            )
        }
    }

    fun loadSeriesDetails(seriesId: String) {
        viewModelScope.launch {
            _seriesUiState.value = SeriesDetailsUiState(
                loadState = DetailsLoadState.Loading,
                episodesLoading = true,
            )
            val session = appSessionRepository.sessionState.first()
            if (session.isDemoMode) {
                val series = FakeDataProvider.seriesById(seriesId) ?: FakeDataProvider.seriesList.firstOrNull()
                _seriesUiState.value = if (series == null) {
                    SeriesDetailsUiState(loadState = DetailsLoadState.Error, message = appStrings.get(R.string.error_series_not_found))
                } else {
                    SeriesDetailsUiState(
                        loadState = DetailsLoadState.Ready,
                        series = series,
                        seasons = series.seasons,
                        isDemoMode = true,
                    )
                }
                return@launch
            }
            val sourceId = session.currentSourceId
            if (sourceId == null) {
                _seriesUiState.value = SeriesDetailsUiState(
                    loadState = DetailsLoadState.Error,
                    message = appStrings.get(R.string.msg_no_source_connected),
                )
                return@launch
            }
            val catalogSeries = catalogRepository.getSeries(sourceId, seriesId)
            if (catalogSeries == null) {
                _seriesUiState.value = SeriesDetailsUiState(
                    loadState = DetailsLoadState.Error,
                    message = appStrings.get(R.string.error_series_not_found),
                )
                return@launch
            }
            _seriesUiState.value = SeriesDetailsUiState(
                loadState = DetailsLoadState.Ready,
                series = catalogSeries.toSeriesItem(seasons = emptyList()),
                catalogSeries = catalogSeries,
                episodesLoading = true,
            )
            val catalogResult = episodeCatalogRepository.getSeriesEpisodeCatalog(
                sourceId = sourceId,
                seriesId = seriesId,
                forceRefresh = true,
            )
            val seasons = DetailsSeasonGrouping.toSeasonItems(
                episodes = catalogResult.episodes,
                seriesId = seriesId,
                additionalSeasonNumbers = catalogResult.seasonNumbers,
            )
            val genres = catalogResult.genres.ifEmpty {
                listOfNotNull(catalogSeries.categoryName?.takeIf { it.isNotBlank() })
            }
            val seriesItem = catalogSeries.toSeriesItem(
                seasons = seasons,
                plotOverride = catalogResult.plot,
                ratingOverride = catalogResult.rating,
                genres = genres,
            )
            val controls = session.currentProfileId?.let { parentalControlsRepository.getControls(it) }
            val relatedSeries = catalogRepository.getRelatedSeries(
                sourceId = sourceId,
                categoryId = catalogSeries.categoryId,
                excludeSeriesId = seriesId,
            ).map { with(CatalogUiMapper) { it.toSeriesItem() } }
            val filteredRelatedSeries = if (controls != null) {
                relatedSeries.filter { item ->
                    !parentalGate.isContentBlocked(item.genres.firstOrNull(), item.rating, controls)
                }
            } else {
                relatedSeries
            }
            val continueWatching = session.currentProfileId?.let { profileId ->
                loadSeriesContinueWatching(profileId, sourceId, seriesId, seasons)
            }
            _seriesUiState.value = SeriesDetailsUiState(
                loadState = DetailsLoadState.Ready,
                series = seriesItem,
                catalogSeries = catalogSeries,
                seasons = seasons,
                cast = catalogResult.cast,
                relatedSeries = filteredRelatedSeries,
                continueWatching = continueWatching,
                episodesLoading = false,
                playbackBlocked = controls != null &&
                    parentalGate.isContentBlocked(
                        seriesItem.genres.firstOrNull(),
                        seriesItem.rating,
                        controls,
                    ),
            )
        }
    }

    fun loadFavoriteState(contentId: String, contentType: FavoriteContentType) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            _isFavorite.value = favoritesRepository.isFavorite(profileId, contentId, contentType)
        }
    }

    fun toggleFavorite(
        contentId: String,
        contentType: FavoriteContentType,
        title: String,
        posterUrl: String?,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
            val session = appSessionRepository.sessionState.first()
            runCatching {
                val isFavorite = favoritesRepository.toggleFavorite(
                    profileId = profileId,
                    contentId = contentId,
                    contentType = contentType,
                    title = title,
                    posterUrl = posterUrl,
                    sourceId = session.currentSourceId,
                )
                _isFavorite.value = isFavorite
                onResult(isFavorite)
            }
        }
    }

    private suspend fun loadMovieContinueWatching(
        profileId: String,
        movieId: String,
        catalogMovie: CatalogMovie,
    ): DetailsContinueWatching? {
        val history = watchHistoryRepository.getProgress(
            profileId = profileId,
            contentId = movieId,
            contentType = WatchHistoryContentType.MOVIE,
        ) ?: return null
        val durationMs = history.durationMs ?: catalogMovie.durationMinutes?.times(60_000L) ?: return null
        if (!WatchHistoryResumePolicy.isContinueWatching(history.positionMs, durationMs)) return null
        val resumeMs = WatchHistoryResumePolicy.resumePositionMs(history.positionMs, durationMs)
        if (resumeMs <= 0L) return null
        return DetailsContinueWatching(
            contentId = movieId,
            resumePositionMs = resumeMs,
        )
    }

    private suspend fun loadSeriesContinueWatching(
        profileId: String,
        sourceId: String,
        seriesId: String,
        seasons: List<SeasonItem>,
    ): DetailsContinueWatching? {
        val historyItems = watchHistoryRepository.observeHistory(profileId, limit = 50).first()
        val episodeHistory = historyItems
            .filter { item ->
                item.contentType == WatchHistoryContentType.EPISODE &&
                    item.seriesId == seriesId &&
                    WatchHistoryResumePolicy.isContinueWatching(item.positionMs, item.durationMs)
            }
            .maxByOrNull { it.lastWatchedAt }
            ?: return null
        val durationMs = episodeHistory.durationMs ?: return null
        val resumeMs = WatchHistoryResumePolicy.resumePositionMs(episodeHistory.positionMs, durationMs)
        if (resumeMs <= 0L) return null
        val catalogEpisode = catalogRepository.getEpisode(sourceId, episodeHistory.contentId)
        val seasonMatch = if (catalogEpisode == null) {
            seasons.firstNotNullOfOrNull { season ->
                season.episodes.find { it.id == episodeHistory.contentId }?.let {
                    season.seasonNumber to it.episodeNumber
                }
            }
        } else {
            null
        }
        return DetailsContinueWatching(
            contentId = episodeHistory.contentId,
            resumePositionMs = resumeMs,
            seasonNumber = catalogEpisode?.seasonNumber ?: seasonMatch?.first,
            episodeNumber = catalogEpisode?.episodeNumber ?: seasonMatch?.second,
        )
    }
}

private fun CatalogMovie.toMovieItem(): MovieItem = MovieItem(
    id = id,
    title = title,
    year = year ?: 0,
    runtimeMinutes = durationMinutes ?: 0,
    rating = rating.orEmpty(),
    plot = plot.orEmpty(),
    genres = genres,
    is4K = false,
    imageUrl = posterUrl,
    backdropUrl = backdropUrl ?: posterUrl,
)

private fun CatalogSeries.toSeriesItem(
    seasons: List<SeasonItem>,
    plotOverride: String? = null,
    ratingOverride: String? = null,
    genres: List<String> = emptyList(),
): SeriesItem = SeriesItem(
    id = id,
    title = title,
    year = year ?: 0,
    rating = ratingOverride?.takeIf { it.isNotBlank() } ?: rating.orEmpty(),
    plot = plotOverride?.takeIf { it.isNotBlank() } ?: plot.orEmpty(),
    genres = genres,
    seasonCount = seasons.size,
    is4K = false,
    imageUrl = posterUrl,
    backdropUrl = backdropUrl ?: posterUrl,
    seasons = seasons,
)
