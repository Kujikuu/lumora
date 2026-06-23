package com.iptvcinema.tv.core.design.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.ChannelTile
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.ContentRail
import com.iptvcinema.tv.core.design.components.PosterCard
import com.iptvcinema.tv.core.design.components.PosterCardVariant
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.NavItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DesignSystemShowcase() {
    var selectedNav by remember { mutableStateOf(NavItem.Home) }

    CinemaScreen(
        selectedNavItem = selectedNav,
        onNavigate = { selectedNav = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
        ) {
            Text(
                text = stringResource(R.string.showcase_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                text = stringResource(R.string.showcase_buttons),
                style = MaterialTheme.typography.titleMedium.copy(color = CinemaColors.TextSecondary),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
            ) {
                CinemaButton(
                    text = stringResource(R.string.btn_watch_now),
                    variant = CinemaButtonVariant.PrimaryGold,
                    icon = Icons.Default.PlayArrow,
                    onClick = {},
                )
                CinemaButton(
                    text = stringResource(R.string.btn_details),
                    variant = CinemaButtonVariant.SecondaryDark,
                    onClick = {},
                )
                CinemaButton(
                    text = stringResource(R.string.btn_remove),
                    variant = CinemaButtonVariant.Danger,
                    onClick = {},
                )
                CinemaButton(
                    text = stringResource(R.string.btn_cancel),
                    variant = CinemaButtonVariant.Ghost,
                    onClick = {},
                )
                CinemaButton(
                    text = "",
                    variant = CinemaButtonVariant.Icon,
                    icon = Icons.Default.Search,
                    onClick = {},
                )
            }

            ContentRail(
                title = stringResource(R.string.showcase_trending),
                items = FakeDataProvider.samplePosters(),
            ) { poster ->
                PosterCard(
                    data = poster,
                    variant = PosterCardVariant.PortraitPoster,
                    onClick = {},
                )
            }

            ContentRail(
                title = stringResource(R.string.showcase_channels),
                items = FakeDataProvider.sampleChannels(),
            ) { channel ->
                ChannelTile(
                    data = channel,
                    onClick = {},
                )
            }

            ContentRail(
                title = stringResource(R.string.showcase_compact),
                items = FakeDataProvider.samplePosters().take(4),
            ) { poster ->
                PosterCard(
                    data = poster,
                    variant = PosterCardVariant.CompactPoster,
                    onClick = {},
                )
            }
        }
    }
}
