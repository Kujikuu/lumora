package com.iptvcinema.tv.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
private const val PRESSED_SCALE = 0.97f
private const val DISABLED_ALPHA = 0.4f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusableCinemaCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = CinemaShapes.Medium,
    defaultBorderWidth: Dp = 0.dp,
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
        animationSpec = tween(durationMillis = 120),
        label = "focusScale",
    )

    val borderColor = when {
        !enabled -> Color.Transparent
        isFocused -> CinemaColors.FocusBorder
        else -> if (defaultBorderWidth > 0.dp) CinemaColors.Border else Color.Transparent
    }
    val borderWidth = if (isFocused && enabled) focusedBorderWidth else defaultBorderWidth

    Box(
        modifier = modifier
            .zIndex(if (isFocused && enabled) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(width = borderWidth, color = borderColor, shape = shape)
                } else {
                    Modifier
                },
            )
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
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                },
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
