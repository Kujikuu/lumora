package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.MovieItem
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
    onAddToList: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    carouselThumbs: List<Pair<String?, String>> = emptyList(),
    selectedThumbIndex: Int = 0,
    onThumbSelect: ((Int) -> Unit)? = null,
) {
    val sectionBringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val sideGradientColors = if (isRtl) {
        listOf(
            Color.Transparent,
            Color.Transparent,
            CinemaColors.Background.copy(alpha = 0.42f),
            CinemaColors.Background.copy(alpha = 0.9f),
        )
    } else {
        listOf(
            CinemaColors.Background.copy(alpha = 0.9f),
            CinemaColors.Background.copy(alpha = 0.42f),
            Color.Transparent,
            Color.Transparent,
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height, max = CinemaSpacing.HeroMaxHeight)
            .bringIntoViewRequester(sectionBringIntoViewRequester),
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
                .height(320.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CinemaColors.Background.copy(alpha = 0.5f),
                            CinemaColors.Background.copy(alpha = 0.85f),
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
                        colors = sideGradientColors,
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
            BadgeChip(
                text = stringResource(R.string.home_spotlight),
                backgroundColor = CinemaColors.AccentDeep,
            )
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
                    modifier = Modifier
                        .then(watchNowFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    sectionBringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                )
                if (onAddToList != null) {
                    CinemaButton(
                        text = "",
                        variant = CinemaButtonVariant.Icon,
                        icon = Icons.Default.Add,
                        onClick = onAddToList,
                    )
                }
                if (onFavorite != null) {
                    CinemaButton(
                        text = "",
                        variant = CinemaButtonVariant.Icon,
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        onClick = onFavorite,
                    )
                }
                CinemaButton(
                    text = stringResource(R.string.btn_details),
                    variant = CinemaButtonVariant.SecondaryDark,
                    icon = Icons.Default.Info,
                    onClick = onDetails,
                )
            }
        }

        if (carouselThumbs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = CinemaSpacing.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                carouselThumbs.forEachIndexed { index, (imageUrl, label) ->
                    FocusableCinemaCard(
                        modifier = Modifier.size(CinemaSpacing.HeroThumbSize),
                        onClick = { onThumbSelect?.invoke(index) },
                        shape = CinemaShapes.Medium,
                        contentDescription = label,
                        focusScale = if (index == selectedThumbIndex) 1.08f else 1f,
                    ) { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CinemaShapes.Medium)
                                .background(CinemaColors.Surface),
                        ) {
                            CinemaAsyncImage(
                                imageUrl = imageUrl,
                                contentDescription = label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                fallbackLabel = label,
                            )
                        }
                    }
                }
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroCarousel(
    movies: List<MovieItem>,
    onWatchNow: (MovieItem) -> Unit,
    onDetails: (MovieItem) -> Unit,
    modifier: Modifier = Modifier,
    watchNowFocusRequester: FocusRequester? = null,
    onAddToList: ((MovieItem) -> Unit)? = null,
    onFavorite: ((MovieItem) -> Unit)? = null,
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
            onAddToList = onAddToList?.let { callback -> { callback(movie) } },
            onFavorite = onFavorite?.let { callback -> { callback(movie) } },
            isFavorite = movie.isFavorite,
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { movies.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
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
            val thumbs = movies.map { (it.backdropUrl ?: it.imageUrl) to it.title }
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
                onAddToList = onAddToList?.let { callback -> { callback(movie) } },
                onFavorite = onFavorite?.let { callback -> { callback(movie) } },
                isFavorite = movie.isFavorite,
                carouselThumbs = thumbs,
                selectedThumbIndex = pagerState.currentPage,
                onThumbSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
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
    enableVerticalScroll: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        start = CinemaSpacing.NavRailWidth + 16.dp,
        end = 24.dp,
        bottom = CinemaSpacing.SectionGap,
    ),
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enableVerticalScroll) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            ) {
                rowItems.forEach { item ->
                    val itemBringIntoViewRequester = remember(item.contentId ?: item.title) {
                        BringIntoViewRequester()
                    }
                    PosterCard(
                        data = item,
                        onClick = { onItemClick(item) },
                        fixedWidth = null,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (!enableVerticalScroll) {
                                    Modifier
                                        .bringIntoViewRequester(itemBringIntoViewRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                scope.launch {
                                                    itemBringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        }
                                } else {
                                    Modifier
                                },
                            )
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
