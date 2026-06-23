package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

enum class CinemaButtonVariant {
    PrimaryGold,
    SecondaryDark,
    Danger,
    Ghost,
    Icon,
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaButton(
    text: String,
    variant: CinemaButtonVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
) {
    val (backgroundColor, contentColor) = when (variant) {
        CinemaButtonVariant.PrimaryGold -> Pair(
            CinemaColors.Gold,
            CinemaColors.Background,
        )
        CinemaButtonVariant.SecondaryDark -> Pair(
            CinemaColors.Surface,
            CinemaColors.TextPrimary,
        )
        CinemaButtonVariant.Danger -> Pair(
            CinemaColors.Danger,
            CinemaColors.TextPrimary,
        )
        CinemaButtonVariant.Ghost -> Pair(
            Color.Transparent,
            CinemaColors.TextPrimary,
        )
        CinemaButtonVariant.Icon -> Pair(
            CinemaColors.SurfaceSoft,
            CinemaColors.Gold,
        )
    }

    val shape = if (variant == CinemaButtonVariant.Icon) CinemaShapes.Small else CinemaShapes.Medium
    val horizontalPadding = if (variant == CinemaButtonVariant.Icon) 0.dp else 24.dp
    val minSize = if (variant == CinemaButtonVariant.Icon) 48.dp else 48.dp
    val showDefaultBorder = variant == CinemaButtonVariant.Ghost

    FocusableCinemaCard(
        modifier = modifier.defaultMinSize(
            minHeight = minSize,
            minWidth = if (variant == CinemaButtonVariant.Icon) minSize else 0.dp,
        ),
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        defaultBorderWidth = if (showDefaultBorder) 1.dp else 0.dp,
    ) { _ ->
        Row(
            modifier = (if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .background(backgroundColor, shape)
                .defaultMinSize(minWidth = if (variant == CinemaButtonVariant.Icon) minSize else 0.dp)
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(
                CinemaSpacing.ButtonGap / 2,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor,
                )
            }
            if (variant != CinemaButtonVariant.Icon || text.isNotEmpty()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    ),
                )
            }
        }
    }
}
