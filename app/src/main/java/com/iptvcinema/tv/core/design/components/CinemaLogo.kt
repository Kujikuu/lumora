package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    navBar: Boolean = false,
) {
    val iconSize = when {
        navBar -> 28.dp
        compact -> 24.dp
        else -> 48.dp
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CinemaBrandMark(size = iconSize)
        if (!compact && !navBar) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.Accent,
                ),
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
fun CinemaBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(CinemaColors.Accent, CinemaColors.Secondary),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            ),
            radius = w / 2f,
            center = center,
        )

        val playPath = Path().apply {
            moveTo(w * 0.36f, h * 0.22f)
            lineTo(w * 0.76f, h * 0.50f)
            lineTo(w * 0.36f, h * 0.78f)
            close()
        }
        drawPath(path = playPath, color = CinemaColors.White)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingIndicator(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(com.iptvcinema.tv.core.design.theme.CinemaShapes.Medium)
            .background(CinemaColors.SurfaceSoft)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
        )
    }
}
