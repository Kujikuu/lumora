package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

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
            firstOptionFocus.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .background(CinemaColors.Surface, CinemaShapes.Large)
                .padding(CinemaSpacing.SectionGap),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                options.forEachIndexed { index, option ->
                    ContextMenuOptionRow(
                        label = option.label,
                        destructive = option.destructive,
                        onClick = { onOptionSelected(option) },
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstOptionFocus)
                        } else {
                            Modifier
                        },
                    )
                }
            }
            CinemaButton(
                text = stringResource(R.string.btn_cancel),
                variant = CinemaButtonVariant.Ghost,
                onClick = onDismiss,
                fullWidth = true,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContextMenuOptionRow(
    label: String,
    destructive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = CinemaShapes.Pill,
        defaultBorderWidth = 0.dp,
        focusScale = 1.02f,
    ) { focused ->
        val background = when {
            focused -> if (destructive) CinemaColors.Danger else CinemaColors.White
            else -> CinemaColors.SurfaceSoft
        }
        val contentColor = when {
            focused -> if (destructive) CinemaColors.White else CinemaColors.Background
            destructive -> CinemaColors.Danger
            else -> CinemaColors.White
        }
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .background(background, CinemaShapes.Pill)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Black,
                color = contentColor,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
