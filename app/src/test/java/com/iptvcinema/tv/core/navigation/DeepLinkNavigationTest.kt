package com.iptvcinema.tv.core.navigation

import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.tvhome.PlaybackDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkNavigationTest {
    private val ready = AppSessionState(isAuthenticated = true, hasSource = true, currentProfileId = "p1")

    @Test
    fun sameProfile_playsTheItem() {
        val link = PlaybackDeepLink(contentType = "episode", contentId = "e1", seriesId = "s1", profileId = "p1")

        assertEquals("player/e1/episode?seriesId=s1", deepLinkRoute(link, ready))
    }

    @Test
    fun otherProfile_opensDetailsInstead() {
        assertEquals(
            AppRoute.seriesDetails("s1"),
            deepLinkRoute(PlaybackDeepLink("episode", "e1", seriesId = "s1", profileId = "p2"), ready),
        )
        assertEquals(
            AppRoute.movieDetails("m1"),
            deepLinkRoute(PlaybackDeepLink("movie", "m1", profileId = "p2"), ready),
        )
    }

    @Test
    fun notReady_dropsTheLink() {
        val link = PlaybackDeepLink(contentType = "movie", contentId = "m1")

        assertNull(deepLinkRoute(link, ready.copy(currentProfileId = null)))
        assertNull(deepLinkRoute(link, AppSessionState()))
    }
}
