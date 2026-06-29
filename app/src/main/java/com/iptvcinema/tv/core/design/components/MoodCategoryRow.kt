package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.home.MoodCategory

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MoodCategoryRow(
    categories: List<MoodCategory>,
    onCategoryClick: (MoodCategory) -> Unit,
    modifier: Modifier = Modifier,
    title: String,
) {
    var focusedTileIndex by remember(title) { mutableIntStateOf(-1) }
    val sectionBringIntoViewRequester = remember(title) { BringIntoViewRequester() }

    LaunchedEffect(focusedTileIndex) {
        if (focusedTileIndex >= 0) {
            sectionBringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = modifier
            .bringIntoViewRequester(sectionBringIntoViewRequester)
            .padding(bottom = CinemaSpacing.CardGap),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.TextPrimary,
            ),
            modifier = Modifier.padding(
                start = CinemaSpacing.NavRailWidth + 16.dp,
                end = CinemaSpacing.ScreenPadding,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    start = CinemaSpacing.NavRailWidth + 16.dp,
                    end = CinemaSpacing.ScreenPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
        ) {
            categories.forEachIndexed { index, category ->
                FocusableCinemaCard(
                    modifier = Modifier
                        .size(CinemaSpacing.MoodTileSize)
                        .onFocusChanged {
                            focusedTileIndex = if (it.isFocused) index else if (focusedTileIndex == index) -1 else focusedTileIndex
                        },
                    onClick = { onCategoryClick(category) },
                    shape = CinemaShapes.Medium,
                    contentDescription = stringResource(category.labelRes),
                ) { _ ->
                    Box(
                        modifier = Modifier
                            .size(CinemaSpacing.MoodTileSize)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(category.gradientStartArgb), Color(category.gradientEndArgb)),
                                ),
                                CinemaShapes.Medium,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CinemaColors.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}
