package com.iptvcinema.tv.features.livetv

import androidx.compose.ui.input.key.Key

/**
 * Digits typed on the remote while watching live TV. The entry commits on its own once the
 * viewer stops typing for [COMMIT_DELAY_MS], or as soon as [MAX_DIGITS] are in.
 */
data class ChannelNumberEntry(val digits: String = "") {
    val isEmpty: Boolean get() = digits.isEmpty()
    val isComplete: Boolean get() = digits.length >= MAX_DIGITS
    val number: Int? get() = digits.toIntOrNull()

    fun append(digit: Char): ChannelNumberEntry {
        if (!digit.isDigit() || isComplete) return this
        return copy(digits = digits + digit)
    }

    companion object {
        const val MAX_DIGITS = 4
        const val COMMIT_DELAY_MS = 1_500L

        /** The digit a remote key stands for, or null for any other key. */
        fun digitFor(key: Key): Char? = when (key) {
            Key.Zero, Key.NumPad0 -> '0'
            Key.One, Key.NumPad1 -> '1'
            Key.Two, Key.NumPad2 -> '2'
            Key.Three, Key.NumPad3 -> '3'
            Key.Four, Key.NumPad4 -> '4'
            Key.Five, Key.NumPad5 -> '5'
            Key.Six, Key.NumPad6 -> '6'
            Key.Seven, Key.NumPad7 -> '7'
            Key.Eight, Key.NumPad8 -> '8'
            Key.Nine, Key.NumPad9 -> '9'
            else -> null
        }
    }
}

object ChannelNumberResolver {
    /**
     * The channel a typed [number] means. A playlist that numbers its channels wins; otherwise the
     * number is the channel's 1-based place in [channels], which is what the strip shows.
     */
    fun <T> resolve(channels: List<T>, number: Int, channelNumberOf: (T) -> Int?): T? {
        if (number <= 0 || channels.isEmpty()) return null
        val numbered = channels.any { channelNumberOf(it)?.let { n -> n > 0 } == true }
        if (numbered) return channels.firstOrNull { channelNumberOf(it) == number }
        return channels.getOrNull(number - 1)
    }
}
