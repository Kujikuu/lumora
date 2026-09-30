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
    if (sameProfile) {
        return AppRoute.player(link.contentId, link.contentType, seriesId = link.seriesId)
    }
    // Another profile's card: open the title instead of playing on this profile's history.
    return when (link.contentType) {
        "episode" -> link.seriesId?.let(AppRoute::seriesDetails)
        "movie" -> AppRoute.movieDetails(link.contentId)
        else -> null
    }
}
