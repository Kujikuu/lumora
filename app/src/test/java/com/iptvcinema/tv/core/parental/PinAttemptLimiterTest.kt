package com.iptvcinema.tv.core.parental

import org.junit.Assert.assertEquals
import org.junit.Test

class PinAttemptLimiterTest {
    private var now = 0L
    private val limiter = PinAttemptLimiter(maxAttempts = 3, lockoutMs = 30_000L, clock = { now })

    @Test
    fun correctPin_isAccepted() {
        assertEquals(PinCheck.Accepted, limiter.check(correct = true))
    }

    @Test
    fun wrongPin_reportsRemainingTries() {
        assertEquals(PinCheck.Wrong(triesLeft = 2), limiter.check(correct = false))
        assertEquals(PinCheck.Wrong(triesLeft = 1), limiter.check(correct = false))
    }

    @Test
    fun tooManyWrongPins_locksOut_evenForTheRightPin() {
        repeat(2) { limiter.check(correct = false) }

        assertEquals(PinCheck.LockedOut(secondsLeft = 30), limiter.check(correct = false))
        now += 10_000
        assertEquals(PinCheck.LockedOut(secondsLeft = 20), limiter.check(correct = true))
    }

    @Test
    fun lockoutEnds_andCounterResets() {
        repeat(3) { limiter.check(correct = false) }
        now += 30_000

        assertEquals(PinCheck.Wrong(triesLeft = 2), limiter.check(correct = false))
    }

    @Test
    fun correctPin_resetsCounter() {
        repeat(2) { limiter.check(correct = false) }
        limiter.check(correct = true)

        assertEquals(PinCheck.Wrong(triesLeft = 2), limiter.check(correct = false))
    }
}
