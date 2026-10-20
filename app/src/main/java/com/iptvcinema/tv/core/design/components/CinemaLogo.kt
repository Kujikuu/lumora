package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors

/**
 * Brand lockup used at the top of onboarding and state screens: the Lumora mark beside the
 * app name. [navBar] shows the mark alone; [compact] uses a smaller lockup.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    navBar: Boolean = false,
) {
    val markSize = when {
        navBar -> 32.dp
        compact -> 28.dp
        else -> 36.dp
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.nav_sidebar_min),
            contentDescription = if (navBar) stringResource(R.string.app_name) else null,
            modifier = Modifier.size(markSize),
            contentScale = ContentScale.Fit,
        )
        if (!navBar) {
            Text(
                text = stringResource(R.string.app_name),
                style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
                    .copy(fontWeight = FontWeight.Black, color = CinemaColors.White),
                maxLines = 1,
                softWrap = false,
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
fun LumoraWordmark(
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
            .clip(com.iptvcinema.tv.core.design.theme.CinemaShapes.Pill)
            .background(CinemaColors.Surface)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextSecondary),
        )
    }
}
