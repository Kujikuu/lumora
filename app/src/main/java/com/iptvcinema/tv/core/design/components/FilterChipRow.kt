package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

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
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        itemsIndexed(items) { index, label ->
            CategoryChip(
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onSelected(index) },
                modifier = if (index == focusedChipIndex && chipFocusRequester != null) {
                    Modifier.focusRequester(chipFocusRequester)
                } else {
                    Modifier
                },
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
        shape = CinemaShapes.Large,
    ) { _ ->
        Box(
            modifier = Modifier
                .background(
                    color = if (isSelected) CinemaColors.Gold else CinemaColors.SurfaceSoft,
                    shape = CinemaShapes.Large,
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) CinemaColors.Background else CinemaColors.TextSecondary,
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
