package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
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
            fontWeight = FontWeight.Black,
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
            fontWeight = FontWeight.ExtraBold,
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
        text = items.joinToString(separator = "  •  "),
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
            .clip(CinemaShapes.Pill)
            .background(CinemaColors.Accent)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = CinemaColors.White,
            ),
        )
    }
}

/**
 * Small uppercase label above a screen's content ("MOVIES", "SERIES"), muted so the spotlight
 * title stays the loudest text. Letter spacing is dropped in RTL so Arabic letters stay joined.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreenEyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = CinemaColors.TextMuted,
            letterSpacing = if (isRtl) 0.sp else 2.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Translucent pill with a back chevron and where Back returns to. Not focusable: Back does it. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BackPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CinemaShapes.Pill)
            .background(CinemaColors.Background.copy(alpha = 0.6f))
            .padding(start = 10.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = CinemaColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.TextSecondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Pill badge with a thin light outline and no fill, for quality labels such as HD. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OutlineBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(1.dp, CinemaColors.White.copy(alpha = 0.5f), CinemaShapes.Pill)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = CinemaColors.White,
            ),
        )
    }
}

/** Red LIVE pill with a small white dot, as on channel heroes and lineup cards. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveDotBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CinemaShapes.Pill)
            .background(CinemaColors.LiveRed)
            .padding(horizontal = 9.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(CinemaColors.White, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.White,
            ),
        )
    }
}

/**
 * Rounded progress track drawn in one pass (no child layouts), filling from the start edge in
 * both directions. Used inset at the bottom of artwork and under now-playing programmes.
 */
@Composable
fun InsetProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = CinemaColors.White.copy(alpha = 0.3f),
    fillColor: Color = CinemaColors.Accent,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(height)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(trackColor, cornerRadius = radius)
                val fillWidth = size.width * clamped
                if (fillWidth <= 0f) return@drawBehind
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f
                drawRoundRect(
                    fillColor,
                    topLeft = Offset(left, 0f),
                    size = Size(fillWidth, size.height),
                    cornerRadius = radius,
                )
            },
    )
}
