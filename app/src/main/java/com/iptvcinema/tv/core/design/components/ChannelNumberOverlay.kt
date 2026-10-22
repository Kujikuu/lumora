package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

/** The channel number being typed on the remote, large in the top corner like a TV tuner. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelNumberOverlay(
    digits: String,
    modifier: Modifier = Modifier,
) {
    if (digits.isEmpty()) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(CinemaSpacing.ScreenPadding),
        contentAlignment = Alignment.TopEnd,
    ) {
        Text(
            text = digits,
            modifier = Modifier
                .background(PlayerSidebarFill, CinemaShapes.Pill)
                .padding(horizontal = 28.dp, vertical = 12.dp),
            style = MaterialTheme.typography.displaySmall.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}
