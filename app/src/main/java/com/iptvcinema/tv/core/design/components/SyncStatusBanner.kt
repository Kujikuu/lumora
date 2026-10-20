package com.iptvcinema.tv.core.design.components

import com.iptvcinema.tv.core.navigation.requestFocusWhenReady
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.catalog.CatalogRefreshState
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SyncStatusBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    subtitle: String? = null,
    progress: Float? = null,
    isSuccess: Boolean = false,
    isFailed: Boolean = false,
    requestFocusOnRefresh: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isRefreshing, requestFocusOnRefresh) {
        if (isRefreshing && requestFocusOnRefresh) {
            focusRequester.requestFocusWhenReady()
        }
    }

    val iconTint = when {
        isFailed -> CinemaColors.Danger
        isSuccess -> CinemaColors.Success
        else -> CinemaColors.Accent
    }
    val icon = when {
        isFailed -> Icons.Default.Error
        isSuccess -> Icons.Default.CheckCircle
        else -> Icons.Default.Refresh
    }
    val showProgress = isRefreshing && progress != null
    // A pill while it is one line of status; a rounded card once a progress bar sits under it.
    val shape = if (showProgress) CinemaShapes.Row else CinemaShapes.Pill

    FocusableCinemaCard(
        modifier = modifier
            .widthIn(min = 280.dp, max = 420.dp)
            .focusRequester(focusRequester),
        enabled = true,
        onClick = {
            if (!isRefreshing) {
                onClick()
            }
        },
        shape = shape,
        focusedBorderWidth = 0.dp,
        focusScale = BANNER_FOCUS_SCALE,
        contentDescription = text,
    ) { focused ->
        val titleColor = if (focused) CinemaColors.Background else CinemaColors.White
        val subtitleColor = if (focused) SettingsPalette.FocusedSecondaryText else CinemaColors.TextMuted
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = BannerMinHeight)
                .background(if (focused) CinemaColors.White else CinemaColors.SurfaceRaised, shape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isRefreshing) {
                    SyncRing(modifier = Modifier.size(18.dp))
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = titleColor,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let { stepText ->
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.labelSmall.copy(color = subtitleColor),
                        )
                    }
                }
            }
            if (showProgress) {
                CinemaProgressBar(
                    fraction = progress ?: 0f,
                    modifier = Modifier.clip(CinemaShapes.Pill),
                )
            }
        }
    }
}

/** A static ring with an accent arc, standing in for a spinner without running an animation. */
@Composable
private fun SyncRing(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawBehind {
            val strokeWidth = 2.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = SettingsPalette.SwitchTrackOff,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = CinemaColors.Accent,
                startAngle = -90f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        },
    )
}

private const val BANNER_FOCUS_SCALE = 1.03f
private val BannerMinHeight = 52.dp

@Composable
fun CatalogRefreshBanner(
    syncBannerText: String?,
    refreshState: CatalogRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bannerText = when (refreshState) {
        CatalogRefreshState.Idle -> null
        is CatalogRefreshState.Refreshing -> stringResource(R.string.refresh_in_progress)
        is CatalogRefreshState.Success -> refreshState.message
        is CatalogRefreshState.Failed -> refreshState.message
    }
    bannerText?.let { text ->
        val refreshing = refreshState as? CatalogRefreshState.Refreshing
        SyncStatusBanner(
            text = text,
            isRefreshing = refreshing != null,
            subtitle = refreshing?.stepLabel,
            progress = refreshing?.progress,
            isSuccess = refreshState is CatalogRefreshState.Success,
            isFailed = refreshState is CatalogRefreshState.Failed,
            onClick = onRefresh,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AccountDegradedBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CinemaColors.SurfaceRaised)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = CinemaColors.Warning,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.account_degraded_message),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
        )
        CinemaButton(
            text = stringResource(R.string.account_degraded_retry),
            variant = CinemaButtonVariant.SecondaryDark,
            onClick = onRetry,
        )
    }
}
