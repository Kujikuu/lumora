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
        navBar -> 36.dp
        compact -> 32.dp
        else -> 56.dp
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CinemaBrandMark(size = iconSize)
        if (navBar || !compact) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CinemaColors.Gold,
                ),
                modifier = Modifier.padding(start = if (navBar) 10.dp else 12.dp),
            )
        }
    }
}

@Composable
fun CinemaBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        drawCircle(color = CinemaColors.Surface, radius = radius, center = center)
        drawCircle(color = CinemaColors.Background, radius = radius * 0.84f, center = center)
        drawCircle(color = CinemaColors.GoldDeep, radius = radius * 0.69f, center = center)
        drawCircle(color = CinemaColors.Background, radius = radius * 0.56f, center = center)
        drawCircle(
            color = CinemaColors.GoldSoft.copy(alpha = 0.22f),
            radius = radius * 0.79f,
            center = center.copy(y = center.y - radius * 0.08f),
        )

        val playPath = Path().apply {
            moveTo(center.x - radius * 0.25f, center.y - radius * 0.43f)
            lineTo(center.x + radius * 0.48f, center.y)
            lineTo(center.x - radius * 0.25f, center.y + radius * 0.43f)
            close()
        }
        drawPath(path = playPath, color = CinemaColors.Gold)
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
