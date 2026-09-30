package com.iptvcinema.tv.features.browse

import androidx.navigation.NavController
import com.iptvcinema.tv.core.model.home.BrowseCardTypes
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.navigation.AppRoute
import com.iptvcinema.tv.core.navigation.navigateToLiveChannel
import com.iptvcinema.tv.features.home.HomeSection

/**
 * What a click on a browse card does, the same on every screen: Continue Watching and Next
 * episode play, channels open Live TV on that channel, category tiles open the catalog grid, and
 * titles open their details.
 */
fun openBrowseCard(navController: NavController, section: HomeSection, card: HomeContentCard) {
    when (section) {
        is HomeSection.ContinueWatching, is HomeSection.NextEpisode -> playBrowseCard(navController, card)
        else -> openBrowseCardDetails(navController, card)
    }
}

/** Plays a movie or episode card; anything else opens its details. */
fun playBrowseCard(navController: NavController, card: HomeContentCard) {
    when (card.contentType) {
        "movie" -> navController.navigate(AppRoute.player(card.contentId, "movie"))
        "episode" -> navController.navigate(AppRoute.player(card.contentId, "episode", card.seriesId))
        else -> openBrowseCardDetails(navController, card)
    }
}

/** Opens the page for a card: details for titles, the series for an episode, Live TV for a channel. */
fun openBrowseCardDetails(navController: NavController, card: HomeContentCard) {
    when (card.contentType) {
        "series" -> navController.navigate(AppRoute.seriesDetails(card.contentId))
        "episode" -> card.seriesId?.takeIf { it.isNotBlank() }
            ?.let { navController.navigate(AppRoute.seriesDetails(it)) }
            ?: navController.navigate(AppRoute.player(card.contentId, "episode"))
        "channel" -> navController.navigateToLiveChannel(card.contentId)
        BrowseCardTypes.MOVIE_CATEGORY -> navController.navigate(AppRoute.movieCatalog(card.contentId))
        BrowseCardTypes.SERIES_CATEGORY -> navController.navigate(AppRoute.seriesCatalog(card.contentId))
        else -> navController.navigate(AppRoute.movieDetails(card.contentId))
    }
}
