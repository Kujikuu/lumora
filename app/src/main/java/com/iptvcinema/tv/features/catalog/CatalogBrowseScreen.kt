package com.iptvcinema.tv.features.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogSortOption
import com.iptvcinema.tv.core.design.components.ExpandedPosterCardVariant
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.FocusAwareContentRail
import com.iptvcinema.tv.core.design.components.PosterCard
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.design.components.catalogContentStart
import com.iptvcinema.tv.core.design.components.isSectionVisible
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.ScreenFocusState
import kotlinx.coroutines.launch

object CatalogBrowseSections {
    const val FEATURED = "featured"
    const val CONTINUE = "continue"
    const val GRID = "grid"
}

data class CatalogSectionIndices(
    val continueWatching: Int,
    val categories: Int,
    val sort: Int,
    val gridStart: Int,
    val maxIndex: Int,
)

@Composable
fun rememberCatalogSortOptions(): List<String> = listOf(
    stringResource(R.string.sort_title_az),
    stringResource(R.string.sort_year),
    stringResource(R.string.sort_newest),
)

fun catalogSortIndex(option: CatalogSortOption): Int = when (option) {
    CatalogSortOption.TITLE_AZ -> 0
    CatalogSortOption.YEAR -> 1
    CatalogSortOption.NEWEST -> 2
}

fun catalogSortFromIndex(index: Int): CatalogSortOption = when (index) {
    1 -> CatalogSortOption.YEAR
    2 -> CatalogSortOption.NEWEST
    else -> CatalogSortOption.TITLE_AZ
}

@Composable
fun CatalogBrowseContent(
    focusState: ScreenFocusState,
    hasFeatured: Boolean,
    continueWatchingItems: List<HomeContentCard>,
    categories: List<String>,
    selectedFilter: Int,
    onCategorySelected: (Int) -> Unit,
    sortOptions: List<String>,
    selectedSortIndex: Int,
    onSortSelected: (Int) -> Unit,
    posters: List<PosterCardData>,
    onPosterClick: (PosterCardData) -> Unit,
    onPosterLongClick: ((PosterCardData) -> Unit)? = null,
    onContinueWatchNow: (HomeContentCard) -> Unit,
    onContinueCardClick: (HomeContentCard) -> Unit,
    onContinueAddToList: (HomeContentCard) -> Unit,
    onContinueFavorite: (HomeContentCard) -> Unit,
    onContinueCardLongClick: ((HomeContentCard) -> Unit)? = null,
    watchNowFocus: FocusRequester,
    continueWatchingFocus: FocusRequester,
    categoryFocus: FocusRequester,
    gridFocus: FocusRequester,
    featuredContent: @Composable () -> Unit = {},
    onRestoreScrollReady: (suspend () -> Unit) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val posterRows = remember(posters) { posters.chunked(CATALOG_GRID_COLUMNS) }
    val sectionIndices = remember(hasFeatured, continueWatchingItems.isNotEmpty(), categories.isNotEmpty()) {
        var index = 1 // featured slot always occupies index 0
        val continueWatching = if (continueWatchingItems.isNotEmpty()) index++ else -1
        val categoriesIndex = if (categories.isNotEmpty()) index++ else -1
        val sort = index++
        val gridStart = index
        val maxIndex = (gridStart + posterRows.lastIndex).coerceAtLeast(sort)
        CatalogSectionIndices(
            continueWatching = continueWatching,
            categories = categoriesIndex,
            sort = sort,
            gridStart = gridStart,
            maxIndex = maxIndex,
        )
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = focusState.scrollOffset.coerceIn(0, sectionIndices.maxIndex),
    )
    val scope = rememberCoroutineScope()
    val contentStart = catalogContentStart()
    var lastFocusedGridRow by remember { mutableIntStateOf(-1) }

    val scrollToSection: suspend (Int) -> Unit = { sectionIndex ->
        if (sectionIndex >= 0 && !listState.isSectionVisible(sectionIndex)) {
            listState.animateScrollToItem(sectionIndex)
        }
    }

    LaunchedEffect(listState, sectionIndices.maxIndex, focusState.scrollOffset) {
        onRestoreScrollReady {
            listState.scrollToItem(focusState.scrollOffset.coerceIn(0, sectionIndices.maxIndex))
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        focusState.saveBrowseFocus(
            sectionId = focusState.sectionId,
            itemIndex = focusState.itemIndex,
            scrollOffset = listState.firstVisibleItemIndex,
            focusedContentId = focusState.focusedContentId,
            categoryIndex = focusState.focusIndex,
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
    ) {
        item(key = CatalogBrowseSections.FEATURED) {
            if (hasFeatured) {
                featuredContent()
            }
        }

        if (continueWatchingItems.isNotEmpty()) {
            item(key = CatalogBrowseSections.CONTINUE) {
                FocusAwareContentRail(
                    title = stringResource(R.string.home_continue_watching),
                    items = continueWatchingItems,
                    variant = ExpandedPosterCardVariant.Landscape,
                    countLabel = continueWatchingItems.size.toString(),
                    sectionId = CatalogBrowseSections.CONTINUE,
                    focusedItemIndex = if (focusState.sectionId == CatalogBrowseSections.CONTINUE) {
                        focusState.itemIndex
                    } else {
                        -1
                    },
                    onFocusedItemIndexChange = { index ->
                        focusState.saveBrowseFocus(
                            sectionId = CatalogBrowseSections.CONTINUE,
                            itemIndex = index,
                            scrollOffset = listState.firstVisibleItemIndex,
                            categoryIndex = selectedFilter,
                        )
                    },
                    firstItemFocusRequester = if (!hasFeatured) continueWatchingFocus else null,
                    onEnteringRail = { scrollToSection(sectionIndices.continueWatching) },
                    parentHandlesVerticalScroll = sectionIndices.continueWatching >= 0,
                    stableContentStart = true,
                    onWatchNow = onContinueWatchNow,
                    onAddToList = onContinueAddToList,
                    onFavorite = onContinueFavorite,
                    onCardClick = onContinueCardClick,
                    onCardLongClick = onContinueCardLongClick,
                )
            }
        }

        if (categories.isNotEmpty()) {
            item(key = "categories") {
                FilterChipRow(
                    items = categories,
                    selectedIndex = selectedFilter.coerceIn(0, categories.lastIndex),
                    onSelected = onCategorySelected,
                    chipFocusRequester = if (!hasFeatured && continueWatchingItems.isEmpty()) {
                        categoryFocus
                    } else {
                        null
                    },
                    focusedChipIndex = selectedFilter,
                    modifier = Modifier.padding(start = contentStart),
                    onEnteringRow = { scrollToSection(sectionIndices.categories) },
                    parentHandlesVerticalScroll = sectionIndices.categories >= 0,
                )
            }
        }

        item(key = "sort") {
            FilterChipRow(
                items = sortOptions,
                selectedIndex = selectedSortIndex,
                onSelected = onSortSelected,
                focusedChipIndex = selectedSortIndex,
                modifier = Modifier.padding(start = contentStart),
                onEnteringRow = { scrollToSection(sectionIndices.sort) },
                parentHandlesVerticalScroll = true,
            )
        }

        itemsIndexed(
            posterRows,
            key = { rowIndex, row -> "poster-row-${row.firstOrNull()?.contentId ?: rowIndex}" },
        ) { rowIndex, rowItems ->
            val gridListIndex = sectionIndices.gridStart + rowIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { clip = true }
                    .padding(
                        start = contentStart,
                        end = 24.dp,
                        top = CinemaSpacing.FocusScaleOverflow / 2,
                        bottom = CinemaSpacing.FocusScaleOverflow / 2,
                    ),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            ) {
                rowItems.forEachIndexed { columnIndex, poster ->
                    val itemIndex = rowIndex * CATALOG_GRID_COLUMNS + columnIndex
                    val isRestoreTarget = focusState.sectionId == CatalogBrowseSections.GRID &&
                        poster.contentId == focusState.focusedContentId
                    val isInitialGridTarget = itemIndex == 0 &&
                        !hasFeatured &&
                        continueWatchingItems.isEmpty() &&
                        categories.isEmpty()
                    PosterCard(
                        data = poster,
                        onClick = { onPosterClick(poster) },
                        onLongClick = onPosterLongClick?.let { callback -> { callback(poster) } },
                        fixedWidth = null,
                        focusScale = 1f,
                        focusedBorderWidth = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focus ->
                                if (focus.isFocused) {
                                    poster.contentId?.let { contentId ->
                                        focusState.saveBrowseFocus(
                                            sectionId = CatalogBrowseSections.GRID,
                                            itemIndex = itemIndex,
                                            scrollOffset = listState.firstVisibleItemIndex,
                                            focusedContentId = contentId,
                                            categoryIndex = selectedFilter,
                                        )
                                    }
                                    scope.launch {
                                        if (rowIndex != lastFocusedGridRow) {
                                            lastFocusedGridRow = rowIndex
                                            if (!listState.isSectionVisible(gridListIndex)) {
                                                listState.animateScrollToItem(gridListIndex)
                                            }
                                        }
                                    }
                                }
                            }
                            .then(
                                if (isRestoreTarget || isInitialGridTarget) {
                                    Modifier.focusRequester(gridFocus)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                repeat(CATALOG_GRID_COLUMNS - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private const val CATALOG_GRID_COLUMNS = 5
