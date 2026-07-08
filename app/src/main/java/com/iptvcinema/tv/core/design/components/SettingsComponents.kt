package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.R
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.AccountSummary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsMenu(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    focusedItemIndex: Int = 0,
) {
    Column(
        modifier = modifier.width(260.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            SettingsRow(
                label = item,
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
                .defaultMinSize(minHeight = 92.dp)
                .background(rowBackground, CinemaShapes.Pill)
                .padding(horizontal = 38.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = rowContent,
                ),
            )
            when {
                trailing != null -> {
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (focused || isSelected) CinemaColors.TextMuted else CinemaColors.TextSecondary,
                            fontWeight = FontWeight.Bold,
                        ),
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
                        imageVector = Icons.Default.ChevronRight,
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
fun AccountSummaryCard(
    account: AccountSummary,
    onManageAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CinemaShapes.Medium)
            .background(CinemaColors.SurfaceSoft)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CinemaShapes.Medium)
                    .background(CinemaColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = account.name.take(1),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CinemaColors.White,
                    ),
                )
                Text(text = account.email, style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary))
                Text(text = account.plan, style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.Accent))
                Text(
                    text = stringResource(R.string.settings_renews, account.renewalDate),
                    style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                )
            }
        }
        CinemaButton(
            text = stringResource(R.string.btn_manage_account),
            variant = CinemaButtonVariant.SecondaryDark,
            onClick = onManageAccount,
        )
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
