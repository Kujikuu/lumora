package com.iptvcinema.tv.core.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryPolicyTest {
    @Test
    fun behindLiveWindow_seeksToLiveEdge() {
        val action = PlaybackRecoveryPolicy.forError(
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
            isLive = true,
            attempt = 0,
        )
        assertEquals(PlaybackRecoveryAction.SEEK_TO_LIVE_EDGE, action)
    }

    @Test
    fun networkTimeout_retries_forLiveAndVod() {
        val code = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        assertEquals(PlaybackRecoveryAction.RETRY, PlaybackRecoveryPolicy.forError(code, isLive = true, attempt = 0))
        assertEquals(PlaybackRecoveryAction.RETRY, PlaybackRecoveryPolicy.forError(code, isLive = false, attempt = 2))
    }

    @Test
    fun malformedContainer_retriesOnlyForLive() {
        val code = PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
        assertEquals(PlaybackRecoveryAction.RETRY, PlaybackRecoveryPolicy.forError(code, isLive = true, attempt = 0))
        assertEquals(PlaybackRecoveryAction.FAIL, PlaybackRecoveryPolicy.forError(code, isLive = false, attempt = 0))
    }

    @Test
    fun unsupportedFormat_failsImmediately() {
        val action = PlaybackRecoveryPolicy.forError(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            isLive = true,
            attempt = 0,
        )
        assertEquals(PlaybackRecoveryAction.FAIL, action)
    }

    @Test
    fun exhaustedRetryBudget_fails_evenForRecoverableErrors() {
        val timeout = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        val behind = PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
        assertEquals(PlaybackRecoveryAction.FAIL, PlaybackRecoveryPolicy.forError(timeout, isLive = true, attempt = 3))
        assertEquals(PlaybackRecoveryAction.FAIL, PlaybackRecoveryPolicy.forError(behind, isLive = true, attempt = 3))
    }

    @Test
    fun retryDelay_doublesPerAttempt() {
        assertEquals(1_000L, PlaybackRecoveryPolicy.retryDelayMs(1))
        assertEquals(2_000L, PlaybackRecoveryPolicy.retryDelayMs(2))
        assertEquals(4_000L, PlaybackRecoveryPolicy.retryDelayMs(3))
    }

    @Test
    fun isStalled_onlyWhenBufferingWithoutProgress() {
        assertTrue(PlaybackRecoveryPolicy.isStalled(stillBuffering = true, positionAtStartMs = 5_000, currentPositionMs = 5_000))
        assertFalse(PlaybackRecoveryPolicy.isStalled(stillBuffering = true, positionAtStartMs = 5_000, currentPositionMs = 7_000))
        assertFalse(PlaybackRecoveryPolicy.isStalled(stillBuffering = false, positionAtStartMs = 5_000, currentPositionMs = 5_000))
    }
}
