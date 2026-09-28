package com.iptvcinema.tv.core.parental

sealed interface PinCheck {
    data object Accepted : PinCheck
    data class Wrong(val triesLeft: Int) : PinCheck
    data class LockedOut(val secondsLeft: Int) : PinCheck

    /** Parental controls could not be loaded, so the PIN cannot be checked. Fails closed. */
    data object Unavailable : PinCheck
}

/** Limits PIN guessing: after [maxAttempts] wrong tries, every try is refused for [lockoutMs]. */
class PinAttemptLimiter(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val lockoutMs: Long = DEFAULT_LOCKOUT_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var failedAttempts = 0
    private var lockedUntilMs = 0L

    @Synchronized
    fun check(correct: Boolean): PinCheck {
        val now = clock()
        if (now < lockedUntilMs) return PinCheck.LockedOut(secondsUntil(now))
        if (lockedUntilMs != 0L) {
            lockedUntilMs = 0L
            failedAttempts = 0
        }
        if (correct) {
            failedAttempts = 0
            return PinCheck.Accepted
        }
        failedAttempts++
        if (failedAttempts >= maxAttempts) {
            lockedUntilMs = now + lockoutMs
            return PinCheck.LockedOut(secondsUntil(now))
        }
        return PinCheck.Wrong(triesLeft = maxAttempts - failedAttempts)
    }

    private fun secondsUntil(now: Long): Int = ((lockedUntilMs - now + 999) / 1000).toInt()

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5
        const val DEFAULT_LOCKOUT_MS = 30_000L
    }
}
