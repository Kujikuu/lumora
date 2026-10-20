package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.util.encodeQrCode

/** Large onboarding panels (source type cards, status panels). */
private val PanelShape = RoundedCornerShape(18.dp)
/** Source rows on the playlist management screen. */
private val SourceRowShape = RoundedCornerShape(16.dp)
private val TypeCardHeight = 170.dp
private val TypeIconTileSize = 44.dp
private val StatusDotSize = 20.dp
private val ActiveRingWidth = 2.dp
private const val TINT_ALPHA = 0.18f
private val DangerText = Color(0xFFFF6B6B)
/** Description text on a focused (white) type card. */
private val OnLightMuted = Color(0xFF4A4F5A)

/**
 * Source type choice on Add Source: an icon tile, a title and a one-line description.
 * Focused, the whole card turns white with dark content.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SourceTypeCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = PanelShape,
        focusedBorderWidth = 0.dp,
        focusScale = 1.03f,
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TypeCardHeight)
                .background(if (focused) CinemaColors.White else CinemaColors.Surface, PanelShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(TypeIconTileSize)
                        .background(
                            if (focused) CinemaColors.Background else CinemaColors.SurfaceRaised,
                            CinemaShapes.XLarge,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CinemaColors.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (focused) CinemaColors.Background else CinemaColors.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (focused) OnLightMuted else CinemaColors.TextMuted,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One connection or import step: a green check when done, a muted empty dot while pending. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ValidationStatusItem(
    label: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(StatusDotSize)
                .background(if (isSuccess) CinemaColors.Success else CinemaColors.SurfaceRaised, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSuccess) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CinemaColors.Background,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isSuccess) CinemaColors.TextPrimary else CinemaColors.TextMuted,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImportPreviewPanel(
    title: String,
    items: List<Pair<String, Boolean>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CinemaColors.SurfaceSoft, PanelShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PanelTitle(title)
        items.forEach { (label, success) ->
            ValidationStatusItem(label = label, isSuccess = success)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PanelTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Black,
            color = CinemaColors.White,
        ),
    )
}

/** Source health pill: tinted fill with text in the status colour. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusBadge(
    text: String,
    status: SourceStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        SourceStatus.ACTIVE -> CinemaColors.Success
        SourceStatus.SYNCING -> CinemaColors.Warning
        SourceStatus.NEEDS_ATTENTION -> CinemaColors.Warning
        SourceStatus.EXPIRED -> CinemaColors.Danger
        SourceStatus.FAILED -> CinemaColors.Danger
    }
    BadgeChip(
        text = text,
        modifier = modifier,
        backgroundColor = color.copy(alpha = TINT_ALPHA),
        textColor = if (color == CinemaColors.Danger) DangerText else color,
    )
}

/**
 * A playlist source row with Sync, Edit and Remove. The active source sits on a raised
 * surface with an accent ring; the others use the darker row surface.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SourceCard(
    name: String,
    type: SourceType,
    status: SourceStatus,
    channelCount: Int?,
    lastSynced: String,
    onSync: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onEdit,
        shape = SourceRowShape,
        focusScale = 1.02f,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) CinemaColors.Surface else CinemaColors.SurfaceSoft, SourceRowShape)
                .then(
                    if (isActive) {
                        Modifier.border(ActiveRingWidth, CinemaColors.Accent, SourceRowShape)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = CinemaColors.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusBadge(
                    text = status.name.replace('_', ' '),
                    status = status,
                )
            }
            Text(
                text = buildString {
                    append(type.name.replace('_', ' '))
                    channelCount?.let { append(" · $it channels") }
                    append(" · Last synced $lastSynced")
                },
                style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceActionButton(
                    text = stringResource(R.string.btn_sync),
                    restColor = CinemaColors.OverlayFill,
                    restTextColor = CinemaColors.White,
                    onClick = onSync,
                )
                SourceActionButton(
                    text = stringResource(R.string.btn_edit),
                    restColor = CinemaColors.White.copy(alpha = 0.10f),
                    restTextColor = CinemaColors.White,
                    onClick = onEdit,
                )
                SourceActionButton(
                    text = stringResource(R.string.btn_remove),
                    restColor = CinemaColors.Danger.copy(alpha = 0.16f),
                    restTextColor = DangerText,
                    onClick = onRemove,
                )
            }
        }
    }
}

/** Compact pill used inside source rows; white with dark text when focused. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceActionButton(
    text: String,
    restColor: Color,
    restTextColor: Color,
    onClick: () -> Unit,
) {
    FocusableCinemaCard(
        onClick = onClick,
        shape = CinemaShapes.Pill,
        focusedBorderWidth = 0.dp,
        focusScale = 1.04f,
    ) { focused ->
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 34.dp)
                .background(if (focused) CinemaColors.White else restColor, CinemaShapes.Pill)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (focused) CinemaColors.Background else restTextColor,
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SyncStatusPanel(
    channels: Int,
    movies: Int,
    series: Int,
    epgAvailable: Boolean,
    lastUpdate: String,
    modifier: Modifier = Modifier,
) {
    val lineStyle = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary)
    Column(
        modifier = modifier
            .background(CinemaColors.SurfaceSoft, SourceRowShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PanelTitle(stringResource(R.string.sync_status_title))
        Text(text = stringResource(R.string.sync_channels, channels), style = lineStyle)
        Text(text = stringResource(R.string.sync_movies, movies), style = lineStyle)
        Text(text = stringResource(R.string.sync_series, series), style = lineStyle)
        Text(
            text = if (epgAvailable) {
                stringResource(R.string.sync_epg_available)
            } else {
                stringResource(R.string.sync_epg_missing)
            },
            style = lineStyle.copy(color = if (epgAvailable) CinemaColors.Success else CinemaColors.TextMuted),
        )
        Text(
            text = stringResource(R.string.sync_last_update, lastUpdate),
            style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FooterNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(top = CinemaSpacing.SectionGap),
        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BenefitItem(
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = CinemaColors.Gold,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.TextPrimary),
                maxLines = 1,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(color = CinemaColors.TextMuted),
                    maxLines = 2,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QrActivationPanel(
    content: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 280.dp,
) {
    val qrBitmap = remember(content) {
        content.takeIf { it.isNotBlank() }?.let { encodeQrCode(it, 512) }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(PanelShape)
            .background(CinemaColors.White)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap,
                contentDescription = stringResource(R.string.cd_activation_qr),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CinemaShapes.XLarge)
                    .background(CinemaColors.Background.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "…",
                    style = MaterialTheme.typography.displaySmall.copy(color = CinemaColors.TextMuted),
                )
            }
        }
    }
}
