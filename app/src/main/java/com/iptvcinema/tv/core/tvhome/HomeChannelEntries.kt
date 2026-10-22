package com.iptvcinema.tv.core.tvhome

enum class HomeProgramKind { Movie, Series, Channel }

/** One card in a home-screen channel row, independent of the TV provider API. */
data class HomeProgramEntry(
    val key: String,
    val kind: HomeProgramKind,
    val title: String,
    val description: String?,
    val posterUrl: String?,
    /** Live channel logos and backdrops are wide; movie and series posters are tall. */
    val isWidePoster: Boolean,
    val intentUri: String,
    /** Fraction watched, for Continue watching cards; null when not applicable. */
    val progress: Float? = null,
    /** When [description] stops being true (end of the current programme); null means no expiry. */
    val validUntilMs: Long? = null,
)

/** One row on the Android TV home screen, keyed so it can be found again across runs. */
data class HomeChannelSpec(
    val key: String,
    val displayName: String,
    val programs: List<HomeProgramEntry>,
)

/** Row keys shared by the TV provider rows and the launcher snapshot. */
object HomeRowKeys {
    const val CONTINUE = "continue"
    const val FAVORITES = "favorites"
    const val LIVE = "live"
    const val NEW = "new"

    /** Rows published to the Android TV home screen; Live now changes too often for the provider. */
    val TV_PROVIDER_ROWS = listOf(CONTINUE, FAVORITES, NEW)
}
