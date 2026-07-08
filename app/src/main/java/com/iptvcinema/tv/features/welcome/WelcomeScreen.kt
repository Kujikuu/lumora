package com.iptvcinema.tv.features.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.navigation.BlockBackHandler
import kotlinx.coroutines.delay

private const val WelcomePageCount = 4
private const val WelcomeNavCooldownMs = 320L
private val WelcomeBrandLogoWidth = 420.dp
private val WelcomeBrandLogoHeight = 73.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
) {
    val buttonFocus = remember { FocusRequester() }
    var currentPage by remember { mutableIntStateOf(0) }
    var lastNavAt by remember { mutableLongStateOf(0L) }

    BlockBackHandler()

    LaunchedEffect(currentPage) {
        delay(120)
        runCatching { buttonFocus.requestFocus() }
    }

    fun navigatePage(delta: Int): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastNavAt < WelcomeNavCooldownMs) return true
        val target = currentPage + delta
        if (target !in 0 until WelcomePageCount) return false
        lastNavAt = now
        currentPage = target
        return true
    }

    CinemaScreen(showTopNav = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true
                    when (event.key) {
                        Key.DirectionDown -> navigatePage(+1)
                        Key.DirectionUp -> navigatePage(-1)
                        else -> false
                    }
                },
        ) {
            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                },
                label = "welcomePage",
            ) { page ->
                when (page) {
                    0 -> WelcomeIntroPage(onGetStarted = onGetStarted, buttonFocus = buttonFocus)
                    1 -> WelcomeQualityPage(onGetStarted = onGetStarted, buttonFocus = buttonFocus)
                    2 -> WelcomeLibraryPage(onGetStarted = onGetStarted, buttonFocus = buttonFocus)
                    else -> WelcomeDiscoverPage(onGetStarted = onGetStarted, buttonFocus = buttonFocus)
                }
            }

            WelcomeBrandLogo(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 72.dp, top = 48.dp),
            )

            WelcomePageIndicator(
                currentPage = currentPage,
                pageCount = WelcomePageCount,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp),
            )

            WelcomeScrollHint(
                currentPage = currentPage,
                pageCount = WelcomePageCount,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
            )

            if (currentPage > 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = CinemaColors.White,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = stringResource(R.string.welcome_go_up),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = CinemaColors.White,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeBrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.nav_sidebar_full),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier
            .width(WelcomeBrandLogoWidth)
            .height(WelcomeBrandLogoHeight),
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
    )
}

@Composable
private fun WelcomePageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (selected) 32.dp else 5.dp)
                    .clip(if (selected) CinemaShapes.Pill else CircleShape)
                    .background(
                        if (selected) {
                            CinemaColors.White
                        } else {
                            CinemaColors.White.copy(alpha = 0.35f)
                        },
                    ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeScrollHint(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage >= pageCount - 1) return

    val label = when (currentPage) {
        0 -> stringResource(R.string.welcome_explore)
        else -> stringResource(R.string.welcome_more)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Bold,
            ),
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = CinemaColors.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomePageShell(
    onGetStarted: () -> Unit,
    buttonFocus: FocusRequester,
    eyebrow: String? = null,
    title: String,
    background: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        background()
        WelcomeScrim(intense = eyebrow != null)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 168.dp, top = 120.dp, end = 48.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = title.uppercase(),
                modifier = Modifier.width(760.dp),
                style = MaterialTheme.typography.displayMedium.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
                maxLines = 3,
            )
            Spacer(Modifier.height(32.dp))
            CinemaButton(
                text = stringResource(R.string.welcome_get_started),
                variant = CinemaButtonVariant.PrimaryAccent,
                onClick = onGetStarted,
                modifier = Modifier.focusRequester(buttonFocus),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeIntroPage(
    onGetStarted: () -> Unit,
    buttonFocus: FocusRequester,
) {
    WelcomePageShell(
        onGetStarted = onGetStarted,
        buttonFocus = buttonFocus,
        title = stringResource(R.string.welcome_title),
        background = {
            PosterWall(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 170.dp)
                    .graphicsLayer { rotationZ = -8f },
            )
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeQualityPage(
    onGetStarted: () -> Unit,
    buttonFocus: FocusRequester,
) {
    WelcomePageShell(
        onGetStarted = onGetStarted,
        buttonFocus = buttonFocus,
        eyebrow = stringResource(R.string.welcome_eyebrow),
        title = stringResource(R.string.welcome_title_alt),
        background = {
            Box(Modifier.fillMaxSize()) {
                ShowcasePosters(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 72.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    CinemaColors.Accent.copy(alpha = 0.18f),
                                    CinemaColors.AccentDeep.copy(alpha = 0.42f),
                                ),
                            ),
                        ),
                )
            }
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeLibraryPage(
    onGetStarted: () -> Unit,
    buttonFocus: FocusRequester,
) {
    WelcomePageShell(
        onGetStarted = onGetStarted,
        buttonFocus = buttonFocus,
        eyebrow = stringResource(R.string.welcome_page3_eyebrow),
        title = stringResource(R.string.welcome_page3_title),
        background = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                WelcomeBenefitsPanel(
                    modifier = Modifier
                        .padding(end = 96.dp)
                        .width(520.dp),
                )
            }
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeDiscoverPage(
    onGetStarted: () -> Unit,
    buttonFocus: FocusRequester,
) {
    WelcomePageShell(
        onGetStarted = onGetStarted,
        buttonFocus = buttonFocus,
        eyebrow = stringResource(R.string.welcome_page4_eyebrow),
        title = stringResource(R.string.welcome_page4_title),
        background = {
            LivePreviewWall(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 64.dp),
            )
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeBenefitsPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        WelcomeBenefitRow(
            icon = Icons.Default.Refresh,
            title = stringResource(R.string.benefit_sync_watchlist),
            description = stringResource(R.string.benefit_sync_watchlist_desc),
        )
        WelcomeBenefitRow(
            icon = Icons.Default.PlayArrow,
            title = stringResource(R.string.benefit_resume),
            description = stringResource(R.string.benefit_resume_desc),
        )
        WelcomeBenefitRow(
            icon = Icons.Default.Star,
            title = stringResource(R.string.benefit_recommendations),
            description = stringResource(R.string.benefit_recommendations_desc),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WelcomeBenefitRow(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CinemaShapes.Large)
                .background(CinemaColors.Surface.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CinemaColors.Accent,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            )
        }
    }
}

@Composable
private fun WelcomeScrim(intense: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CinemaColors.Background,
                        CinemaColors.Background.copy(alpha = if (intense) 0.96f else 0.92f),
                        CinemaColors.Background.copy(alpha = if (intense) 0.72f else 0.52f),
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
                        CinemaColors.Background.copy(alpha = 0.22f),
                        Color.Transparent,
                        CinemaColors.Background.copy(alpha = 0.45f),
                    ),
                ),
            ),
    )
}

@Composable
private fun BoxScope.PosterWall(
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

@Composable
private fun BoxScope.ShowcasePosters(
    modifier: Modifier = Modifier,
) {
    val posters = remember {
        FakeDataProvider.movies.mapNotNull { it.imageUrl }.take(3)
    }
    if (posters.size < 3) return

    Box(modifier = modifier.size(width = 520.dp, height = 520.dp)) {
        PosterTile(
            imageUrl = posters[0],
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 250.dp, height = 360.dp),
        )
        PosterTile(
            imageUrl = posters[1],
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = 40.dp)
                .size(width = 220.dp, height = 320.dp),
        )
        PosterTile(
            imageUrl = posters[2],
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-24).dp)
                .size(width = 240.dp, height = 340.dp),
        )
    }
}

@Composable
private fun BoxScope.LivePreviewWall(modifier: Modifier = Modifier) {
    val channels = remember { FakeDataProvider.channels.take(6) }
    Column(
        modifier = modifier.width(560.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        channels.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { channel ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(CinemaShapes.Medium)
                            .background(CinemaColors.Surface),
                    ) {
                        CinemaAsyncImage(
                            imageUrl = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fallbackLabel = channel.name,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterTile(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
    }
}
