package com.iptvcinema.tv.features.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.ChannelProgramPanel
import com.iptvcinema.tv.core.design.components.ChannelTile
import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.ContentRail
import com.iptvcinema.tv.core.design.components.DetailHero
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ProgramLineupCard
import com.iptvcinema.tv.core.design.components.SkeletonDetailHero
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.navigateToLiveChannel
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.core.util.rememberPrototypeFeedback

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelDetailsScreen(
    channelId: String,
    navController: NavController,
    viewModel: ChannelDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchLiveFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("channel_details")
    val showFeedback = rememberPrototypeFeedback()
    val feedbackAddedToMyList = stringResource(R.string.feedback_added_to_mylist)
    val feedbackRemovedFromMyList = stringResource(R.string.feedback_removed_from_mylist)
    val watchLiveLabel = stringResource(R.string.btn_watch_live)
    val openInLiveTvLabel = stringResource(R.string.btn_open_in_live_tv)

    LaunchedEffect(channelId) {
        viewModel.loadChannelDetails(channelId)
    }

    PopBackHandler(onBack = { navController.popBackStack() })

    LaunchedEffect(focusState.hasSavedFocus, uiState.loadState) {
        if (uiState.loadState == DetailsLoadState.Ready && focusState.hasSavedFocus) {
            focusState.restoreFocus(watchLiveFocus)
        } else if (uiState.loadState == DetailsLoadState.Ready) {
            focusState.requestInitialFocus(watchLiveFocus)
            focusState.saveFocusIndex(0)
        }
    }

    when (uiState.loadState) {
        DetailsLoadState.Loading -> {
            MainShellScaffold(navController = navController, selectedNavItem = NavItem.LiveTv) {
                SkeletonDetailHero()
            }
        }
        DetailsLoadState.Error -> {
            MainShellScaffold(navController = navController, selectedNavItem = NavItem.LiveTv) {
                EmptyState(
                    title = stringResource(R.string.msg_no_live_channels),
                    description = uiState.message ?: stringResource(R.string.msg_no_live_channels_desc),
                    primaryAction = stringResource(R.string.btn_back),
                    secondaryAction = null,
                    onPrimary = { navController.popBackStack() },
                    onSecondary = null,
                )
            }
        }
        DetailsLoadState.Ready -> {
            MainShellScaffold(
                navController = navController,
                selectedNavItem = NavItem.LiveTv,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    DetailHero(
                        title = uiState.channelName,
                        metadata = listOfNotNull(
                            uiState.category.takeIf { it.isNotBlank() },
                            stringResource(R.string.badge_live),
                        ),
                        synopsis = uiState.currentProgram?.description?.takeIf { it.isNotBlank() }
                            ?: uiState.currentProgram?.title.orEmpty(),
                        primaryActionLabel = watchLiveLabel,
                        watchLaterLabel = stringResource(R.string.btn_add_to_favorites),
                        onWatchNow = {
                            navController.navigate(AppRoute.player(uiState.channelId, "live"))
                        },
                        onFavorite = {
                            viewModel.toggleFavorite { added ->
                                showFeedback(
                                    if (added) feedbackAddedToMyList else feedbackRemovedFromMyList,
                                )
                            }
                        },
                        isFavorite = uiState.isFavorite,
                        backdropUrl = uiState.logoUrl,
                        watchNowFocusRequester = watchLiveFocus,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = CinemaSpacing.NavRailWidth + 16.dp),
                    ) {
                        CinemaButton(
                            text = openInLiveTvLabel,
                            variant = CinemaButtonVariant.Ghost,
                            onClick = {
                                navController.navigateToLiveChannel(uiState.channelId)
                            },
                        )
                    }
                    ChannelProgramPanel(
                        currentProgram = uiState.currentProgram,
                        nextPrograms = uiState.nextPrograms,
                        nowMs = uiState.nowMs,
                    )
                    if (uiState.todayPrograms.isNotEmpty()) {
                        ContentRail(
                            title = stringResource(R.string.channel_today_lineup),
                            items = uiState.todayPrograms,
                        ) { program ->
                            ProgramLineupCard(
                                program = program,
                                isNowPlaying = program.id == uiState.currentProgram?.id,
                                nowMs = uiState.nowMs,
                                onClick = {},
                            )
                        }
                    }
                    if (uiState.relatedChannels.isNotEmpty()) {
                        ContentRail(
                            title = stringResource(R.string.channel_related),
                            items = uiState.relatedChannels,
                        ) { channel ->
                            ChannelTile(
                                data = channel.toChannelTileData(),
                                onClick = {
                                    navController.navigate(AppRoute.channelDetails(channel.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ChannelItem.toChannelTileData(): ChannelTileData = ChannelTileData(
    id = id,
    channelName = name,
    logoUrl = logoUrl,
    currentProgram = currentProgram,
    qualityBadge = qualityBadge,
    programProgress = programProgress,
)
