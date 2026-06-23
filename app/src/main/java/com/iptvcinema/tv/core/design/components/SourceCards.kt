package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.util.encodeQrCode

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SourceTypeCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick,
        shape = CinemaShapes.Large,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinemaColors.SurfaceSoft, CinemaShapes.Large)
                .padding(CinemaSpacing.SectionGap),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CinemaColors.Gold,
                ),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ValidationStatusItem(
    label: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isSuccess) CinemaColors.Success else CinemaColors.Warning,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextPrimary),
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
            .clip(CinemaShapes.Large)
            .background(CinemaColors.SurfaceGlass)
            .padding(CinemaSpacing.SectionGap),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = CinemaColors.Gold,
            ),
        )
        items.forEach { (label, success) ->
            ValidationStatusItem(label = label, isSuccess = success)
        }
    }
}

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
    BadgeChip(text = text, modifier = modifier, backgroundColor = color)
}

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
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onEdit,
        shape = CinemaShapes.Large,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinemaColors.SurfaceSoft, CinemaShapes.Large)
                .padding(CinemaSpacing.SectionGap),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                CinemaButton(text = stringResource(R.string.btn_sync), variant = CinemaButtonVariant.SecondaryDark, onClick = onSync)
                CinemaButton(text = stringResource(R.string.btn_edit), variant = CinemaButtonVariant.Ghost, onClick = onEdit)
                CinemaButton(text = stringResource(R.string.btn_remove), variant = CinemaButtonVariant.Danger, onClick = onRemove)
            }
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
    Column(
        modifier = modifier
            .clip(CinemaShapes.Large)
            .background(CinemaColors.SurfaceGlass)
            .padding(CinemaSpacing.SectionGap),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.sync_status_title),
            style = MaterialTheme.typography.titleMedium.copy(color = CinemaColors.Gold),
        )
        Text(text = stringResource(R.string.sync_channels, channels), style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(R.string.sync_movies, movies), style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(R.string.sync_series, series), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (epgAvailable) {
                stringResource(R.string.sync_epg_available)
            } else {
                stringResource(R.string.sync_epg_missing)
            },
            style = MaterialTheme.typography.bodyMedium,
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
            .clip(CinemaShapes.Large)
            .background(CinemaColors.SurfaceGlass)
            .border(1.dp, CinemaColors.Gold, CinemaShapes.Large)
            .padding(size * 0.06f),
        contentAlignment = Alignment.Center,
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap,
                contentDescription = stringResource(R.string.cd_activation_qr),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CinemaShapes.Small),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CinemaColors.TextPrimary.copy(alpha = 0.08f), CinemaShapes.Small),
            )
        }
    }
}
