package com.iptvcinema.tv.core.database.entity

/**
 * Numeric form of a provider rating, used to sort top-rated rails through an index.
 *
 * It mirrors SQLite's `CAST(rating AS REAL)`, which the v10 migration uses to back-fill old
 * rows. Matching it keeps old and new rows ranked the same way, and keeps the sync diff from
 * seeing every migrated row as changed.
 */
object RatingValue {
    // SQLite skips ASCII whitespace, then reads the longest leading real literal. "7.5/10" reads
    // as 7.5; text with no leading number ("N/A", "") reads as 0.0.
    private val LEADING_REAL = Regex(
        "^[ \\t\\n\\u000B\\u000C\\r]*([+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)",
    )

    fun of(rating: String?): Double? {
        if (rating == null) return null
        val literal = LEADING_REAL.find(rating)?.groupValues?.get(1) ?: return 0.0
        return literal.toDouble()
    }
}
