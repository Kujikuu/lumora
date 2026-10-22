package com.iptvcinema.tv.core.design.components

import com.iptvcinema.tv.core.navigation.requestFocusWhenReady
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

data class CinemaMenuOption(
    val id: String,
    val label: String,
    val destructive: Boolean = false,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaContextMenuDialog(
    title: String,
    options: List<CinemaMenuOption>,
    onDismiss: () -> Unit,
    onOptionSelected: (CinemaMenuOption) -> Unit,
) {
    val firstOptionFocus = remember(options, title) { FocusRequester() }

    LaunchedEffect(options, title) {
        if (options.isNotEmpty()) {
            firstOptionFocus.requestFocusWhenReady()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(CONTEXT_MENU_WIDTH_FRACTION)
                .background(CinemaColors.Surface, DialogPanelShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            options.forEachIndexed { index, option ->
                DialogPillAction(
                    label = option.label,
                    onClick = { onOptionSelected(option) },
                    restContent = if (option.destructive) SettingsPalette.DangerText else CinemaColors.White,
                    focusedContent = if (option.destructive) CinemaColors.Danger else CinemaColors.Background,
                    fillWidth = true,
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstOptionFocus)
                    } else {
                        Modifier
                    },
                )
            }
            DialogPillAction(
                label = stringResource(R.string.btn_cancel),
                onClick = onDismiss,
                restContent = CinemaColors.TextMuted,
                fillWidth = true,
                minHeight = 40.dp,
                textStyle = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** Rounded corners shared by the context menu and confirm dialog panels. */
internal val DialogPanelShape = RoundedCornerShape(16.dp)

private const val CONTEXT_MENU_WIDTH_FRACTION = 0.36f
private const val DIALOG_ACTION_FOCUS_SCALE = 1.02f
private const val DISABLED_CONTENT_ALPHA = 0.5f

/**
 * A pill action for dialogs. At rest it shows [restBackground] and [restContent]; focused, it is
 * a white pill with [focusedContent], matching focus everywhere else in the app.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun DialogPillAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    restBackground: Color = Color.Transparent,
    restContent: Color = CinemaColors.White,
    focusedContent: Color = CinemaColors.Background,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    minHeight: Dp = 44.dp,
    textStyle: TextStyle? = null,
) {
    FocusableCinemaCard(
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        onClick = onClick,
        enabled = enabled,
        shape = CinemaShapes.Pill,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = DIALOG_ACTION_FOCUS_SCALE,
    ) { focused ->
        val contentColor = when {
            focused -> focusedContent
            !enabled -> restContent.copy(alpha = DISABLED_CONTENT_ALPHA)
            else -> restContent
        }
        Text(
            text = label,
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(minHeight = minHeight)
                .background(if (focused) CinemaColors.White else restBackground, CinemaShapes.Pill)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            style = (textStyle ?: MaterialTheme.typography.labelLarge).copy(
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
