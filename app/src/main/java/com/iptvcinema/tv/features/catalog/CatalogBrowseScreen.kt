package com.iptvcinema.tv.features.catalog

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogSortOption
import com.iptvcinema.tv.core.design.components.ExpandedPosterCardVariant
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.components.FocusAwareContentRail
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.design.components.PosterGrid
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.ScreenFocusState

object CatalogBrowseSections {
    const val FEATURED = "featured"
    const val CONTINUE = "continue"
    const val GRID = "grid"
}

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
    scrollState: ScrollState,
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
    watchNowFocus: FocusRequester,
    continueWatchingFocus: FocusRequester,
    categoryFocus: FocusRequester,
    gridFocus: FocusRequester,
    featuredContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(scrollState.value) {
        focusState.saveBrowseFocus(
            sectionId = focusState.sectionId,
            itemIndex = focusState.itemIndex,
            scrollOffset = scrollState.value,
            focusedContentId = focusState.focusedContentId,
            categoryIndex = focusState.focusIndex,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
    ) {
        featuredContent()

        if (continueWatchingItems.isNotEmpty()) {
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
                        scrollOffset = scrollState.value,
                        categoryIndex = selectedFilter,
                    )
                },
                firstItemFocusRequester = if (!hasFeatured) continueWatchingFocus else null,
                onWatchNow = onContinueWatchNow,
                onAddToList = onContinueAddToList,
                onFavorite = onContinueFavorite,
                onCardClick = onContinueCardClick,
            )
        }

        if (categories.isNotEmpty()) {
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
                modifier = Modifier.padding(start = CinemaSpacing.ContentStart),
            )
        }

        FilterChipRow(
            items = sortOptions,
            selectedIndex = selectedSortIndex,
            onSelected = onSortSelected,
            focusedChipIndex = selectedSortIndex,
            modifier = Modifier.padding(start = CinemaSpacing.ContentStart),
        )

        PosterGrid(
            items = posters,
            enableVerticalScroll = false,
            focusedContentId = focusState.focusedContentId.takeIf {
                focusState.sectionId == CatalogBrowseSections.GRID
            },
            firstItemFocusRequester = if (
                !hasFeatured &&
                continueWatchingItems.isEmpty() &&
                categories.isEmpty()
            ) {
                gridFocus
            } else {
                null
            },
            contentPadding = PaddingValues(bottom = CinemaSpacing.SectionGap),
            onItemClick = onPosterClick,
            onItemLongClick = onPosterLongClick,
            onItemFocused = { poster ->
                poster.contentId?.let { contentId ->
                    focusState.saveBrowseFocus(
                        sectionId = CatalogBrowseSections.GRID,
                        scrollOffset = scrollState.value,
                        focusedContentId = contentId,
                        categoryIndex = selectedFilter,
                    )
                }
            },
        )
    }
}
