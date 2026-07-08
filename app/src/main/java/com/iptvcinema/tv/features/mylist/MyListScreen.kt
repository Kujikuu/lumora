package com.iptvcinema.tv.features.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.ChannelTile
import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.design.components.SkeletonPosterGrid
import com.iptvcinema.tv.core.design.components.animateToFocusedItem
import com.iptvcinema.tv.core.design.components.shellContentStart
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyListScreen(
    navController: NavController,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val watchLaterFocus = remember { FocusRequester() }
    val channelsFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("my_list")
    val scope = rememberCoroutineScope()
    val contentStart = shellContentStart()

    val exitRailToContent: () -> Unit = {
        scope.launch {
            val state = uiState
            val requester = if (state is MyListUiState.Ready && state.watchLater.isNotEmpty()) {
                watchLaterFocus
            } else {
                channelsFocus
            }
            runCatching { requester.requestFocus() }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMyList()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MainShellBackHandler(navController = navController, isHomeTab = false)

    MainShellScaffold(
        navController = navController,
        selectedNavItem = NavItem.MyList,
        onRailExitRight = exitRailToContent,
    ) {
        when (val state = uiState) {
            MyListUiState.Loading -> {
                Column(
                    modifier = Modifier.padding(top = CinemaSpacing.ScreenPaddingVertical),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    CinemaSerifTitle(text = stringResource(R.string.mylist_title))
                    SkeletonPosterGrid(columns = 4, rows = 2)
                }
            }
            is MyListUiState.Error -> {
                ErrorState(
                    title = stringResource(R.string.error_load_list),
                    description = state.message,
                    errorCode = "MY_LIST_ERROR",
                    onRetry = viewModel::loadMyList,
                    onSwitchStream = { navController.navigate(AppRoute.PLAYLIST_MANAGEMENT) },
                    onBack = { navController.popBackStack() },
                    showSwitchStream = false,
                    backLabel = stringResource(R.string.btn_back),
                )
            }
            is MyListUiState.Ready -> {
                LaunchedEffect(state.hasContent, focusState.initialFocusHandled) {
                    if (state.hasContent && !focusState.initialFocusHandled) {
                        val requester = if (state.watchLater.isNotEmpty()) watchLaterFocus else channelsFocus
                        focusState.requestInitialFocus(requester)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = CinemaSpacing.ScreenPaddingVertical),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    CinemaSerifTitle(text = stringResource(R.string.mylist_title))
                    if (!state.hasContent) {
                        EmptyState(
                            title = stringResource(R.string.mylist_nothing_saved),
                            description = stringResource(R.string.mylist_nothing_saved_desc),
                            primaryAction = stringResource(R.string.nav_movies),
                            secondaryAction = stringResource(R.string.nav_live_tv),
                            onPrimary = { navController.navigate(AppRoute.MOVIES) },
                            onSecondary = { navController.navigate(AppRoute.LIVE_TV) },
                        )
                    } else {
                        if (state.watchLater.isNotEmpty()) {
                            WatchLaterRail(
                                favorites = state.watchLater,
                                contentStart = contentStart,
                                firstItemFocusRequester = watchLaterFocus,
                                onOpenFavorite = { openFavorite(navController, it) },
                                onRemoveFavorite = viewModel::removeFavorite,
                            )
                        }
                        if (state.favoriteChannels.isNotEmpty()) {
                            FavoriteChannelsRail(
                                channels = state.favoriteChannels,
                                contentStart = contentStart,
                                firstItemFocusRequester = if (state.watchLater.isEmpty()) channelsFocus else null,
                                onChannelClick = { channel ->
                                    navController.navigate(AppRoute.liveTv(channel.contentId))
                                },
                                onRemoveChannel = { channel ->
                                    viewModel.removeFavorite(channel.favorite)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WatchLaterRail(
    favorites: List<FavoriteItem>,
    contentStart: androidx.compose.ui.unit.Dp,
    firstItemFocusRequester: FocusRequester,
    onOpenFavorite: (FavoriteItem) -> Unit,
    onRemoveFavorite: (FavoriteItem) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.CardGap)) {
        Text(
            text = stringResource(R.string.btn_watch_later),
            style = MaterialTheme.typography.labelLarge.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(start = contentStart),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                start = contentStart,
                end = CinemaSpacing.ScreenPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                AllAddedCard(
                    modifier = Modifier
                        .focusRequester(firstItemFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    listState.animateToFocusedItem(0)
                                }
                            }
                        },
                )
            }
            itemsIndexed(favorites, key = { _, favorite -> favorite.id }) { index, favorite ->
                MyCollectionTile(
                    data = favorite.toPosterCardData(),
                    onClick = { onOpenFavorite(favorite) },
                    onLongClick = { onRemoveFavorite(favorite) },
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                listState.animateToFocusedItem(index + 1)
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FavoriteChannelsRail(
    channels: List<FavoriteChannelCard>,
    contentStart: androidx.compose.ui.unit.Dp,
    firstItemFocusRequester: FocusRequester?,
    onChannelClick: (FavoriteChannelCard) -> Unit,
    onRemoveChannel: (FavoriteChannelCard) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Column(verticalArrangement = Arrangement.spacedBy(CinemaSpacing.CardGap)) {
        Text(
            text = stringResource(R.string.mylist_favorite_channels),
            style = MaterialTheme.typography.labelLarge.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(start = contentStart),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                start = contentStart,
                end = CinemaSpacing.ScreenPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                ChannelTile(
                    data = channel.toChannelTileData(),
                    onClick = { onChannelClick(channel) },
                    onLongClick = { onRemoveChannel(channel) },
                    modifier = Modifier
                        .then(
                            if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    listState.animateToFocusedItem(index)
                                }
                            }
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AllAddedCard(modifier: Modifier = Modifier) {
    FocusableCinemaCard(
        modifier = modifier
            .width(270.dp)
            .height(153.dp),
        onClick = {},
        shape = CinemaShapes.Card,
        focusScale = 1.02f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Bookmarks,
                contentDescription = null,
                tint = CinemaColors.Background,
                modifier = Modifier.width(29.dp).height(29.dp),
            )
            Text(
                text = stringResource(R.string.mylist_all_added),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = CinemaColors.Background,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MyCollectionTile(
    data: PosterCardData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier
            .width(245.dp)
            .height(141.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CinemaShapes.Card,
        focusScale = 1.02f,
        contentDescription = data.title,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CinemaShapes.Card)
                .background(CinemaColors.Surface),
        ) {
            CinemaAsyncImage(
                imageUrl = data.imageUrl,
                contentDescription = data.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallbackLabel = data.title,
            )
            Text(
                text = data.title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(9.dp),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun FavoriteChannelCard.toChannelTileData(): ChannelTileData = ChannelTileData(
    id = contentId,
    channelName = name,
    logoUrl = logoUrl,
    currentProgram = currentProgram,
    isLive = true,
)

private fun openFavorite(navController: NavController, favorite: FavoriteItem) {
    when (favorite.contentType) {
        FavoriteContentType.MOVIE -> navController.navigate(AppRoute.movieDetails(favorite.contentId))
        FavoriteContentType.SERIES, FavoriteContentType.EPISODE ->
            navController.navigate(AppRoute.seriesDetails(favorite.contentId))
        FavoriteContentType.CHANNEL -> navController.navigate(AppRoute.liveTv(favorite.contentId))
    }
}
