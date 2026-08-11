package com.iptvcinema.tv.core.epg

import java.util.Collections
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LatestPerSourceJobRunnerTest {
    @Test
    fun replacementWaitsForCancelledJobCleanupBeforeStarting() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val runner = LatestPerSourceJobRunner(scope, Dispatchers.Default)
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelling = CompletableDeferred<Unit>()
        val releaseCleanup = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val events = Collections.synchronizedList(mutableListOf<String>())

        runner.launch("source-1") {
            firstStarted.complete(Unit)
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                firstCancelling.complete(Unit)
                withContext(NonCancellable) { releaseCleanup.await() }
                events += "first-finished"
            }
        }
        firstStarted.await()

        val replacement = runner.launch("source-1") {
            events += "second-started"
            secondStarted.complete(Unit)
        }
        firstCancelling.await()

        assertNull(withTimeoutOrNull(100) { secondStarted.await() })
        releaseCleanup.complete(Unit)
        withTimeout(2_000) { replacement.join() }

        assertEquals(listOf("first-finished", "second-started"), events)
        scope.cancel()
    }

    @Test
    fun cancelAndJoinWaitsForInProgressCleanup() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val runner = LatestPerSourceJobRunner(scope, Dispatchers.Default)
        val started = CompletableDeferred<Unit>()
        val cancelling = CompletableDeferred<Unit>()
        val releaseCleanup = CompletableDeferred<Unit>()

        runner.launch("source-1") {
            started.complete(Unit)
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                cancelling.complete(Unit)
                withContext(NonCancellable) { releaseCleanup.await() }
            }
        }
        started.await()

        val cancellation = async { runner.cancelAndJoin("source-1") }
        cancelling.await()
        assertFalse(cancellation.isCompleted)
        releaseCleanup.complete(Unit)
        withTimeout(2_000) { cancellation.await() }

        scope.cancel()
    }

    @Test
    fun awaitIdleFollowsReplacementUntilSourceHasNoPendingJob() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val runner = LatestPerSourceJobRunner(scope, Dispatchers.Default)
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val releaseSecond = CompletableDeferred<Unit>()

        runner.launch("source-1") {
            firstStarted.complete(Unit)
            withContext(NonCancellable) { releaseFirst.await() }
        }
        firstStarted.await()
        runner.launch("source-1") {
            secondStarted.complete(Unit)
            releaseSecond.await()
        }

        val waiting = async { runner.awaitIdle("source-1") }
        assertFalse(waiting.isCompleted)
        releaseFirst.complete(Unit)
        secondStarted.await()
        assertFalse(waiting.isCompleted)
        releaseSecond.complete(Unit)
        withTimeout(2_000) { waiting.await() }

        scope.cancel()
    }
}
