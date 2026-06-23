package com.iptvcinema.tv.core.xtream

import java.net.URI

object XtreamUrlNormalizer {
    fun normalize(input: String): Result<String> {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Server URL is required"))
        }
        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "http://$trimmed"
        }
        val withoutTrailing = withScheme.trimEnd('/')
        return runCatching {
            URI(withoutTrailing).host ?: error("Invalid server URL")
            withoutTrailing
        }.recoverCatching {
            throw IllegalArgumentException("Invalid server URL")
        }
    }
}
