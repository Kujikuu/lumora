package com.iptvcinema.tv.core.util

object AccountDisplayNameResolver {
    fun resolve(
        email: String?,
        metadata: Map<String, Any?> = emptyMap(),
    ): String? {
        metadataDisplayName(metadata)?.let { return it }
        return email?.substringBefore("@")
            ?.takeIf { it.isNotBlank() }
            ?.replaceFirstChar { char -> char.uppercaseChar() }
    }

    fun metadataDisplayName(metadata: Map<String, Any?>): String? {
        listOf("full_name", "name", "display_name").forEach { key ->
            val value = metadata[key]?.toString()?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }
}
