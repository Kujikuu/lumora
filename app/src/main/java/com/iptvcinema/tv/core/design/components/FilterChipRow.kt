package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import kotlinx.coroutines.launch

private val FilterChipRowHeight = 56.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FilterChipRow(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chipFocusRequester: FocusRequester? = null,
    focusedChipIndex: Int = 0,
    onEnteringRow: suspend () -> Unit = {},
    parentHandlesVerticalScroll: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val sectionBringIntoViewRequester = remember(items) { BringIntoViewRequester() }
    var hadFocusInRow by remember(items) { mutableStateOf(false) }

    LazyRow(
        modifier = modifier
            .height(FilterChipRowHeight)
            .graphicsLayer { clip = true }
            .focusGroup()
            .then(
                if (!parentHandlesVerticalScroll) {
                    Modifier.bringIntoViewRequester(sectionBringIntoViewRequester)
                } else {
                    Modifier
                },
            ),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = CinemaSpacing.FocusScaleOverflow / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(items) { index, label ->
            CategoryChip(
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onSelected(index) },
                focusScale = 1f,
                modifier = Modifier
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                val enteringRow = !hadFocusInRow
                                hadFocusInRow = true
                                if (enteringRow) {
                                    onEnteringRow()
                                    if (!parentHandlesVerticalScroll) {
                                        sectionBringIntoViewRequester.bringIntoView()
                                    }
                                }
                                listState.animateToFocusedItem(index)
                            }
                        }
                    }
                    .then(
                        if (index == focusedChipIndex && chipFocusRequester != null) {
                            Modifier.focusRequester(chipFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusScale: Float = 1.02f,
) {
    FocusableCinemaCard(
        modifier = modifier,
        onClick = onClick,
        shape = CinemaShapes.Pill,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = focusScale,
    ) { focused ->
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 52.dp)
                .background(
                    color = when {
                        isSelected -> CinemaColors.Accent
                        focused -> CinemaColors.White
                        else -> CinemaColors.SurfaceSoft
                    },
                    shape = CinemaShapes.Pill,
                )
                .padding(horizontal = 26.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        focused && !isSelected -> CinemaColors.Background
                        else -> CinemaColors.White
                    },
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CategoryChip(label = label, isSelected = isSelected, onClick = onClick, modifier = modifier)
}
