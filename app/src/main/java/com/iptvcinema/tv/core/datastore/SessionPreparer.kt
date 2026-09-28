package com.iptvcinema.tv.core.datastore

/** Prepares the local session after sign-in and reports where the user should land. */
interface SessionPreparer {
    suspend fun prepareSessionState(): AppSessionState
    suspend fun authenticateLocalDev(): AppSessionState
}
