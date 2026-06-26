package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.MovieItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(
    title: String,
    metadata: List<String>,
    description: String,
    onWatchNow: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    height: androidx.compose.ui.unit.Dp = CinemaSpacing.HeroMinHeight,
    carouselDotCount: Int = 0,
    selectedCarouselDot: Int = 0,
    watchNowFocusRequester: FocusRequester? = null,
    backdropUrl: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height, max = CinemaSpacing.HeroMaxHeight),
    ) {
        if (backdropUrl != null) {
            CinemaAsyncImage(
                imageUrl = backdropUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallbackLabel = title,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CinemaColors.Surface,
                                CinemaColors.Background,
                            ),
                        ),
                    ),
            )
        }

        // Bottom gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CinemaColors.Background.copy(alpha = 0.6f),
                            CinemaColors.Background,
                        ),
                    ),
                ),
        )

        // Left side gradient for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CinemaColors.Background.copy(alpha = 0.8f),
                            CinemaColors.Background.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.55f)
                .padding(
                    start = CinemaSpacing.NavRailWidth + 16.dp,
                    bottom = 28.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = CinemaColors.TextPrimary,
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (metadata.isNotEmpty()) {
                MetadataRow(items = metadata)
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                CinemaButton(
                    text = stringResource(R.string.btn_watch_now),
                    variant = CinemaButtonVariant.PrimaryAccent,
                    icon = Icons.Default.PlayArrow,
                    onClick = onWatchNow,
                    modifier = watchNowFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
                CinemaButton(
                    text = stringResource(R.string.btn_details),
                    variant = CinemaButtonVariant.SecondaryDark,
                    icon = Icons.Default.Info,
                    onClick = onDetails,
                )
            }
        }

        if (carouselDotCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = CinemaSpacing.ScreenPadding, bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(carouselDotCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == selectedCarouselDot) 16.dp else 6.dp,
                                height = 3.dp,
                            )
                            .clip(CinemaShapes.Small)
                            .background(
                                if (index == selectedCarouselDot) CinemaColors.White else CinemaColors.TextMuted.copy(alpha = 0.5f),
                            ),
                    )
                }
            }
        }
    }
}

private const val HeroCarouselAutoAdvanceMs = 8_000L

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroCarousel(
    movies: List<MovieItem>,
    onWatchNow: (MovieItem) -> Unit,
    onDetails: (MovieItem) -> Unit,
    modifier: Modifier = Modifier,
    watchNowFocusRequester: FocusRequester? = null,
) {
    if (movies.isEmpty()) return

    if (movies.size == 1) {
        val movie = movies.first()
        HeroBanner(
            title = movie.title,
            metadata = heroMovieMetadata(movie),
            description = movie.plot,
            onWatchNow = { onWatchNow(movie) },
            onDetails = { onDetails(movie) },
            modifier = modifier,
            watchNowFocusRequester = watchNowFocusRequester,
            backdropUrl = movie.backdropUrl ?: movie.imageUrl,
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { movies.size })
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, isFocused, movies.size) {
        if (isFocused) return@LaunchedEffect
        delay(HeroCarouselAutoAdvanceMs)
        val nextPage = (pagerState.currentPage + 1) % movies.size
        pagerState.animateScrollToPage(nextPage)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionRight -> {
                        if (pagerState.currentPage < movies.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false,
        ) { page ->
            val movie = movies[page]
            HeroBanner(
                title = movie.title,
                metadata = heroMovieMetadata(movie),
                description = movie.plot,
                onWatchNow = { onWatchNow(movie) },
                onDetails = { onDetails(movie) },
                modifier = Modifier.fillMaxWidth(),
                carouselDotCount = movies.size,
                selectedCarouselDot = pagerState.currentPage,
                watchNowFocusRequester = if (page == pagerState.currentPage) watchNowFocusRequester else null,
                backdropUrl = movie.backdropUrl ?: movie.imageUrl,
            )
        }
    }
}

private fun heroMovieMetadata(movie: MovieItem): List<String> = listOfNotNull(
    movie.year.takeIf { it > 0 }?.toString(),
    movie.genres.joinToString(" ").takeIf { it.isNotBlank() },
    movie.runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterGrid(
    items: List<PosterCardData>,
    onItemClick: (PosterCardData) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 5,
    firstItemFocusRequester: FocusRequester? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = CinemaSpacing.NavRailWidth + 16.dp,
                end = 24.dp,
                bottom = CinemaSpacing.SectionGap,
            ),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            ) {
                rowItems.forEach { item ->
                    PosterCard(
                        data = item,
                        onClick = { onItemClick(item) },
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (item == items.firstOrNull() && firstItemFocusRequester != null) {
                                    Modifier.focusRequester(firstItemFocusRequester)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                // Fill remaining columns with empty spacers so cards stay uniform
                repeat(columns - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileCard(
    name: String,
    initial: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.size(width = 140.dp, height = 180.dp),
        onClick = onClick,
        shape = CinemaShapes.Medium,
    ) { focused ->
        FocusableCardSurface(
            backgroundColor = if (focused || isSelected) CinemaColors.Surface else CinemaColors.SurfaceSoft,
            shape = CinemaShapes.Medium,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CinemaShapes.Medium)
                        .background(CinemaColors.SurfaceSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CinemaColors.White,
                        ),
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) CinemaColors.White else CinemaColors.TextSecondary,
                    ),
                )
            }
        }
    }
}
