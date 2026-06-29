package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
) {
    val scrollState = rememberScrollState()
    val sectionBringIntoViewRequester = remember(title) { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val railHeight = variant.railHeight()

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(railHeight)
                .horizontalScroll(scrollState)
                .padding(
                    start = CinemaSpacing.NavRailWidth + 16.dp,
                    end = CinemaSpacing.ScreenPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val itemBringIntoViewRequester = remember(item.contentId) { BringIntoViewRequester() }
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
                            scope.launch {
                                sectionBringIntoViewRequester.bringIntoView()
                                itemBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    modifier = Modifier
                        .bringIntoViewRequester(itemBringIntoViewRequester)
                        .then(
                            if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}
