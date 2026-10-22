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
)

/** One row on the Android TV home screen, keyed so it can be found again across runs. */
data class HomeChannelSpec(
    val key: String,
    val displayName: String,
    val programs: List<HomeProgramEntry>,
)
