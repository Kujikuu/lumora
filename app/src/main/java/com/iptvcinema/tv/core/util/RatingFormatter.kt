package com.iptvcinema.tv.core.util

import java.util.Locale

object RatingFormatter {
    fun formatForDisplay(rating: String?): String? {
        if (rating.isNullOrBlank()) return null
        val trimmed = rating.trim()
        val numeric = trimmed.toDoubleOrNull() ?: return trimmed
        return String.format(Locale.US, "%.1f", numeric)
    }
}
