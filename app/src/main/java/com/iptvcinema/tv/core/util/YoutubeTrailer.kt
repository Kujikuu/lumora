package com.iptvcinema.tv.core.util

fun youtubeTrailerUrl(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    return when {
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) -> value
        else -> "https://www.youtube.com/watch?v=$value"
    }
}
