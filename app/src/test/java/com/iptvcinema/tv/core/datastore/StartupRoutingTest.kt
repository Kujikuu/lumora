package com.iptvcinema.tv.core.datastore

import com.iptvcinema.tv.core.model.SourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupRoutingTest {
    private val ready = AppSessionState(
        isAuthenticated = true,
        userId = "user-1",
        hasSource = true,
        currentProfileId = "profile-1",
        currentSourceId = "source-1",
        sourceType = SourceType.XTREAM_CODES,
    )

    @Test
    fun savedAccountAndSource_routesFromLocalSession() {
        assertTrue(canRouteFromLocalSession(ready))
    }

    @Test
    fun savedAccountAndSourceWithoutProfile_stillRoutesLocally() {
        // Profile selection loads its own data; the source check can run behind it.
        assertTrue(canRouteFromLocalSession(ready.copy(currentProfileId = null)))
    }

    @Test
    fun firstLaunch_waitsForTheNetwork() {
        assertFalse(canRouteFromLocalSession(AppSessionState()))
    }

    @Test
    fun signedOut_waitsForTheNetwork() {
        // Supabase may still hold a session the DataStore lost; only the full check finds it.
        assertFalse(canRouteFromLocalSession(ready.copy(isAuthenticated = false, userId = null)))
    }

    @Test
    fun noSource_waitsForTheNetwork() {
        // The active source is restored from the account, which needs the network.
        assertFalse(canRouteFromLocalSession(ready.copy(hasSource = false, currentSourceId = null)))
        assertFalse(canRouteFromLocalSession(ready.copy(currentSourceId = null)))
    }

    @Test
    fun demoSource_routesFromLocalSession() {
        val demo = ready.copy(currentSourceId = "demo", sourceType = SourceType.DEMO, isDemoMode = true)
        assertTrue(canRouteFromLocalSession(demo))
    }
}
