package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FilterChipRow(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chipFocusRequester: FocusRequester? = null,
    focusedChipIndex: Int = 0,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var hadFocusInRow by remember(items) { mutableStateOf(false) }

    LazyRow(
        modifier = modifier.onFocusChanged { if (!it.hasFocus) hadFocusInRow = false },
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        itemsIndexed(items) { index, label ->
            CategoryChip(
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onSelected(index) },
                modifier = Modifier
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                hadFocusInRow = true
                                listState.animateScrollToItem(index)
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
) {
    FocusableCinemaCard(
        modifier = modifier,
        onClick = onClick,
        shape = CinemaShapes.Small,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Box(
            modifier = Modifier
                .background(
                    color = when {
                        isSelected -> CinemaColors.Accent
                        focused -> CinemaColors.Surface
                        else -> CinemaColors.SurfaceSoft
                    },
                    shape = CinemaShapes.Small,
                )
                .then(
                    if (!isSelected && focused) {
                        Modifier.border(1.dp, CinemaColors.FocusBorder, CinemaShapes.Small)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) CinemaColors.White else CinemaColors.TextPrimary,
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
