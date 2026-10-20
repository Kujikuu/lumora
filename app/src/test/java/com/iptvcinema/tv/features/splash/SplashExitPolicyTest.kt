package com.iptvcinema.tv.features.splash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashExitPolicyTest {
    @Test
    fun phase_staysInIntroUntilTheBrandHasShownLongEnough() {
        assertEquals(
            SplashPhase.Intro,
            SplashExitPolicy.phase(introFinished = false, minimumShown = false, destinationReady = true),
        )
        assertEquals(
            SplashPhase.Intro,
            SplashExitPolicy.phase(introFinished = false, minimumShown = false, destinationReady = false),
        )
    }

    @Test
    fun phase_keepsPlayingTheIntroWhileTheDestinationIsNotReady() {
        assertEquals(
            SplashPhase.Intro,
            SplashExitPolicy.phase(introFinished = false, minimumShown = true, destinationReady = false),
        )
    }

    @Test
    fun phase_exitsEarlyOnceMinimumShownAndDestinationReady() {
        assertEquals(
            SplashPhase.Exit,
            SplashExitPolicy.phase(introFinished = false, minimumShown = true, destinationReady = true),
        )
    }

    @Test
    fun phase_waitsAfterIntroUntilDestinationIsReady() {
        assertEquals(
            SplashPhase.Waiting,
            SplashExitPolicy.phase(introFinished = true, minimumShown = true, destinationReady = false),
        )
    }

    @Test
    fun phase_exitsOnceIntroFinishedAndDestinationReady() {
        assertEquals(
            SplashPhase.Exit,
            SplashExitPolicy.phase(introFinished = true, minimumShown = true, destinationReady = true),
        )
        // With animations off the intro counts as finished at once, so there is no minimum.
        assertEquals(
            SplashPhase.Exit,
            SplashExitPolicy.phase(introFinished = true, minimumShown = false, destinationReady = true),
        )
    }

    @Test
    fun minimumBrandTime_isShortButLetsTheMarkLand() {
        assertTrue(SplashExitPolicy.MINIMUM_BRAND_MS in 500L..700L)
    }

    @Test
    fun shouldSkipIntro_onlyWhenSystemAnimationsAreOff() {
        assertTrue(SplashExitPolicy.shouldSkipIntro(animatorDurationScale = 0f))
        assertFalse(SplashExitPolicy.shouldSkipIntro(animatorDurationScale = 1f))
        assertFalse(SplashExitPolicy.shouldSkipIntro(animatorDurationScale = 0.5f))
    }
}
