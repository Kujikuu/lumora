package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.design.theme.rememberAppFontFamily

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaSerifTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val titleFont = rememberAppFontFamily()
    Text(
        text = text,
        modifier = Modifier
            .padding(start = CinemaSpacing.ContentStart, end = 24.dp, top = 20.dp)
            .then(modifier),
        style = MaterialTheme.typography.displaySmall.copy(
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            color = CinemaColors.White,
        ),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = CinemaColors.TextPrimary,
        ),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MetadataRow(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Text(
        text = items.joinToString(separator = " · "),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextSecondary),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CinemaShapes.Medium,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(CinemaColors.SurfaceGlass, shape)
            .padding(CinemaSpacing.ButtonGap),
    ) {
        content()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GoldBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CinemaShapes.Small)
            .background(CinemaColors.Accent)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
            ),
        )
    }
}
