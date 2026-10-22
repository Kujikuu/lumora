package com.iptvcinema.tv.app

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Completes once the first activity frame has been drawn, so background start-up work can wait
 * until the first screen is up instead of fighting it for CPU and disk on a weak TV.
 */
@Singleton
class FirstFrameSignal @Inject constructor() {
    private val drawn = CompletableDeferred<Unit>()

    fun markDrawn() {
        drawn.complete(Unit)
    }

    /**
     * Waits for the first frame; false after [timeoutMs] when no activity started (a job or the
     * launcher's content provider woke the process).
     */
    suspend fun await(timeoutMs: Long): Boolean =
        withTimeoutOrNull(timeoutMs) { drawn.await() } != null
}
