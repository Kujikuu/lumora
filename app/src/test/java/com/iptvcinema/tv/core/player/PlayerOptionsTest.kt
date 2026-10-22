package com.iptvcinema.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerOptionsTest {
    @Test
    fun aspectMode_cyclesAndRoundTripsStorage() {
        assertEquals(VideoAspectMode.FILL, VideoAspectMode.FIT.next())
        assertEquals(VideoAspectMode.FIT, VideoAspectMode.STRETCH.next())
        assertEquals(VideoAspectMode.STRETCH, VideoAspectMode.fromStorage("stretch"))
        assertEquals(VideoAspectMode.FIT, VideoAspectMode.fromStorage("garbage"))
    }

    @Test
    fun speed_matchesTheNearestOption() {
        assertEquals(PlaybackSpeedOption.X1_25, PlaybackSpeedOption.fromSpeed(1.3f))
        assertEquals(PlaybackSpeedOption.X1, PlaybackSpeedOption.fromSpeed(1f))
    }

    @Test
    fun sleepTimer_minutesLeftRoundsUpAndNeverGoesNegative() {
        assertEquals(1, SleepTimerOption.minutesLeft(endsAtMs = 61_000L, nowMs = 1_000L))
        assertEquals(2, SleepTimerOption.minutesLeft(endsAtMs = 61_001L, nowMs = 1_000L))
        assertEquals(0, SleepTimerOption.minutesLeft(endsAtMs = 0L, nowMs = 5_000L))
        assertEquals(30 * 60_000L, SleepTimerOption.MIN_30.durationMs)
    }
}
