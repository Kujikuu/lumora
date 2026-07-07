package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FocusAwareContentRail(
    title: String,
    items: List<HomeContentCard>,
    onWatchNow: (HomeContentCard) -> Unit,
    onAddToList: (HomeContentCard) -> Unit,
    onFavorite: (HomeContentCard) -> Unit,
    onCardClick: (HomeContentCard) -> Unit,
    modifier: Modifier = Modifier,
    variant: ExpandedPosterCardVariant = ExpandedPosterCardVariant.Portrait,
    countLabel: String? = null,
    firstItemFocusRequester: FocusRequester? = null,
    onItemFocused: (HomeContentCard?) -> Unit = {},
    sectionId: String? = null,
    focusedItemIndex: Int = -1,
    onFocusedItemIndexChange: (Int) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val sectionBringIntoViewRequester = remember(title) { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val railHeight = variant.railHeight()
    val focusOverflow = CinemaSpacing.FocusScaleOverflow
    var hadFocusInRail by remember(title) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .bringIntoViewRequester(sectionBringIntoViewRequester)
            .padding(bottom = CinemaSpacing.CardGap),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = CinemaSpacing.NavRailWidth + 16.dp,
                    end = CinemaSpacing.ScreenPadding,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CinemaColors.TextPrimary,
                ),
            )
            if (!countLabel.isNullOrBlank()) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                )
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(railHeight + focusOverflow)
                .padding(vertical = focusOverflow / 2)
                .onFocusChanged { if (!it.hasFocus) hadFocusInRail = false },
            contentPadding = PaddingValues(
                start = CinemaSpacing.NavRailWidth + 16.dp,
                end = CinemaSpacing.ScreenPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { _, item -> item.contentId }) { index, item ->
                val itemFocusRequester = remember(item.contentId, sectionId) {
                    if (sectionId != null && index == focusedItemIndex) FocusRequester() else null
                }
                ExpandedPosterCard(
                    data = item,
                    variant = variant,
                    onWatchNow = { onWatchNow(item) },
                    onAddToList = { onAddToList(item) },
                    onFavorite = { onFavorite(item) },
                    onCardClick = { onCardClick(item) },
                    onFocusChanged = { focused ->
                        if (focused) {
                            onItemFocused(item)
                            onFocusedItemIndexChange(index)
                            scope.launch {
                                val enteringRail = !hadFocusInRail
                                hadFocusInRail = true
                                if (enteringRail) {
                                    sectionBringIntoViewRequester.bringIntoView()
                                }
                                listState.scrollToItem(index)
                            }
                        }
                    },
                    modifier = Modifier
                        .then(
                            if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else if (itemFocusRequester != null) {
                                Modifier.focusRequester(itemFocusRequester)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}
