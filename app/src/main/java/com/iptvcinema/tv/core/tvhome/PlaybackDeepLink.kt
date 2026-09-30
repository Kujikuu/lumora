package com.iptvcinema.tv.core.tvhome

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * A "play this" link from outside the app, such as a Watch Next card on the TV home screen.
 * It carries no position: the player resumes from watch history, which is always newer.
 */
data class PlaybackDeepLink(
    /** "movie" or "episode", the same strings the player route uses. */
    val contentType: String,
    val contentId: String,
    val seriesId: String? = null,
    /** Profile the item belongs to; the link is only played for that profile. */
    val profileId: String? = null,
) {
    fun toUri(): String {
        val params = buildList {
            add("type" to contentType)
            add("id" to contentId)
            seriesId?.takeIf { it.isNotBlank() }?.let { add("series" to it) }
            profileId?.takeIf { it.isNotBlank() }?.let { add("profile" to it) }
        }
        return "$SCHEME://$HOST?" + params.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    }

    companion object {
        const val SCHEME = "lumora"
        const val HOST = "play"
        private val PLAYABLE_TYPES = setOf("movie", "episode")

        fun parse(uri: String?): PlaybackDeepLink? {
            if (uri.isNullOrBlank()) return null
            val parsed = runCatching { URI(uri) }.getOrNull() ?: return null
            if (!parsed.scheme.equals(SCHEME, ignoreCase = true) || !parsed.host.equals(HOST, ignoreCase = true)) {
                return null
            }
            val params = parsed.rawQuery.orEmpty()
                .split('&')
                .mapNotNull { pair ->
                    val key = pair.substringBefore('=', missingDelimiterValue = "")
                    if (key.isEmpty()) return@mapNotNull null
                    key to decode(pair.substringAfter('=', missingDelimiterValue = ""))
                }
                .toMap()
            val type = params["type"]?.lowercase()?.takeIf { it in PLAYABLE_TYPES } ?: return null
            val id = params["id"]?.takeIf { it.isNotBlank() } ?: return null
            return PlaybackDeepLink(
                contentType = type,
                contentId = id,
                seriesId = params["series"]?.takeIf { it.isNotBlank() },
                profileId = params["profile"]?.takeIf { it.isNotBlank() },
            )
        }

        private fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

        private fun decode(value: String): String =
            runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)
    }
}
