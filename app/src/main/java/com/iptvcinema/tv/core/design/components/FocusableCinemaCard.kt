package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

private const val FOCUSED_SCALE = 1.04f
private const val PRESSED_SCALE = 0.98f
private const val DISABLED_ALPHA = 0.4f

@Composable
fun FocusableCinemaCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: RoundedCornerShape = CinemaShapes.Medium,
    defaultBorderWidth: Dp = 1.dp,
    focusedBorderWidth: Dp = 2.dp,
    focusScale: Float = FOCUSED_SCALE,
    contentDescription: String? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }

    val targetScale = when {
        !enabled -> 1f
        isPressed -> PRESSED_SCALE
        isFocused -> focusScale
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 150),
        label = "focusScale",
    )

    val borderColor = when {
        !enabled -> CinemaColors.Border.copy(alpha = DISABLED_ALPHA)
        isFocused -> CinemaColors.FocusBorder
        else -> CinemaColors.Border
    }
    val borderWidth = if (isFocused && enabled) focusedBorderWidth else defaultBorderWidth

    Box(
        modifier = modifier
            .zIndex(if (isFocused && enabled) 1f else 0f)
            .scale(scale)
            .then(
                if (isFocused && enabled) {
                    Modifier.shadow(
                        elevation = 18.dp,
                        shape = shape,
                        ambientColor = CinemaColors.Gold.copy(alpha = 0.44f),
                        spotColor = CinemaColors.Gold.copy(alpha = 0.36f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (
                    enabled &&
                    event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(isFocused && enabled)
    }
}

@Composable
fun BoxScope.FocusableCardSurface(
    backgroundColor: Color,
    shape: RoundedCornerShape = CinemaShapes.Medium,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(backgroundColor, shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}
