package com.iptvcinema.tv.features.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.util.isolateDirection
import com.iptvcinema.tv.features.browse.ImmersiveBrowse
import com.iptvcinema.tv.features.browse.openBrowseCardDetails
import com.iptvcinema.tv.features.browse.rememberImmersiveBrowseState

/**
 * "More like this" for a movie or series in the Home layout: similar titles, then top rated and
 * new ones, with the spotlight following focus. Long press adds or removes from My List.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RelatedScreen(
    kind: RelatedKind,
    contentId: String,
    navController: NavController,
    viewModel: RelatedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusState = rememberScreenFocusState("related")
    val browse = rememberImmersiveBrowseState(focusState)

    LaunchedEffect(kind, contentId) { viewModel.load(kind, contentId) }

    PopBackHandler(onBack = { navController.popBackStack() })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background),
    ) {
        when (uiState.loadState) {
            DetailsLoadState.Loading -> SkeletonHomeContent(modifier = Modifier.padding(top = CinemaSpacing.ScreenPaddingVertical))
            DetailsLoadState.Error -> EmptyState(
                title = stringResource(R.string.related_more_like_this),
                description = uiState.message ?: stringResource(R.string.related_empty),
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
                onCardClick = { _, card -> openBrowseCardDetails(navController, card) },
                onCardLongClick = { _, card -> viewModel.toggleFavorite(card) },
                overlay = {
                    Text(
                        text = stringResource(R.string.related_title, uiState.anchorTitle.isolateDirection()),
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
