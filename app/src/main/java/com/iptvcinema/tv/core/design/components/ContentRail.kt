package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun <T> ContentRail(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    countLabel: String? = null,
    itemContent: @Composable (T) -> Unit,
) {
    Column(
        modifier = modifier.padding(bottom = CinemaSpacing.CardGap),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CinemaColors.TextPrimary,
                    ),
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium.copy(color = CinemaColors.TextMuted),
                    )
                }
            }
            if (!countLabel.isNullOrBlank()) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CinemaColors.GoldSoft,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
        ) {
            items.forEach { item ->
                itemContent(item)
            }
        }
    }
}
