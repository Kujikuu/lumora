package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryListPanel(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listFocusRequester: FocusRequester? = null,
    initialFocusedIndex: Int = 0,
    onItemFocused: ((Int) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .clip(CinemaShapes.Medium)
            .background(CinemaColors.SurfaceSoft)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(title = stringResource(R.string.hint_filter_desc))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(items, key = { index, label -> "$index-$label" }) { index, label ->
                CategoryListRow(
                    label = label,
                    isSelected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    onFocused = {
                        onItemFocused?.invoke(index)
                        onSelected(index)
                    },
                    modifier = if (index == initialFocusedIndex && listFocusRequester != null) {
                        Modifier.focusRequester(listFocusRequester)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryListRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var wasFocused by remember(label) { mutableStateOf(false) }
    FocusableCinemaCard(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !wasFocused) {
                    wasFocused = true
                    onFocused()
                } else if (!focusState.isFocused) {
                    wasFocused = false
                }
            },
        onClick = onClick,
        shape = CinemaShapes.Small,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        focused -> CinemaColors.Surface
                        isSelected -> CinemaColors.SurfaceSoft
                        else -> CinemaColors.Background
                    },
                    CinemaShapes.Small,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) CinemaColors.White else CinemaColors.TextPrimary,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
