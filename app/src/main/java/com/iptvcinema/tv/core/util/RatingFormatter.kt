package com.iptvcinema.tv.core.util

import java.util.Locale

object RatingFormatter {
    fun formatForDisplay(rating: String?): String? {
        if (rating.isNullOrBlank()) return null
        val trimmed = rating.trim()
        val numeric = trimmed.toDoubleOrNull() ?: return trimmed
        // Some providers put a year or a vote count in the rating field ("2018.0").
        if (numeric > MAX_SCORE) return null
        return String.format(Locale.US, "%.1f", numeric)
    }

    /** The top of the 0-10 scale providers use; anything above it is not a score. */
    const val MAX_SCORE = 10.0
}
