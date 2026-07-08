package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun <T> ContentRail(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    countLabel: String? = null,
    itemKey: ((T) -> Any)? = null,
    enableFocusScroll: Boolean = true,
    itemContent: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val contentStart = shellContentStart()

    Column(
        modifier = modifier.padding(bottom = CinemaSpacing.CardGap),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = contentStart,
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
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                )
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = contentStart,
                end = CinemaSpacing.ScreenPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { index, item -> itemKey?.invoke(item) ?: index }) { index, item ->
                Box(
                    modifier = if (enableFocusScroll) {
                        Modifier.onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    listState.animateToFocusedItem(index)
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ) {
                    itemContent(item)
                }
            }
        }
    }
}
