package com.iptvcinema.tv.core.data.repository.supabase

import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionMirrorActionTest {

    @Test
    fun authenticated_mapsToSignedInWithUserId() {
        val status = SessionStatus.Authenticated(session(userId = "user-1"), SessionSource.Storage)

        assertEquals(AuthMirrorAction.SignedIn("user-1"), status.toMirrorAction())
    }

    @Test
    fun authenticatedWithoutUser_mapsToSignedInKeepingStoredId() {
        val status = SessionStatus.Authenticated(session(userId = null), SessionSource.Storage)

        assertEquals(AuthMirrorAction.SignedIn(null), status.toMirrorAction())
    }

    @Test
    fun refreshFailure_keepsLocalSession() {
        val status = SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(RuntimeException("offline")))

        assertEquals(AuthMirrorAction.NoChange, status.toMirrorAction())
    }

    @Test
    fun initializing_keepsLocalSession() {
        assertEquals(AuthMirrorAction.NoChange, SessionStatus.Initializing.toMirrorAction())
    }

    @Test
    fun notAuthenticated_mapsToSignedOut() {
        assertEquals(AuthMirrorAction.SignedOut, SessionStatus.NotAuthenticated(isSignOut = false).toMirrorAction())
        assertEquals(AuthMirrorAction.SignedOut, SessionStatus.NotAuthenticated(isSignOut = true).toMirrorAction())
    }

    @Test
    fun hasUsableSession_trueWhileRefreshIsFailing() {
        val failing = SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(RuntimeException("offline")))

        assertTrue(failing.hasUsableSession())
        assertTrue(SessionStatus.Authenticated(session("u"), SessionSource.Storage).hasUsableSession())
        assertFalse(SessionStatus.NotAuthenticated(isSignOut = false).hasUsableSession())
        assertFalse(SessionStatus.Initializing.hasUsableSession())
    }

    private fun session(userId: String?): UserSession = UserSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 3600,
        tokenType = "bearer",
        user = userId?.let { UserInfo(aud = "authenticated", id = it) },
    )
}
