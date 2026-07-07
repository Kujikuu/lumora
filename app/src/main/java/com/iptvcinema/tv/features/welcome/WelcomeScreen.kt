package com.iptvcinema.tv.features.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaLogo
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.navigation.BlockBackHandler

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
) {
    val buttonFocus = remember { FocusRequester() }

    BlockBackHandler()

    LaunchedEffect(Unit) {
        buttonFocus.requestFocus()
    }

    CinemaScreen(showTopNav = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
        ) {
            PosterWall(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 170.dp)
                    .graphicsLayer(rotationZ = -8f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CinemaColors.Background,
                                CinemaColors.Background.copy(alpha = 0.92f),
                                CinemaColors.Background.copy(alpha = 0.52f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CinemaColors.Background.copy(alpha = 0.20f),
                                Color.Transparent,
                                CinemaColors.Background.copy(alpha = 0.40f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 168.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                CinemaLogo()
                Spacer(Modifier.height(140.dp))
                Text(
                    text = stringResource(R.string.welcome_title).uppercase(),
                    modifier = Modifier.width(820.dp),
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Black,
                    ),
                    maxLines = 2,
                )
                CinemaButton(
                    text = stringResource(R.string.welcome_get_started),
                    variant = CinemaButtonVariant.PrimaryAccent,
                    onClick = onGetStarted,
                    modifier = Modifier.focusRequester(buttonFocus),
                )
            }

            Text(
                text = stringResource(R.string.welcome_explore),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun PosterWall(
    modifier: Modifier = Modifier,
) {
    val images = remember {
        (FakeDataProvider.movies.mapNotNull { it.imageUrl } + FakeDataProvider.seriesList.mapNotNull { it.imageUrl })
            .take(12)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        images.chunked(4).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.offset(x = if (rowIndex % 2 == 0) 0.dp else 130.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                row.forEach { imageUrl ->
                    Box(
                        modifier = Modifier
                            .size(width = 330.dp, height = 190.dp)
                            .clip(CinemaShapes.Medium)
                            .background(CinemaColors.Surface),
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
                                .background(CinemaColors.Background.copy(alpha = 0.26f)),
                        )
                    }
                }
            }
        }
    }
}
