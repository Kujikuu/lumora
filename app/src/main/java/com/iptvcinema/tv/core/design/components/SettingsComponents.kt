package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

data class SettingsMenuItem(
    val label: String,
    val icon: ImageVector? = null,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsPanelHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Black,
            ),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsHintText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp),
        style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsMenu(
    items: List<SettingsMenuItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    focusedItemIndex: Int = 0,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            SettingsMenuRow(
                item = item,
                isSelected = index == selectedIndex,
                onClick = { onSelected(index) },
                modifier = if (index == focusedItemIndex && firstItemFocusRequester != null) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * A quiet list row: no background until focused. The open section is marked with an accent
 * bar and bold white text, so "where am I" and "where is focus" never look the same.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsMenuRow(
    item: SettingsMenuItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = CinemaShapes.Medium,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = 1.0f,
    ) { focused ->
        val contentColor = when {
            focused -> CinemaColors.Background
            isSelected -> CinemaColors.White
            else -> CinemaColors.TextSecondary
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(if (focused) CinemaColors.White else Color.Transparent, CinemaShapes.Medium)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(CinemaShapes.Pill)
                    .background(if (isSelected && !focused) CinemaColors.Accent else Color.Transparent),
            )
            Spacer(Modifier.width(12.dp))
            item.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = contentColor,
                    fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: String? = null,
    trailingIcon: ImageVector? = null,
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        shape = CinemaShapes.Pill,
        defaultBorderWidth = 0.dp,
        focusScale = 1.02f,
    ) { focused ->
        val rowBackground = when {
            !enabled -> CinemaColors.SurfaceSoft.copy(alpha = 0.5f)
            focused -> CinemaColors.White
            isSelected -> CinemaColors.White
            else -> CinemaColors.SurfaceSoft
        }
        val rowContent = when {
            focused || isSelected -> CinemaColors.Background
            !enabled -> CinemaColors.TextMuted
            else -> CinemaColors.White
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .background(rowBackground, CinemaShapes.Pill)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = rowContent,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                trailing != null -> {
                    Text(
                        text = trailing,
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (focused || isSelected) CinemaColors.TextMuted else CinemaColors.TextSecondary,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                trailingIcon != null && enabled -> {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = stringResource(R.string.settings_expand_collapse),
                        tint = if (focused || isSelected) CinemaColors.TextMuted else CinemaColors.TextSecondary,
                        modifier = Modifier.size(34.dp),
                    )
                }
                enabled -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (focused || isSelected) CinemaColors.TextMuted else CinemaColors.TextSecondary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsToggle(
    label: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggle,
        shape = CinemaShapes.Small,
        defaultBorderWidth = 0.dp,
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (focused) CinemaColors.Surface else CinemaColors.SurfaceSoft,
                    CinemaShapes.Small,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextPrimary))
            Text(
                text = if (isOn) stringResource(R.string.toggle_on) else stringResource(R.string.toggle_off),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isOn) CinemaColors.Success else CinemaColors.TextMuted,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RatingRestrictionSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChipRow(
        items = options,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BlockedCategoryList(
    categories: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_blocked_categories),
            style = MaterialTheme.typography.titleMedium.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Bold,
            ),
        )
        categories.forEach { category ->
            Text(
                text = stringResource(R.string.settings_category_bullet, category),
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CategoryChip(label = name, isSelected = isSelected, onClick = onClick, modifier = modifier)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyState(
    title: String,
    description: String,
    primaryAction: String,
    secondaryAction: String?,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?,
    footerNote: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CinemaLogo()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
            ),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            CinemaButton(text = primaryAction, variant = CinemaButtonVariant.PrimaryAccent, onClick = onPrimary)
            if (secondaryAction != null && onSecondary != null) {
                CinemaButton(text = secondaryAction, variant = CinemaButtonVariant.SecondaryDark, onClick = onSecondary)
            }
        }
        if (footerNote != null) {
            FooterNote(text = footerNote)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ErrorState(
    title: String,
    description: String,
    errorCode: String?,
    onRetry: () -> Unit,
    onSwitchStream: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showSwitchStream: Boolean = true,
    switchStreamLabel: String? = null,
    backLabel: String? = null,
) {
    val resolvedSwitchLabel = switchStreamLabel ?: stringResource(R.string.btn_switch_stream)
    val resolvedBackLabel = backLabel ?: stringResource(R.string.btn_back_to_guide)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.White,
            ),
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            CinemaButton(text = stringResource(R.string.btn_try_again), variant = CinemaButtonVariant.PrimaryAccent, onClick = onRetry)
            if (showSwitchStream) {
                CinemaButton(text = resolvedSwitchLabel, variant = CinemaButtonVariant.SecondaryDark, onClick = onSwitchStream)
            }
            CinemaButton(text = resolvedBackLabel, variant = CinemaButtonVariant.Ghost, onClick = onBack)
        }
        if (errorCode != null) {
            Text(
                text = stringResource(R.string.error_code_label, errorCode),
                style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
