package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
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

private val FilterChipRowHeight = 52.dp
private val ChipMinHeight = 40.dp

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    // Open scrolled to the selected chip: a chip off screen is not composed, so focus sent to it
    // would land nowhere and the next D-pad press would jump unpredictably.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    val sectionBringIntoViewRequester = remember(items) { BringIntoViewRequester() }
    var hadFocusInRow by remember(items) { mutableStateOf(false) }

    LazyRow(
        modifier = modifier
            .height(FilterChipRowHeight)
            .graphicsLayer { clip = true }
            // Coming back to the row (Up from a grid, say) lands on the chip the viewer last used,
            // which is the selected one, not whichever chip happens to sit above the focus.
            .focusRestorer()
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
        // Default: quiet surface. Selected: raised surface, bold white, accent dot.
        // Focused: white pill with dark text, like every focused button.
        val background = when {
            focused -> CinemaColors.White
            isSelected -> CinemaColors.SurfaceRaised
            else -> CinemaColors.Surface
        }
        val textColor = when {
            focused -> CinemaColors.Background
            isSelected -> CinemaColors.White
            else -> CinemaColors.TextSecondary
        }
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = ChipMinHeight)
                .background(color = background, shape = CinemaShapes.Pill)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            if (focused) CinemaColors.Background else CinemaColors.Accent,
                            CircleShape,
                        ),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected || focused) FontWeight.ExtraBold else FontWeight.Bold,
                    color = textColor,
                ),
                maxLines = 1,
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
