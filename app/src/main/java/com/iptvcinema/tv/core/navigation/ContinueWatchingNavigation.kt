package com.iptvcinema.tv.core.navigation

import androidx.navigation.NavController
import com.iptvcinema.tv.core.model.home.HomeContentCard

fun openContinueWatchingDetails(navController: NavController, card: HomeContentCard) {
    when (card.contentType) {
        "episode" -> card.seriesId?.let { seriesId ->
            navController.navigate(AppRoute.seriesDetails(seriesId))
        }
        "movie" -> navController.navigate(AppRoute.movieDetails(card.contentId))
    }
}
