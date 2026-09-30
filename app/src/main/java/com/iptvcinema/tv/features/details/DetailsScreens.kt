package com.iptvcinema.tv.features.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.design.components.DetailHero
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.SkeletonDetailHero
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.util.youtubeTrailerUrl

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    movieId: String,
    navController: NavController,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.movieUiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val context = LocalContext.current
    val feedbackRatingBlocked = stringResource(R.string.feedback_rating_blocked)
    val feedbackAddedToMyList = stringResource(R.string.feedback_added_to_mylist)
    val feedbackRemovedFromMyList = stringResource(R.string.feedback_removed_from_mylist)
    val watchNowFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("movie_details")

    LaunchedEffect(movieId) {
        viewModel.loadMovieDetails(movieId)
        viewModel.loadFavoriteState(movieId, FavoriteContentType.MOVIE)
    }

    PopBackHandler(onBack = { navController.popBackStack() })

    LaunchedEffect(focusState.hasSavedFocus, uiState.loadState) {
        if (uiState.loadState == DetailsLoadState.Ready && focusState.hasSavedFocus) {
            focusState.restoreFocus(watchNowFocus)
        } else if (uiState.loadState == DetailsLoadState.Ready) {
            focusState.requestInitialFocus(watchNowFocus)
            focusState.saveFocusIndex(0)
        }
    }

    when (uiState.loadState) {
        DetailsLoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaColors.Background),
            ) {
                SkeletonDetailHero(modifier = Modifier.fillMaxSize())
            }
        }
        DetailsLoadState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaColors.Background),
            ) {
                EmptyState(
                    title = stringResource(R.string.error_movie_unavailable),
                    description = uiState.message ?: stringResource(R.string.error_movie_not_found),
                    primaryAction = stringResource(R.string.btn_back),
                    secondaryAction = null,
                    onPrimary = { navController.popBackStack() },
                    onSecondary = null,
                )
            }
        }
        DetailsLoadState.Ready -> {
            val movie = uiState.movie ?: return
            val trailerUrl = youtubeTrailerUrl(uiState.catalogMovie?.youtubeTrailer)
            val continueWatching = uiState.continueWatching
            val primaryActionLabel = if (continueWatching != null) {
                stringResource(R.string.btn_continue_watching)
            } else {
                stringResource(R.string.btn_play_movie)
            }
            val hasRelatedMovies = if (uiState.isDemoMode) {
                FakeDataProvider.samplePosters().isNotEmpty()
            } else {
                uiState.relatedMovies.isNotEmpty()
            }
            DetailHero(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaColors.Background),
                title = movie.title,
                metadata = listOfNotNull(
                    movie.year.takeIf { it > 0 }?.toString(),
                    movie.runtimeMinutes.takeIf { it > 0 }?.let {
                        stringResource(R.string.details_runtime_hours_minutes, it / 60, it % 60)
                    },
                    if (movie.is4K) stringResource(R.string.badge_4k) else "HD",
                    movie.rating.takeIf { it.isNotBlank() }?.let {
                        stringResource(R.string.details_rating, it)
                    },
                    movie.genres.joinToString(" ").takeIf { it.isNotBlank() },
                ),
                synopsis = movie.plot,
                primaryActionLabel = primaryActionLabel,
                onWatchNow = {
                    if (uiState.playbackBlocked) {
                        android.widget.Toast.makeText(
                            context,
                            feedbackRatingBlocked,
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        navController.navigate(
                            AppRoute.player(
                                contentId = continueWatching?.contentId ?: movie.id,
                                contentType = "movie",
                                resumePositionMs = continueWatching?.resumePositionMs,
                            ),
                        )
                    }
                },
                onFavorite = {
                    viewModel.toggleFavorite(
                        contentId = movie.id,
                        contentType = FavoriteContentType.MOVIE,
                        title = movie.title,
                        posterUrl = movie.imageUrl,
                    ) { added ->
                        val message = if (added) feedbackAddedToMyList else feedbackRemovedFromMyList
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                isFavorite = isFavorite,
                showTrailer = trailerUrl != null,
                onTrailer = trailerUrl?.let { url ->
                    {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                onMoreLikeThis = if (hasRelatedMovies) {
                    { navController.navigate(AppRoute.movieRelated(movieId)) }
                } else {
                    null
                },
                showScrollHint = hasRelatedMovies,
                backdropUrl = movie.backdropUrl ?: movie.imageUrl,
                watchNowFocusRequester = watchNowFocus,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    navController: NavController,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.seriesUiState.collectAsState()
    val seasons = uiState.seasons
    val firstEpisodeId = seasons.firstOrNull()?.episodes?.firstOrNull()?.id
    val isFavorite by viewModel.isFavorite.collectAsState()
    val context = LocalContext.current
    val feedbackRatingBlocked = stringResource(R.string.feedback_rating_blocked)
    val feedbackAddedToMyList = stringResource(R.string.feedback_added_to_mylist)
    val feedbackRemovedFromMyList = stringResource(R.string.feedback_removed_from_mylist)
    val watchNowFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("series_details")

    LaunchedEffect(seriesId) {
        viewModel.loadSeriesDetails(seriesId)
        viewModel.loadFavoriteState(seriesId, FavoriteContentType.SERIES)
    }

    PopBackHandler(onBack = { navController.popBackStack() })

    LaunchedEffect(focusState.hasSavedFocus, uiState.loadState) {
        if (uiState.loadState == DetailsLoadState.Ready && focusState.hasSavedFocus) {
            focusState.restoreFocus(watchNowFocus)
        } else if (uiState.loadState == DetailsLoadState.Ready) {
            focusState.requestInitialFocus(watchNowFocus)
            focusState.saveFocusIndex(0)
        }
    }

    when (uiState.loadState) {
        DetailsLoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaColors.Background),
            ) {
                SkeletonDetailHero(modifier = Modifier.fillMaxSize())
            }
        }
        DetailsLoadState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaColors.Background),
            ) {
                EmptyState(
                    title = stringResource(R.string.error_series_unavailable),
                    description = uiState.message ?: stringResource(R.string.error_series_not_found),
                    primaryAction = stringResource(R.string.btn_back),
                    secondaryAction = null,
                    onPrimary = { navController.popBackStack() },
                    onSecondary = null,
                )
            }
        }
        DetailsLoadState.Ready -> {
            val series = uiState.series ?: return
            val trailerUrl = youtubeTrailerUrl(uiState.catalogSeries?.youtubeTrailer)
            val continueWatching = uiState.continueWatching
            val primaryActionLabel = when {
                continueWatching?.seasonNumber != null && continueWatching.episodeNumber != null -> {
                    stringResource(
                        R.string.btn_continue_series,
                        continueWatching.seasonNumber,
                        continueWatching.episodeNumber,
                    )
                }
                else -> stringResource(R.string.btn_play_series)
            }
            val hasEpisodes = seasons.any { it.episodes.isNotEmpty() }
            DetailHero(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaColors.Background),
                title = series.title,
                metadata = listOfNotNull(
                    series.year.takeIf { it > 0 }?.toString(),
                    if (series.seasonCount > 0) {
                        pluralStringResource(
                            R.plurals.details_season_count,
                            series.seasonCount,
                            series.seasonCount,
                        )
                    } else {
                        null
                    },
                    if (series.is4K) stringResource(R.string.badge_4k) else "HD",
                    series.rating.takeIf { it.isNotBlank() }?.let { stringResource(R.string.details_rating, it) },
                    series.genres.joinToString(" ").takeIf { it.isNotBlank() },
                ),
                synopsis = series.plot,
                primaryActionLabel = primaryActionLabel,
                onWatchNow = {
                    if (uiState.playbackBlocked) {
                        android.widget.Toast.makeText(context, feedbackRatingBlocked, android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val targetEpisodeId = continueWatching?.contentId ?: firstEpisodeId
                        targetEpisodeId?.let { episodeId ->
                            navController.navigate(
                                AppRoute.player(
                                    contentId = episodeId,
                                    contentType = "episode",
                                    seriesId = seriesId,
                                    resumePositionMs = continueWatching?.resumePositionMs,
                                ),
                            )
                        }
                    }
                },
                onFavorite = {
                    viewModel.toggleFavorite(
                        contentId = series.id,
                        contentType = FavoriteContentType.SERIES,
                        title = series.title,
                        posterUrl = series.imageUrl,
                    ) { added ->
                        val message = if (added) feedbackAddedToMyList else feedbackRemovedFromMyList
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                isFavorite = isFavorite,
                showTrailer = trailerUrl != null,
                onTrailer = trailerUrl?.let { url ->
                    {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                onChooseEpisode = if (hasEpisodes) {
                    { navController.navigate(AppRoute.seriesEpisodes(seriesId)) }
                } else {
                    null
                },
                onMoreLikeThis = if (uiState.isDemoMode || uiState.relatedSeries.isNotEmpty()) {
                    { navController.navigate(AppRoute.seriesRelated(seriesId)) }
                } else {
                    null
                },
                showScrollHint = hasEpisodes,
                backdropUrl = series.backdropUrl ?: series.imageUrl,
                watchNowFocusRequester = watchNowFocus,
            )
        }
    }
}
