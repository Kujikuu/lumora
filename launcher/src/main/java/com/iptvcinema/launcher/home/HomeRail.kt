package com.iptvcinema.launcher.home

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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.launcher.home.HomeDimens.railHeight
import com.iptvcinema.tv.core.design.components.glideItemToStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import kotlinx.coroutines.launch

/**
 * A titled horizontal rail. The row glides so the focused card sits at the start, and
 * [entryRequester] always sits on the card focused last (or the first on screen), so Up/Down from
 * another row lands where the viewer left. Left/Right at the ends are swallowed so focus never
 * jumps to another row.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeRail(
    title: String,
    row: HomeRow,
    entryRequester: FocusRequester,
    onRowFocused: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onCardClick: (HomeCard) -> Unit,
    modifier: Modifier = Modifier,
    onCardLongClick: ((HomeCard) -> Unit)? = null,
) {
    val lastIndex = (row.cards.size - 1).coerceAtLeast(0)
    var entryIndex by remember(row.key) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val resolvedEntry by remember(lastIndex) {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            resolveRailEntry(
                entryIndex = entryIndex,
                lastIndex = lastIndex,
                onScreen = visible.filter { it.offset >= 0 }.map { it.index } + visible.filter { it.offset < 0 }.map { it.index },
            )
        }
    }
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { onUp(); true }
                    Key.DirectionDown -> { onDown(); true }
                    // At either end, the key that would leave the rail is swallowed.
                    Key.DirectionLeft -> if (isRtl) entryIndex == lastIndex else entryIndex == 0
                    Key.DirectionRight -> if (isRtl) entryIndex == 0 else entryIndex == lastIndex
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(HomeDimens.RailTitleGap),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CinemaColors.TextPrimary),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = HomeDimens.ScreenPadding),
        )
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(row.style.railHeight()),
            userScrollEnabled = false,
            contentPadding = PaddingValues(
                start = HomeDimens.ScreenPadding,
                end = HomeDimens.ScreenPadding,
                top = HomeDimens.FocusOverflow / 2,
                bottom = HomeDimens.FocusOverflow / 2,
            ),
            horizontalArrangement = Arrangement.spacedBy(HomeDimens.CardGap),
        ) {
            itemsIndexed(row.cards, key = { _, card -> card.key }) { index, card ->
                // Each card reads only whether it is the entry card, so a focus move recomposes
                // the two cards whose answer changed, not the whole rail.
                val isEntry by remember(index) { derivedStateOf { index == resolvedEntry } }
                HomeCardView(
                    card = card,
                    style = row.style,
                    onClick = { onCardClick(card) },
                    onLongClick = onCardLongClick?.let { callback -> { callback(card) } },
                    modifier = Modifier
                        .then(if (isEntry) Modifier.focusRequester(entryRequester) else Modifier)
                        .onFocusChanged { focus ->
                            if (focus.isFocused) {
                                entryIndex = index
                                onRowFocused()
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
 * the rail shrank. When that card is not on screen, the first card on screen takes its place.
 */
internal fun resolveRailEntry(entryIndex: Int, lastIndex: Int, onScreen: List<Int>): Int {
    val clamped = entryIndex.coerceIn(0, lastIndex)
    return if (onScreen.isEmpty() || clamped in onScreen) clamped else onScreen.first()
}
