package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val width = when {
        navBar -> 46.dp
        compact -> 52.dp
        else -> 105.dp
    }
    val textStyle = when {
        navBar -> MaterialTheme.typography.labelMedium
        compact -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.displaySmall
    }
    Text(
        text = stringResource(R.string.app_name).uppercase().replace(" ", "\n"),
        modifier = modifier.width(width),
        style = textStyle.copy(
            fontWeight = FontWeight.Black,
            color = CinemaColors.White,
            textAlign = TextAlign.Start,
        ),
        lineHeight = textStyle.fontSize,
        maxLines = 2,
        softWrap = false,
    )
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
            color = Color(0xFFFFF4C2),
            radius = w / 2f,
            center = center,
        )
        fun sparkle(cx: Float, cy: Float, radius: Float) {
            val path = Path().apply {
                moveTo(cx, cy - radius)
                cubicTo(cx + radius * 0.16f, cy - radius * 0.18f, cx + radius * 0.18f, cy - radius * 0.16f, cx + radius, cy)
                cubicTo(cx + radius * 0.18f, cy + radius * 0.16f, cx + radius * 0.16f, cy + radius * 0.18f, cx, cy + radius)
                cubicTo(cx - radius * 0.16f, cy + radius * 0.18f, cx - radius * 0.18f, cy + radius * 0.16f, cx - radius, cy)
                cubicTo(cx - radius * 0.18f, cy - radius * 0.16f, cx - radius * 0.16f, cy - radius * 0.18f, cx, cy - radius)
                close()
            }
            drawPath(path = path, color = Color(0xFFFFC400))
        }
        sparkle(w * 0.56f, h * 0.48f, w * 0.28f)
        sparkle(w * 0.34f, h * 0.36f, w * 0.18f)
        sparkle(w * 0.38f, h * 0.68f, w * 0.12f)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AccountAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CinemaBrandMark(size = size)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun YangoWordmark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Text(
        text = stringResource(R.string.app_name).uppercase().replace(" ", "\n"),
        modifier = modifier,
        style = (if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displayMedium).copy(
            fontWeight = FontWeight.Black,
            color = CinemaColors.White,
        ),
        lineHeight = if (compact) MaterialTheme.typography.headlineSmall.fontSize else MaterialTheme.typography.displayMedium.fontSize,
    )
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
