package com.iptvcinema.tv.core.design.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

data class RemoteHint(
    @StringRes val keyRes: Int,
    @StringRes val descriptionRes: Int,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RemoteHintBar(
    hints: List<RemoteHint>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        hints.forEach { hint ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(hint.keyRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CinemaColors.GoldSoft,
                    ),
                )
                Text(
                    text = stringResource(hint.descriptionRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = CinemaColors.TextMuted,
                    ),
                )
            }
        }
    }
}

@Composable
fun defaultRemoteHints(): List<RemoteHint> = listOf(
    RemoteHint(R.string.hint_navigate_key, R.string.hint_navigate_desc),
    RemoteHint(R.string.hint_select_key, R.string.hint_select_desc),
    RemoteHint(R.string.hint_back_key, R.string.hint_back_desc),
)

@Composable
fun searchRemoteHints(): List<RemoteHint> = defaultRemoteHints() + listOf(
    RemoteHint(R.string.hint_filter_key, R.string.hint_filter_desc),
    RemoteHint(R.string.hint_clear_key, R.string.hint_clear_desc),
)

@Composable
fun myListRemoteHints(): List<RemoteHint> = defaultRemoteHints() + listOf(
    RemoteHint(R.string.hint_longpress_key, R.string.hint_longpress_desc),
)
