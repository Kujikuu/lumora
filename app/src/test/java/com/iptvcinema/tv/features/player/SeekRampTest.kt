package com.iptvcinema.tv.features.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SeekRampTest {
    @Test
    fun singlePress_seeksTenSeconds() {
        val ramp = SeekRamp().press(nowMs = 1_000L, direction = 1)
        assertEquals(10_000L, ramp.deltaMs)
    }

    @Test
    fun quickPresses_growTheStep() {
        var ramp = SeekRamp()
        var now = 0L
        repeat(4) { now += 300L; ramp = ramp.press(now, 1) }
        assertEquals(30_000L, ramp.deltaMs)
        repeat(5) { now += 300L; ramp = ramp.press(now, 1) }
        assertEquals(60_000L, ramp.deltaMs)
    }

    @Test
    fun pause_resetsTheRun() {
        var ramp = SeekRamp()
        repeat(5) { ramp = ramp.press(it * 100L, 1) }
        ramp = ramp.press(ramp.lastPressMs + SeekRamp.RESET_AFTER_MS + 1L, 1)
        assertEquals(10_000L, ramp.deltaMs)
    }

    @Test
    fun changingDirection_startsOver() {
        var ramp = SeekRamp()
        repeat(5) { ramp = ramp.press(it * 100L, 1) }
        ramp = ramp.press(ramp.lastPressMs + 100L, -1)
        assertEquals(-10_000L, ramp.deltaMs)
    }

    @Test
    fun label_formatsSecondsAndMinutes() {
        assertEquals("+30s", SeekRamp.label(30_000L))
        assertEquals("-1m", SeekRamp.label(-60_000L))
    }
}
