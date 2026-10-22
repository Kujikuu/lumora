package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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

data class SettingsMenuItem(
    val label: String,
    val icon: ImageVector? = null,
)

private val MenuItemMinHeight = 38.dp
private val ValueRowMinHeight = 50.dp
private val ToggleRowMinHeight = 50.dp
private val ToggleRowWithDescriptionMinHeight = 62.dp
private val SettingsChipMinHeight = 34.dp
private const val ROW_FOCUS_SCALE = 1.02f
private const val CHIP_FOCUS_SCALE = 1.04f

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsPanelHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
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
        modifier = modifier.padding(start = 6.dp, bottom = 4.dp),
        style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextMuted),
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
        verticalArrangement = Arrangement.spacedBy(3.dp),
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
 * A quiet pill: no fill at rest, a raised fill for the open section, white when focused,
 * so "where am I" and "where is focus" never look the same.
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
        shape = CinemaShapes.Pill,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = 1.0f,
    ) { focused ->
        val background = when {
            focused -> CinemaColors.White
            isSelected -> CinemaColors.SurfaceRaised
            else -> Color.Transparent
        }
        val contentColor = when {
            focused -> CinemaColors.Background
            isSelected -> CinemaColors.White
            else -> CinemaColors.TextMuted
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MenuItemMinHeight)
                .background(background, CinemaShapes.Pill)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = contentColor,
                    fontWeight = if (isSelected || focused) FontWeight.ExtraBold else FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A value row: label at the start, the current value and an optional chevron at the end.
 * [showChevron] defaults to rows without a value, which open something.
 */
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
    showChevron: Boolean = trailing == null,
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        shape = CinemaShapes.Row,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = ROW_FOCUS_SCALE,
    ) { focused ->
        val rowBackground = when {
            !enabled -> CinemaColors.Surface.copy(alpha = 0.5f)
            focused -> CinemaColors.White
            isSelected -> CinemaColors.SurfaceRaised
            else -> CinemaColors.Surface
        }
        val labelColor = when {
            focused -> CinemaColors.Background
            !enabled -> CinemaColors.TextSecondary
            else -> CinemaColors.White
        }
        val valueColor = when {
            focused -> SettingsPalette.FocusedSecondaryText
            !enabled -> CinemaColors.TextMuted
            else -> CinemaColors.TextSecondary
        }
        val iconTint = if (focused) SettingsPalette.FocusedSecondaryText else CinemaColors.TextMuted
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ValueRowMinHeight)
                .background(rowBackground, CinemaShapes.Row)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    modifier = Modifier.widthIn(max = 280.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = valueColor,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                trailingIcon != null && enabled -> {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = stringResource(R.string.settings_expand_collapse),
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                showChevron && enabled -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
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
    description: String? = null,
) {
    val stateText = if (isOn) stringResource(R.string.toggle_on) else stringResource(R.string.toggle_off)
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggle,
        shape = CinemaShapes.Row,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = ROW_FOCUS_SCALE,
    ) { focused ->
        val hasDescription = !description.isNullOrBlank()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(
                    minHeight = if (hasDescription) ToggleRowWithDescriptionMinHeight else ToggleRowMinHeight,
                )
                .background(
                    if (focused) CinemaColors.White else CinemaColors.Surface,
                    CinemaShapes.Row,
                )
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (focused) CinemaColors.Background else CinemaColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                if (hasDescription) {
                    Text(
                        text = description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (focused) SettingsPalette.FocusedSecondaryText else CinemaColors.TextMuted,
                        ),
                    )
                }
            }
            SettingsSwitch(isOn = isOn, stateText = stateText)
        }
    }
}

/** Static switch: accent track when on; the knob sits at the end edge, so it mirrors in RTL. */
@Composable
private fun SettingsSwitch(isOn: Boolean, stateText: String) {
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 24.dp)
            .background(
                if (isOn) CinemaColors.Accent else SettingsPalette.SwitchTrackOff,
                CinemaShapes.Pill,
            )
            .padding(3.dp)
            .semantics { stateDescription = stateText },
        contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    if (isOn) CinemaColors.White else SettingsPalette.SwitchKnobOff,
                    CircleShape,
                ),
        )
    }
}

/** How a [SettingsChip] looks at rest. Focus is always a white pill with dark text. */
enum class SettingsChipStyle {
    /** No fill at rest; the selected chip gets a raised fill (profile pickers). */
    Quiet,
    /** Surface fill at rest; the selected chip is filled with the accent (ratings). */
    Filled,
    /** Red tint for blocked or destructive items. */
    Danger,
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SettingsChipStyle = SettingsChipStyle.Filled,
    trailingIcon: ImageVector? = null,
) {
    FocusableCinemaCard(
        modifier = modifier,
        onClick = onClick,
        shape = CinemaShapes.Pill,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = CHIP_FOCUS_SCALE,
    ) { focused ->
        val background = when {
            focused -> CinemaColors.White
            style == SettingsChipStyle.Danger -> SettingsPalette.DangerTint
            style == SettingsChipStyle.Quiet -> if (isSelected) CinemaColors.SurfaceRaised else Color.Transparent
            isSelected -> CinemaColors.Accent
            else -> CinemaColors.Surface
        }
        val contentColor = when {
            focused && style == SettingsChipStyle.Danger -> CinemaColors.Danger
            focused && isSelected && style == SettingsChipStyle.Filled -> CinemaColors.Accent
            focused -> CinemaColors.Background
            style == SettingsChipStyle.Danger -> SettingsPalette.DangerText
            style == SettingsChipStyle.Quiet && !isSelected -> CinemaColors.TextMuted
            else -> CinemaColors.White
        }
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = SettingsChipMinHeight, minWidth = 48.dp)
                .background(background, CinemaShapes.Pill)
                .padding(horizontal = 14.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                ),
                maxLines = 1,
            )
            trailingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
            }
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
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            SettingsChip(
                label = option,
                isSelected = index == selectedIndex,
                onClick = { onSelected(index) },
                style = SettingsChipStyle.Filled,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockedCategoryList(
    categories: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_blocked_categories),
            style = MaterialTheme.typography.titleSmall.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        // Wraps to new lines, so long or many category names never get squeezed or clipped.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                Text(
                    text = category,
                    modifier = Modifier
                        .background(SettingsPalette.DangerTint, CinemaShapes.Pill)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = SettingsPalette.DangerText,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                )
            }
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
    SettingsChip(
        label = name,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        style = SettingsChipStyle.Quiet,
    )
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
