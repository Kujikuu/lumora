package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
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
            .heightIn(min = height, max = CinemaSpacing.HeroMaxHeight)
            .clip(CinemaShapes.Large)
            .border(1.dp, CinemaColors.Border, CinemaShapes.Large),
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
                                CinemaColors.BackgroundSoft,
                                CinemaColors.GoldDeep.copy(alpha = 0.45f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(420.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CinemaColors.GoldDeep.copy(alpha = 0.22f),
                                CinemaColors.GoldDeep.copy(alpha = 0.38f),
                            ),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CinemaColors.Background.copy(alpha = if (backdropUrl != null) 0.88f else 0.92f),
                            CinemaColors.Background.copy(alpha = if (backdropUrl != null) 0.62f else 0.55f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp)
                .fillMaxWidth(0.62f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.hero_featured),
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.GoldDeep),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        color = CinemaColors.GoldSoft,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium.copy(color = CinemaColors.GoldSoft),
                    )
                }
                MetadataRow(items = metadata)
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                CinemaButton(
                    text = stringResource(R.string.btn_watch_now),
                    variant = CinemaButtonVariant.PrimaryGold,
                    icon = Icons.Default.PlayArrow,
                    onClick = onWatchNow,
                    modifier = watchNowFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
                CinemaButton(
                    text = stringResource(R.string.btn_details),
                    variant = CinemaButtonVariant.SecondaryDark,
                    onClick = onDetails,
                )
            }
        }
        if (carouselDotCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(carouselDotCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == selectedCarouselDot) 10.dp else 8.dp)
                            .clip(CinemaShapes.Large)
                            .background(
                                if (index == selectedCarouselDot) CinemaColors.Gold else CinemaColors.TextMuted.copy(alpha = 0.5f),
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
fun FeaturedStrip(
    title: String,
    metadata: List<String>,
    onWatchNow: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    backdropUrl: String? = null,
) {
    HeroBanner(
        title = title,
        metadata = metadata,
        description = "",
        onWatchNow = onWatchNow,
        onDetails = onDetails,
        modifier = modifier,
        height = CinemaSpacing.HeroFeaturedMinHeight,
        carouselDotCount = 0,
        backdropUrl = backdropUrl,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterGrid(
    items: List<PosterCardData>,
    onItemClick: (PosterCardData) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 5,
) {
    Column(
        modifier = modifier.padding(bottom = CinemaSpacing.SectionGap),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap)) {
                rowItems.forEach { item ->
                    PosterCard(
                        data = item,
                        onClick = { onItemClick(item) },
                    )
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
        modifier = modifier.size(width = 160.dp, height = 200.dp),
        onClick = onClick,
        shape = CinemaShapes.Large,
    ) { focused ->
        FocusableCardSurface(
            backgroundColor = CinemaColors.SurfaceSoft,
            shape = CinemaShapes.Large,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CinemaShapes.Large)
                        .background(
                            if (isSelected || focused) CinemaColors.GoldDeep else CinemaColors.Surface,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CinemaColors.TextPrimary,
                        ),
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) CinemaColors.Gold else CinemaColors.TextPrimary,
                    ),
                )
            }
        }
    }
}
