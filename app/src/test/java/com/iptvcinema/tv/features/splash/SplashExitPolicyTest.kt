package com.iptvcinema.tv.features.splash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashExitPolicyTest {
    @Test
    fun phase_staysInIntroWhileAnimationRuns_evenIfDestinationIsReady() {
        assertEquals(SplashPhase.Intro, SplashExitPolicy.phase(introFinished = false, destinationReady = true))
        assertEquals(SplashPhase.Intro, SplashExitPolicy.phase(introFinished = false, destinationReady = false))
    }

    @Test
    fun phase_waitsAfterIntroUntilDestinationIsReady() {
        assertEquals(SplashPhase.Waiting, SplashExitPolicy.phase(introFinished = true, destinationReady = false))
    }

    @Test
    fun phase_exitsOnceIntroFinishedAndDestinationReady() {
        assertEquals(SplashPhase.Exit, SplashExitPolicy.phase(introFinished = true, destinationReady = true))
    }

    @Test
    fun shouldSkipIntro_onlyWhenSystemAnimationsAreOff() {
        assertTrue(SplashExitPolicy.shouldSkipIntro(animatorDurationScale = 0f))
        assertFalse(SplashExitPolicy.shouldSkipIntro(animatorDurationScale = 1f))
        assertFalse(SplashExitPolicy.shouldSkipIntro(animatorDurationScale = 0.5f))
    }
}
