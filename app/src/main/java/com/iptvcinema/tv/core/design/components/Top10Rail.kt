package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.HomeContentCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Top10Rail(
    title: String,
    items: List<HomeContentCard>,
    onCardClick: (HomeContentCard) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
) {
    val listState = rememberLazyListState()
    val sectionBringIntoViewRequester = remember(title) { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val shellImmersion = LocalShellImmersion.current
    val contentStart = shellContentStart()
    var focusedItemIndex by remember(title) { mutableIntStateOf(0) }

    LaunchedEffect(listState, shellImmersion) {
        if (shellImmersion == null) return@LaunchedEffect
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                focusedItemIndex,
            )
        }.collect { (_, _, index) ->
            shellImmersion.updateRailScroll(listState, index)
        }
    }

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .bringIntoViewRequester(sectionBringIntoViewRequester)
            .padding(bottom = CinemaSpacing.CardGap),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.AccentSoft,
            ),
            modifier = Modifier.padding(
                start = contentStart,
                end = CinemaSpacing.ScreenPadding,
            ),
        )

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp),
            contentPadding = PaddingValues(
                start = contentStart,
                end = CinemaSpacing.ScreenPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items.take(10), key = { _, item -> item.contentId }) { index, item ->
                Top10Card(
                    rank = index + 1,
                    data = item,
                    onClick = { onCardClick(item) },
                    modifier = Modifier
                        .then(
                            if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged { focus ->
                            if (focus.isFocused) {
                                focusedItemIndex = index
                                shellImmersion?.updateRailImmersion(
                                    hasRailFocus = true,
                                    focusedItemIndex = index,
                                    listState = listState,
                                )
                                scope.launch {
                                sectionBringIntoViewRequester.bringIntoView()
                                listState.scrollToItem(index)
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Top10Card(
    rank: Int,
    data: HomeContentCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(250.dp)
            .height(204.dp),
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 150.sp,
                lineHeight = 150.sp,
                color = CinemaColors.White.copy(alpha = 0.72f),
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 6.dp),
        )

        FocusableCinemaCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 112.dp, height = 168.dp),
            onClick = onClick,
            shape = CinemaShapes.Card,
            defaultBorderWidth = 0.dp,
            contentDescription = data.title,
            focusScale = 1.02f,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CinemaShapes.Card)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = data.imageUrl,
                    contentDescription = data.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = data.title,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    CinemaColors.Background.copy(alpha = 0.18f),
                                    CinemaColors.Background.copy(alpha = 0.72f),
                                ),
                            ),
                        ),
                )
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CinemaColors.White,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(6.dp),
                )
            }
        }
    }
}
