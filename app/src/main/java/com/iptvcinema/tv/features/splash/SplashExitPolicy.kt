package com.iptvcinema.tv.features.splash

enum class SplashPhase { Intro, Waiting, Exit }

object SplashExitPolicy {
    // The intro always plays to the end so the brand moment never gets cut mid-animation.
    fun phase(introFinished: Boolean, destinationReady: Boolean): SplashPhase = when {
        !introFinished -> SplashPhase.Intro
        !destinationReady -> SplashPhase.Waiting
        else -> SplashPhase.Exit
    }

    fun shouldSkipIntro(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f
}
