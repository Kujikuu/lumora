package com.iptvcinema.tv.features.splash

enum class SplashPhase { Intro, Waiting, Exit }

object SplashExitPolicy {
    /**
     * How long the brand shows before the splash may leave early. Long enough for the logo
     * bands and card to land; the rest of the intro is not worth a slower start on a weak TV.
     */
    const val MINIMUM_BRAND_MS = 650L

    // Leaves as soon as the destination is known and the brand has had its moment; the full
    // intro only plays when the destination takes longer than that.
    fun phase(introFinished: Boolean, minimumShown: Boolean, destinationReady: Boolean): SplashPhase = when {
        destinationReady && (introFinished || minimumShown) -> SplashPhase.Exit
        !introFinished -> SplashPhase.Intro
        else -> SplashPhase.Waiting
    }

    fun shouldSkipIntro(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f
}
