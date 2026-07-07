package com.iptvcinema.tv.core.datastore

import com.iptvcinema.tv.core.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSessionStateTest {

    @Test
    fun resolveStartupDestination_unauthenticated_returnsWelcome() {
        val state = AppSessionState(isAuthenticated = false)

        assertEquals(StartupDestination.Welcome, state.resolveStartupDestination())
    }

    @Test
    fun resolveStartupDestination_noSource_returnsAddSource() {
        val state = AppSessionState(isAuthenticated = true, hasSource = false)

        assertEquals(StartupDestination.AddSource, state.resolveStartupDestination())
    }

    @Test
    fun resolveStartupDestination_noProfile_returnsProfileSelection() {
        val state = AppSessionState(
            isAuthenticated = true,
            hasSource = true,
            currentProfileId = null,
        )

        assertEquals(StartupDestination.ProfileSelection, state.resolveStartupDestination())
    }

    @Test
    fun resolveStartupDestination_ready_returnsHome() {
        val state = AppSessionState(
            isAuthenticated = true,
            hasSource = true,
            currentProfileId = "profile-1",
        )

        assertEquals(StartupDestination.Home, state.resolveStartupDestination())
    }

    @Test
    fun meetsRequirement_ready_requiresAuthSourceAndProfile() {
        val ready = AppSessionState(
            isAuthenticated = true,
            hasSource = true,
            currentProfileId = "profile-1",
        )
        val missingProfile = ready.copy(currentProfileId = null)

        assertTrue(ready.meetsRequirement(SessionRequirement.Ready))
        assertFalse(missingProfile.meetsRequirement(SessionRequirement.Ready))
    }

    @Test
    fun redirectRouteFor_returnsNullWhenRequirementMet() {
        val state = AppSessionState(
            isAuthenticated = true,
            hasSource = true,
            currentProfileId = "profile-1",
        )

        assertNull(state.redirectRouteFor(SessionRequirement.Ready))
    }

    @Test
    fun redirectRouteFor_returnsWelcomeWhenUnauthenticated() {
        val state = AppSessionState(isAuthenticated = false)

        assertEquals(
            StartupDestination.Welcome.route(),
            state.redirectRouteFor(SessionRequirement.Ready),
        )
    }

    @Test
    fun resolveStartupDestination_withUserIdStillRequiresProfileBeforeHome() {
        val state = AppSessionState(
            isAuthenticated = true,
            userId = "supabase-user-id",
            hasSource = true,
            currentProfileId = null,
        )

        assertEquals(StartupDestination.ProfileSelection, state.resolveStartupDestination())
    }

    @Test
    fun connectedSourceLabel_returnsExpectedLabels() {
        assertNull(AppSessionState().connectedSourceLabel())
        assertEquals("Demo Mode", AppSessionState(hasSource = true, isDemoMode = true).connectedSourceLabel())
        assertEquals(
            "Xtream Codes",
            AppSessionState(hasSource = true, sourceType = SourceType.XTREAM_CODES).connectedSourceLabel(),
        )
        assertEquals(
            "M3U Playlist",
            AppSessionState(hasSource = true, sourceType = SourceType.M3U).connectedSourceLabel(),
        )
    }
}
