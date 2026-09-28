package com.iptvcinema.tv.features.states

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.AccountAvatar
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaLogo
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.navigation.AppRoute

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyStateScreen(
    navController: NavController,
) {
    BackHandler { navController.popBackStack() }

    CinemaScreen(showTopNav = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
        ) {
            ReloadPosterWall(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 120.dp)
                    .graphicsLayer(rotationZ = -5f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CinemaColors.Background,
                                CinemaColors.Background.copy(alpha = 0.78f),
                                CinemaColors.Background.copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 154.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                CinemaLogo()
                Text(
                    text = stringResource(R.string.reload_title),
                    modifier = Modifier.width(960.dp),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Black,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CinemaButton(
                        text = stringResource(R.string.btn_reload),
                        variant = CinemaButtonVariant.PrimaryAccent,
                        onClick = { navController.navigate(AppRoute.ADD_SOURCE) },
                    )
                    CinemaButton(
                        text = stringResource(R.string.btn_change_account),
                        variant = CinemaButtonVariant.SecondaryDark,
                        icon = null,
                        onClick = { navController.navigate(AppRoute.WELCOME) },
                    )
                }
            }
            AccountAvatar(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 376.dp, top = 232.dp),
                size = 56.dp,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ErrorStateScreen(
    navController: NavController,
) {
    BackHandler { navController.popBackStack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.error_server_not_responding),
                    style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.TextSecondary),
                )
                Text(
                    text = stringResource(R.string.error_reload_page),
                    style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.TextSecondary),
                )
            }
            CinemaButton(
                text = stringResource(R.string.btn_close),
                variant = CinemaButtonVariant.PrimaryAccent,
                onClick = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun ReloadPosterWall(
    modifier: Modifier = Modifier,
) {
    val images = remember {
        (FakeDataProvider.movies.mapNotNull { it.backdropUrl ?: it.imageUrl } +
            FakeDataProvider.seriesList.mapNotNull { it.backdropUrl ?: it.imageUrl })
            .take(15)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        images.chunked(5).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.offset(x = if (rowIndex % 2 == 0) 0.dp else 150.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                row.forEach { imageUrl ->
                    Box(
                        modifier = Modifier
                            .size(width = 430.dp, height = 238.dp)
                            .clip(CinemaShapes.Medium)
                            .background(CinemaColors.SurfaceSoft),
                    ) {
                        CinemaAsyncImage(
                            imageUrl = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fallbackLabel = "",
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CinemaColors.Background.copy(alpha = 0.28f)),
                        )
                    }
                }
            }
        }
    }
}
