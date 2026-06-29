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
    val isFavorite: Boolean = false,
    val showTop10Badge: Boolean = false,
    val primaryAction: HomeCardAction = HomeCardAction.WatchNow,
)

fun HomeContentCard.toFavoriteContentType(): FavoriteContentType = when (contentType) {
    "movie" -> FavoriteContentType.MOVIE
    "series" -> FavoriteContentType.SERIES
    "episode" -> FavoriteContentType.EPISODE
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
