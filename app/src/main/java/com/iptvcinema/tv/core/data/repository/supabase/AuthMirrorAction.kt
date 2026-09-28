package com.iptvcinema.tv.core.data.repository.supabase

import io.github.jan.supabase.auth.status.SessionStatus

/**
 * What the local DataStore session should do for a given Supabase [SessionStatus].
 *
 * A failed token refresh (usually no network) is not a sign-out: supabase-kt keeps the
 * stored session and retries, so the local session must stay as it is.
 */
sealed interface AuthMirrorAction {
    data class SignedIn(val userId: String?) : AuthMirrorAction
    data object SignedOut : AuthMirrorAction
    data object NoChange : AuthMirrorAction
}

fun SessionStatus.toMirrorAction(): AuthMirrorAction = when (this) {
    is SessionStatus.Authenticated -> AuthMirrorAction.SignedIn(session.user?.id)
    is SessionStatus.NotAuthenticated -> AuthMirrorAction.SignedOut
    is SessionStatus.RefreshFailure -> AuthMirrorAction.NoChange
    SessionStatus.Initializing -> AuthMirrorAction.NoChange
}

fun SessionStatus.hasUsableSession(): Boolean =
    this is SessionStatus.Authenticated || this is SessionStatus.RefreshFailure
