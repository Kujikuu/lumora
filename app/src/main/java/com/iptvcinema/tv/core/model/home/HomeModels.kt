package com.iptvcinema.tv.core.model.home

import com.iptvcinema.tv.core.model.FavoriteContentType

enum class HomeCardAction {
    WatchNow,
    ContinueWatching,
}

data class HomeContentCard(
    val contentId: String,
    val contentType: String,
    val seriesId: String? = null,
    val title: String,
    val imageUrl: String? = null,
    val backdropUrl: String? = null,
    val year: String? = null,
    val genres: List<String> = emptyList(),
    val plot: String? = null,
    val runtimeOrEpisodes: String? = null,
    val highlightText: String? = null,
    val subtitle: String? = null,
    val progress: Float? = null,
    val remainingTimeLabel: String? = null,
    val isFavorite: Boolean = false,
    val showTop10Badge: Boolean = false,
    /** Position in a ranked rail (1-based), drawn as a large number on Home cards. */
    val rank: Int? = null,
    val rating: String? = null,
    val primaryAction: HomeCardAction = HomeCardAction.WatchNow,
)

/** Card types that open a browse page instead of a title. */
object BrowseCardTypes {
    const val MOVIE_CATEGORY = "movie_category"
    const val SERIES_CATEGORY = "series_category"
    const val SEARCH_TERM = "search_term"
}

val HomeContentCard.isCategory: Boolean
    get() = contentType == BrowseCardTypes.MOVIE_CATEGORY || contentType == BrowseCardTypes.SERIES_CATEGORY

fun HomeContentCard.toFavoriteContentType(): FavoriteContentType = when (contentType) {
    "movie" -> FavoriteContentType.MOVIE
    "series" -> FavoriteContentType.SERIES
    "episode" -> FavoriteContentType.EPISODE
    "channel" -> FavoriteContentType.CHANNEL
    else -> FavoriteContentType.MOVIE
}

enum class MoodBrowseTarget {
    Movies,
    Series,
}

data class MoodCategory(
    val id: String,
    val labelRes: Int,
    val filter: String,
    val target: MoodBrowseTarget,
    val gradientStartArgb: Long,
    val gradientEndArgb: Long,
)
