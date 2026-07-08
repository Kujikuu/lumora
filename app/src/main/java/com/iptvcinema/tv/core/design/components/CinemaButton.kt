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
    PrimaryAccent,
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
        CinemaButtonVariant.PrimaryAccent -> Pair(
            CinemaColors.Accent,
            CinemaColors.White,
        )
        CinemaButtonVariant.SecondaryDark -> Pair(
            CinemaColors.SurfaceGlass,
            CinemaColors.White,
        )
        CinemaButtonVariant.Danger -> Pair(
            CinemaColors.Danger,
            CinemaColors.White,
        )
        CinemaButtonVariant.Ghost -> Pair(
            CinemaColors.Surface.copy(alpha = 0.52f),
            CinemaColors.White,
        )
        CinemaButtonVariant.Icon -> Pair(
            CinemaColors.SurfaceGlass,
            CinemaColors.White,
        )
    }

    val shape = CinemaShapes.Pill
    val horizontalPadding = if (variant == CinemaButtonVariant.Icon) 0.dp else 20.dp
    val minSize = 46.dp

    FocusableCinemaCard(
        modifier = modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier),
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        defaultBorderWidth = 0.dp,
        focusScale = 1.02f,
    ) { _ ->
        Row(
            modifier = Modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(
                    minHeight = minSize,
                    minWidth = if (variant == CinemaButtonVariant.Icon) minSize else 0.dp,
                )
                .background(backgroundColor, shape)
                .padding(horizontal = horizontalPadding, vertical = 9.dp),
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
                    modifier = Modifier.size(18.dp),
                    tint = contentColor,
                )
            }
            if (variant != CinemaButtonVariant.Icon || text.isNotEmpty()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = contentColor,
                    ),
                )
            }
        }
    }
}
