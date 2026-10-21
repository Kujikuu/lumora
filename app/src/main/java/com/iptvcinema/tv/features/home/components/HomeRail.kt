package com.iptvcinema.tv.features.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.components.clipUnderNavRail
import com.iptvcinema.tv.core.design.components.shellContentStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection
import kotlinx.coroutines.launch

internal fun HomeSection.cardStyle(): HomeCardStyle = when (this) {
    is HomeSection.ContinueWatching, is HomeSection.NextEpisode, is HomeSection.Episodes -> HomeCardStyle.Landscape
    is HomeSection.RecentChannels, is HomeSection.Channels -> HomeCardStyle.Channel
    is HomeSection.Categories -> HomeCardStyle.Category
    is HomeSection.Rail, is HomeSection.TopRated -> HomeCardStyle.Poster
}

/** Fixed rail height, so vertical moves between rails never trigger a relayout. */
internal fun HomeCardStyle.railHeight(): Dp = height() + HomeDimens.FocusOverflow

/**
 * A titled horizontal rail. The row glides so the focused card sits at the start.
 *
 * [entryRequester] always sits on the card focused last in this rail, so the screen can move
 * focus into the rail from above or below and land where the viewer left it.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeRail(
    section: HomeSection,
    entryRequester: FocusRequester,
    onCardFocused: (card: HomeContentCard, index: Int) -> Unit,
    onCardClick: (HomeContentCard) -> Unit,
    modifier: Modifier = Modifier,
    onCardLongClick: ((HomeContentCard) -> Unit)? = null,
    initialFocusIndex: Int = 0,
) {
    val style = section.cardStyle()
    val lastIndex = (section.items.size - 1).coerceAtLeast(0)
    val initialIndex = initialFocusIndex.coerceIn(0, lastIndex)
    var entryIndex by remember(section.id) { mutableIntStateOf(initialIndex) }
    // Not read from entryIndex: reading it here would recompose the whole rail on every move.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    // The requester must sit on a card that is on screen: an off-screen card is not composed, so
    // a requester on it is detached and Up/Down into this rail would do nothing. That happens
    // when the list reorders under a remembered card (Continue Watching after watching a title).
    val resolvedEntry by remember(lastIndex) {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            resolveRailEntry(
                entryIndex = entryIndex,
                lastIndex = lastIndex,
                onScreen = visible.filter { it.offset >= 0 }.map { it.index } +
                    visible.filter { it.offset < 0 }.map { it.index },
            )
        }
    }
    val scope = rememberCoroutineScope()
    val contentStart = shellContentStart()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HomeDimens.RailTitleGap),
    ) {
        Text(
            text = section.title.resolve(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.TextPrimary,
            ),
            maxLines = 1,
            modifier = Modifier.padding(start = contentStart, end = CinemaSpacing.ScreenPadding),
        )
        LazyRow(
            state = listState,
            modifier = Modifier
                .clipUnderNavRail()
                .fillMaxWidth()
                .height(style.railHeight()),
            contentPadding = PaddingValues(
                start = contentStart,
                end = CinemaSpacing.ScreenPadding,
                top = HomeDimens.FocusOverflow / 2,
                bottom = HomeDimens.FocusOverflow / 2,
            ),
            horizontalArrangement = Arrangement.spacedBy(HomeDimens.CardGap),
        ) {
            itemsIndexed(section.items, key = { _, card -> "${card.contentType}:${card.contentId}" }) { index, card ->
                // Each card reads only whether it is the entry card, so a focus move recomposes
                // the two cards whose answer changed, not every composed card in the rail.
                val isEntry by remember(index) {
                    derivedStateOf { index == resolvedEntry }
                }
                HomeCard(
                    card = card,
                    style = style,
                    onClick = { onCardClick(card) },
                    onLongClick = onCardLongClick?.let { callback -> { callback(card) } },
                    modifier = Modifier
                        .then(
                            if (isEntry) {
                                Modifier.focusRequester(entryRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged { focus ->
                            if (focus.isFocused) {
                                entryIndex = index
                                onCardFocused(card, index)
                                scope.launch { listState.glideItemToStart(index) }
                            }
                        },
                )
            }
        }
    }
}

/**
 * The card that holds the rail's entry requester: the card focused last, or the last card when
 * the rail shrank. When that card is not on screen, the first card on screen takes its place;
 * [onScreen] lists the on-screen indexes, cards fully past the start edge first.
 */
internal fun resolveRailEntry(entryIndex: Int, lastIndex: Int, onScreen: List<Int>): Int {
    val clamped = entryIndex.coerceIn(0, lastIndex)
    return if (onScreen.isEmpty() || clamped in onScreen) clamped else onScreen.first()
}

@Composable
private fun HomeRailTitle.resolve(): String =
    text ?: argument?.let { stringResource(resId, it) } ?: stringResource(resId)
