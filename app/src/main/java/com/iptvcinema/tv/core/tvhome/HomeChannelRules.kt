package com.iptvcinema.tv.core.tvhome

object HomeChannelRules {
    /** Alternates two lists (first, second, first, ...) and appends whatever is left over. */
    fun <T> interleave(first: List<T>, second: List<T>): List<T> = buildList {
        val longest = maxOf(first.size, second.size)
        for (index in 0 until longest) {
            first.getOrNull(index)?.let(::add)
            second.getOrNull(index)?.let(::add)
        }
    }
}
