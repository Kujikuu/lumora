package com.iptvcinema.tv.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.datastore.SessionRequirement
import com.iptvcinema.tv.core.tvhome.PlaybackDeepLink

/**
 * Plays a link from the TV home screen once the app is ready for it. On a cold start the link
 * waits out the splash screen; if the viewer still has to sign in or pick a profile, it is dropped.
 */
@Composable
fun PendingDeepLinkHandler(
    navController: NavController,
    sessionViewModel: SessionViewModel,
) {
    val link by sessionViewModel.pendingDeepLink.collectAsState()
    val session by sessionViewModel.sessionState.collectAsState()
    val isHydrated by sessionViewModel.isHydrated.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    LaunchedEffect(link, session, isHydrated, currentRoute) {
        val pending = link ?: return@LaunchedEffect
        if (!isHydrated || currentRoute == null || currentRoute == AppRoute.SPLASH) return@LaunchedEffect
        sessionViewModel.consumeDeepLink(pending)
        deepLinkRoute(pending, session)?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
        }
    }
}

/** Where a link leads for this session, or null when the app is not ready to play anything. */
internal fun deepLinkRoute(link: PlaybackDeepLink, session: AppSessionState): String? {
    if (!session.meetsRequirement(SessionRequirement.Ready)) return null
    val sameProfile = link.profileId == null || link.profileId == session.currentProfileId
    return when (link.contentType) {
        // A series card always opens the show's page; the viewer picks the episode there.
        "series" -> AppRoute.seriesDetails(link.contentId)
        "live" -> if (sameProfile) AppRoute.player(link.contentId, "live") else AppRoute.channelDetails(link.contentId)
        // Another profile's card: open the title instead of playing on this profile's history.
        "episode" -> if (sameProfile) {
            AppRoute.player(link.contentId, link.contentType, seriesId = link.seriesId)
        } else {
            link.seriesId?.let(AppRoute::seriesDetails)
        }
        "movie" -> if (sameProfile) {
            AppRoute.player(link.contentId, link.contentType, seriesId = link.seriesId)
        } else {
            AppRoute.movieDetails(link.contentId)
        }
        else -> null
    }
}
