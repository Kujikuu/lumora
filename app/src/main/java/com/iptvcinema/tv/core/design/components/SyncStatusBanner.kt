package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SyncStatusBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = CinemaColors.GoldSoft,
            fontWeight = FontWeight.Medium,
        ),
    )
}
