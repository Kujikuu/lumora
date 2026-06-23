package com.iptvcinema.tv.core.parental

object RatingPolicy {
    private val ratingOrder = listOf(
        "G",
        "PG",
        "PG-13",
        "12+",
        "TV-PG",
        "TV-14",
        "16+",
        "R",
        "TV-MA",
        "18+",
        "NC-17",
        "XXX",
    )

    private val aliases = mapOf(
        "PG13" to "PG-13",
        "TVPG" to "TV-PG",
        "TV14" to "TV-14",
        "TVMA" to "TV-MA",
        "NC17" to "NC-17",
        "ADULT" to "18+",
    )

    fun normalize(rating: String?): String? {
        if (rating.isNullOrBlank()) return null
        val trimmed = rating.trim()
        val upper = trimmed.uppercase()
        aliases[upper.replace("-", "").replace(" ", "")]?.let { return it }
        ratingOrder.firstOrNull { it.equals(trimmed, ignoreCase = true) }?.let { return it }
        return trimmed
    }

    fun rank(rating: String?): Int {
        val normalized = normalize(rating) ?: return -1
        val exact = ratingOrder.indexOfFirst { it.equals(normalized, ignoreCase = true) }
        if (exact >= 0) return exact
        val digits = normalized.filter { it.isDigit() }.toIntOrNull()
        return when {
            digits != null && normalized.contains('+') -> ratingOrder.indexOf("12+") + when {
                digits <= 12 -> 0
                digits <= 16 -> 2
                else -> 4
            }
            normalized.contains("18", ignoreCase = true) -> ratingOrder.indexOf("18+")
            normalized.contains("16", ignoreCase = true) -> ratingOrder.indexOf("16+")
            normalized.contains("12", ignoreCase = true) -> ratingOrder.indexOf("12+")
            else -> -1
        }
    }

    fun isAllowed(contentRating: String?, maxRating: String?): Boolean {
        val max = normalize(maxRating) ?: return true
        val content = normalize(contentRating)
        if (content == null) return true
        val contentRank = rank(content)
        val maxRank = rank(max)
        if (contentRank < 0 || maxRank < 0) return true
        return contentRank <= maxRank
    }
}
