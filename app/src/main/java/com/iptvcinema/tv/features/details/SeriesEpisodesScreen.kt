package com.iptvcinema.tv.features.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.SkeletonHomeContent
import com.iptvcinema.tv.core.design.components.shellHeroContentStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.util.isolateDirection
import com.iptvcinema.tv.features.browse.ImmersiveBrowse
import com.iptvcinema.tv.features.browse.rememberImmersiveBrowseState

/**
 * Every episode of a series in the Home layout: one rail per season (Up and Down move between
 * seasons), progress bars on started and watched episodes, and the spotlight showing the focused
 * episode over the series backdrop. Focus opens on the episode to resume or play next.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesEpisodesScreen(
    seriesId: String,
    navController: NavController,
    viewModel: SeriesEpisodesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusState = rememberScreenFocusState("series_episodes")
    val browse = rememberImmersiveBrowseState(focusState)
    val blockedMessage = stringResource(R.string.feedback_rating_blocked)

    LaunchedEffect(seriesId) { viewModel.load(seriesId) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshWatchState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PopBackHandler(onBack = { navController.popBackStack() })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
    ) {
        when (uiState.loadState) {
            DetailsLoadState.Loading -> SkeletonHomeContent(modifier = Modifier.padding(top = CinemaSpacing.ScreenPaddingVertical))
            DetailsLoadState.Error -> EmptyState(
                title = stringResource(R.string.error_series_unavailable),
                description = uiState.message ?: stringResource(R.string.error_series_not_found),
                primaryAction = stringResource(R.string.btn_back),
                secondaryAction = null,
                onPrimary = { navController.popBackStack() },
                onSecondary = null,
                modifier = Modifier.align(Alignment.Center),
            )
            DetailsLoadState.Ready -> ImmersiveBrowse(
                state = browse,
                sections = uiState.sections,
                isReady = true,
                initialFocus = uiState.resumeFocus,
                onCardClick = { _, card ->
                    if (uiState.playbackBlocked) {
                        Toast.makeText(context, blockedMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        navController.navigate(AppRoute.player(card.contentId, "episode", seriesId))
                    }
                },
                overlay = {
                    Text(
                        text = uiState.seriesTitle.isolateDirection(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = shellHeroContentStart(),
                                top = CinemaSpacing.ScreenPaddingVertical,
                                end = CinemaSpacing.ScreenPadding,
                            ),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CinemaColors.TextSecondary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
