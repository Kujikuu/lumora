package com.iptvcinema.tv.core.database.entity

/**
 * Whether a movie can lead the Home hero: it needs a backdrop and a synopsis.
 *
 * Stored as a column so the hero query reads an index instead of scanning every movie. It
 * mirrors the v11 back-fill, `TRIM(x) != ''`, and SQLite's TRIM removes spaces only, so the
 * sync diff does not see migrated rows as changed.
 */
object HeroArt {
    fun of(backdropUrl: String?, plot: String?): Boolean = backdropUrl.hasText() && plot.hasText()

    private fun String?.hasText(): Boolean = this != null && trim(' ').isNotEmpty()
}
