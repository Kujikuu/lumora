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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.CinemaSerifTitle
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ErrorState
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.design.components.SkeletonPosterGrid
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.FavoriteItem
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.MainShellBackHandler
import com.iptvcinema.tv.core.navigation.MainShellScaffold
import com.iptvcinema.tv.core.navigation.NavItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyListScreen(
    navController: NavController,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = CinemaSpacing.ScreenPaddingVertical),
                    verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
                ) {
                    CinemaSerifTitle(text = stringResource(R.string.mylist_title))
                    if (state.favorites.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.mylist_nothing_saved),
                            description = stringResource(R.string.mylist_nothing_saved_desc),
                            primaryAction = stringResource(R.string.nav_movies),
                            secondaryAction = null,
                            onPrimary = { navController.navigate(AppRoute.MOVIES) },
                            onSecondary = null,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.btn_watch_later),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = CinemaColors.White,
                                fontWeight = FontWeight.Black,
                            ),
                            modifier = Modifier.padding(start = CinemaSpacing.ContentStart),
                        )
                        LazyRow(
                            contentPadding = PaddingValues(
                                start = CinemaSpacing.ContentStart,
                                end = CinemaSpacing.ScreenPadding,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            item {
                                AllAddedCard()
                            }
                            items(state.favorites, key = { it.id }) { favorite ->
                                MyCollectionTile(
                                    data = favorite.toPosterCardData(),
                                    onClick = { openFavorite(navController, favorite) },
                                    onLongClick = { viewModel.removeFavorite(favorite) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AllAddedCard() {
    FocusableCinemaCard(
        modifier = Modifier
            .width(270.dp)
            .height(153.dp),
        onClick = {},
        shape = CinemaShapes.Small,
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
                imageVector = Icons.Default.FavoriteBorder,
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
) {
    FocusableCinemaCard(
        modifier = Modifier
            .width(245.dp)
            .height(141.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CinemaShapes.Small,
        focusScale = 1.02f,
        contentDescription = data.title,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CinemaShapes.Small)
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

private fun openFavorite(navController: NavController, favorite: FavoriteItem) {
    when (favorite.contentType) {
        FavoriteContentType.MOVIE -> navController.navigate(AppRoute.movieDetails(favorite.contentId))
        FavoriteContentType.SERIES, FavoriteContentType.EPISODE ->
            navController.navigate(AppRoute.seriesDetails(favorite.contentId))
        FavoriteContentType.CHANNEL -> navController.navigate(AppRoute.liveTv(favorite.contentId))
    }
}
