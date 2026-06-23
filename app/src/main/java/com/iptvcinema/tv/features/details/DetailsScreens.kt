package com.iptvcinema.tv.features.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.design.components.CastCard
import com.iptvcinema.tv.core.design.components.ContentRail
import com.iptvcinema.tv.core.design.components.DetailHero
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.EpisodeCard
import com.iptvcinema.tv.core.design.components.LanguageChip
import com.iptvcinema.tv.core.design.components.PosterCard
import com.iptvcinema.tv.core.design.components.SeasonSelector
import com.iptvcinema.tv.core.design.components.SkeletonDetailHero
import com.iptvcinema.tv.core.design.components.SkeletonEpisodeList
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.util.rememberPrototypeFeedback

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    movieId: String,
    navController: NavController,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.movieUiState.collectAsState()
    var selectedLang by remember { mutableIntStateOf(0) }
    var selectedSub by remember { mutableIntStateOf(0) }
    val isFavorite by viewModel.isFavorite.collectAsState()
    val showFeedback = rememberPrototypeFeedback()
    val feedbackRatingBlocked = stringResource(R.string.feedback_rating_blocked)
    val feedbackTrailerSoon = stringResource(R.string.feedback_trailer_soon)
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
            MainShellScaffold(navController = navController, selectedNavItem = NavItem.Movies) {
                SkeletonDetailHero()
            }
        }
        DetailsLoadState.Error -> {
            MainShellScaffold(navController = navController, selectedNavItem = NavItem.Movies) {
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
            MainShellScaffold(
                navController = navController,
                selectedNavItem = NavItem.Movies,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    DetailHero(
                        title = movie.title,
                        metadata = listOfNotNull(
                            movie.year.takeIf { it > 0 }?.toString(),
                            movie.genres.joinToString(" ").takeIf { it.isNotBlank() },
                            movie.runtimeMinutes.takeIf { it > 0 }?.let {
                                stringResource(R.string.details_runtime_hours_minutes, it / 60, it % 60)
                            },
                            if (movie.is4K) stringResource(R.string.badge_4k) else "HD",
                            movie.rating.takeIf { it.isNotBlank() }?.let { stringResource(R.string.details_rating, it) },
                        ),
                        synopsis = movie.plot,
                        onWatchNow = {
                            if (uiState.playbackBlocked) {
                                showFeedback(feedbackRatingBlocked)
                            } else {
                                navController.navigate(AppRoute.player(movie.id, "movie"))
                            }
                        },
                        onTrailer = { showFeedback(feedbackTrailerSoon) },
                        onFavorite = {
                            viewModel.toggleFavorite(
                                contentId = movie.id,
                                contentType = FavoriteContentType.MOVIE,
                                title = movie.title,
                                posterUrl = movie.imageUrl,
                            ) { added ->
                                showFeedback(if (added) feedbackAddedToMyList else feedbackRemovedFromMyList)
                            }
                        },
                        isFavorite = isFavorite,
                        backdropUrl = movie.backdropUrl ?: movie.imageUrl,
                        watchNowFocusRequester = watchNowFocus,
                    )
                    if (uiState.isDemoMode) {
                        DemoCastSection()
                        DemoLanguagesSection(selectedLang) { selectedLang = it }
                        DemoSubtitlesSection(selectedSub) { selectedSub = it }
                        ContentRail(title = stringResource(R.string.rail_more_like_this), items = FakeDataProvider.samplePosters()) { poster ->
                            PosterCard(
                                data = poster,
                                onClick = {
                                    FakeDataProvider.movies.find { it.title == poster.title }?.let {
                                        navController.navigate(AppRoute.movieDetails(it.id))
                                    }
                                },
                            )
                        }
                    } else {
                        val cast = uiState.cast
                        if (cast.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.details_cast),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap)) {
                                cast.forEach { CastCard(member = it) }
                            }
                        }
                        if (uiState.relatedMovies.isNotEmpty()) {
                            ContentRail(title = stringResource(R.string.rail_more_like_this), items = uiState.relatedMovies) { related ->
                                PosterCard(
                                    data = related.toPosterCardData(),
                                    onClick = { navController.navigate(AppRoute.movieDetails(related.id)) },
                                )
                            }
                        }
                    }
                }
            }
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
    var selectedSeason by remember(seasons) { mutableIntStateOf(seasons.firstOrNull()?.seasonNumber ?: 1) }
    var selectedLang by remember { mutableIntStateOf(0) }
    val episodes = seasons.find { it.seasonNumber == selectedSeason }?.episodes.orEmpty()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val showFeedback = rememberPrototypeFeedback()
    val feedbackRatingBlocked = stringResource(R.string.feedback_rating_blocked)
    val feedbackTrailerSoon = stringResource(R.string.feedback_trailer_soon)
    val feedbackAddedToMyList = stringResource(R.string.feedback_added_to_mylist)
    val feedbackRemovedFromMyList = stringResource(R.string.feedback_removed_from_mylist)
    val watchNowFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("series_details")

    LaunchedEffect(seriesId) {
        viewModel.loadSeriesDetails(seriesId)
        viewModel.loadFavoriteState(seriesId, FavoriteContentType.SERIES)
    }

    LaunchedEffect(seasons) {
        if (seasons.isNotEmpty() && seasons.none { it.seasonNumber == selectedSeason }) {
            selectedSeason = seasons.first().seasonNumber
        }
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
            MainShellScaffold(navController = navController, selectedNavItem = NavItem.Series) {
                Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap)) {
                    SkeletonDetailHero()
                    SkeletonEpisodeList()
                }
            }
        }
        DetailsLoadState.Error -> {
            MainShellScaffold(navController = navController, selectedNavItem = NavItem.Series) {
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
            MainShellScaffold(
                navController = navController,
                selectedNavItem = NavItem.Series,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    DetailHero(
                        title = series.title,
                        metadata = listOfNotNull(
                            series.year.takeIf { it > 0 }?.toString(),
                            series.genres.joinToString(" ").takeIf { it.isNotBlank() },
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
                        ),
                        synopsis = series.plot,
                        primaryActionLabel = stringResource(R.string.btn_play_first),
                        onWatchNow = {
                            if (uiState.playbackBlocked) {
                                showFeedback(feedbackRatingBlocked)
                            } else {
                                episodes.firstOrNull()?.let {
                                    navController.navigate(AppRoute.player(it.id, "episode", seriesId))
                                }
                            }
                        },
                        onTrailer = { showFeedback(feedbackTrailerSoon) },
                        onFavorite = {
                            viewModel.toggleFavorite(
                                contentId = series.id,
                                contentType = FavoriteContentType.SERIES,
                                title = series.title,
                                posterUrl = series.imageUrl,
                            ) { added ->
                                showFeedback(if (added) feedbackAddedToMyList else feedbackRemovedFromMyList)
                            }
                        },
                        isFavorite = isFavorite,
                        backdropUrl = series.backdropUrl ?: series.imageUrl,
                        watchNowFocusRequester = watchNowFocus,
                    )
                    when {
                        uiState.episodesLoading -> {
                            SkeletonEpisodeList()
                        }
                        seasons.isNotEmpty() -> {
                            SeasonSelector(
                                seasons = seasons.map { it.seasonNumber },
                                selectedSeason = selectedSeason,
                                onSeasonSelected = { selectedSeason = it },
                            )
                            ContentRail(title = stringResource(R.string.rail_episodes), items = episodes) { episode ->
                                EpisodeCard(
                                    episodeNumber = episode.episodeNumber,
                                    title = episode.title,
                                    durationMinutes = episode.durationMinutes,
                                    progress = episode.progress,
                                    onClick = {
                                        if (uiState.playbackBlocked) {
                                            showFeedback(feedbackRatingBlocked)
                                        } else {
                                            navController.navigate(AppRoute.player(episode.id, "episode", seriesId))
                                        }
                                    },
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = stringResource(R.string.details_no_episodes),
                                style = MaterialTheme.typography.titleMedium.copy(color = CinemaColors.TextMuted),
                            )
                        }
                    }
                    if (uiState.isDemoMode) {
                        DemoCastSection()
                        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                            FakeDataProvider.languages.forEachIndexed { i, lang ->
                                LanguageChip(label = lang, isSelected = i == selectedLang, onClick = { selectedLang = i })
                            }
                        }
                    } else if (uiState.cast.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.details_cast),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap)) {
                            uiState.cast.forEach { CastCard(member = it) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DemoCastSection() {
    Text(
        text = stringResource(R.string.details_cast),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap)) {
        FakeDataProvider.cast.forEach { CastCard(member = it) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DemoLanguagesSection(selectedLang: Int, onSelected: (Int) -> Unit) {
    Text(text = stringResource(R.string.details_languages), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
        FakeDataProvider.languages.forEachIndexed { i, lang ->
            LanguageChip(label = lang, isSelected = i == selectedLang, onClick = { onSelected(i) })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DemoSubtitlesSection(selectedSub: Int, onSelected: (Int) -> Unit) {
    Text(text = stringResource(R.string.details_subtitles), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
        FakeDataProvider.subtitles.forEachIndexed { i, sub ->
            LanguageChip(label = sub, isSelected = i == selectedSub, onClick = { onSelected(i) })
        }
    }
}

private fun com.iptvcinema.tv.core.model.MovieItem.toPosterCardData() =
    com.iptvcinema.tv.core.design.components.PosterCardData(
        title = title,
        year = year.takeIf { it > 0 }?.toString(),
        runtime = runtimeMinutes.takeIf { it > 0 }?.let { "${it / 60}h ${it % 60}m" },
        imageUrl = imageUrl,
        is4K = is4K,
        contentId = id,
    )
