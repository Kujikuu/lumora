package com.iptvcinema.tv.core.util

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
            val value = metadata[key].asPlainText()?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }

    // Metadata comes from JSON: a JsonPrimitive's toString() keeps the quotes ("\"Ahmed\"").
    private fun Any?.asPlainText(): String? = when (this) {
        null, is JsonNull -> null
        is JsonPrimitive -> contentOrNull
        else -> toString()
    }
}
