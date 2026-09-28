package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.util.RatingFormatter

/** Pure carousel rules, kept out of Compose so they can be unit tested. */
internal object HeroCarouselLogic {
    fun nextIndex(current: Int, count: Int): Int = if (count <= 0) 0 else (current + 1) % count

    /** Rating chip text, or null when the provider sent no rating or a zero placeholder. */
    fun ratingBadge(rating: String?): String? {
        val formatted = RatingFormatter.formatForDisplay(rating) ?: return null
        val numeric = formatted.toDoubleOrNull()
        if (numeric != null && numeric <= 0.0) return null
        return "★ $formatted"
    }

    fun metadata(movie: MovieItem): List<String> = listOfNotNull(
        movie.year.takeIf { it > 0 }?.toString(),
        movie.genres.take(2).joinToString(" · ").takeIf { it.isNotBlank() },
        movie.runtimeMinutes.takeIf { it > 0 }?.let { "${it}m" },
    )
}
