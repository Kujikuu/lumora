package com.iptvcinema.tv.core.navigation

import com.iptvcinema.tv.core.navigation.NavItem.Home
import com.iptvcinema.tv.core.navigation.NavItem.LiveTv
import com.iptvcinema.tv.core.navigation.NavItem.Movies
import com.iptvcinema.tv.core.navigation.NavItem.MyList
import com.iptvcinema.tv.core.navigation.NavItem.Profile
import com.iptvcinema.tv.core.navigation.NavItem.Search
import com.iptvcinema.tv.core.navigation.NavItem.Series
import com.iptvcinema.tv.core.navigation.NavItem.Settings

object AppRoute {
    const val SPLASH = "splash"
    const val ACTIVATION = "activation"
    const val ADD_SOURCE = "add_source?mode={mode}"
    const val XTREAM_FORM = "xtream_form"
    const val M3U_FORM = "m3u_form"
    const val PROFILE_SELECTION = "profile_selection?mode={mode}"
    const val HOME = "home"
    const val LIVE_TV = "live_tv?channelId={channelId}"
    const val LIVE_TV_BASE = "live_tv"
    const val MOVIES = "movies?filter={filter}"
    const val SERIES = "series?filter={filter}"
    const val MOVIE_DETAILS = "movie_details/{movieId}"
    const val SERIES_DETAILS = "series_details/{seriesId}"
    const val PLAYER = "player/{contentId}/{contentType}?seriesId={seriesId}"
    const val SEARCH = "search"
    const val MY_LIST = "my_list"
    const val SETTINGS = "settings"
    const val PLAYLIST_MANAGEMENT = "playlist_management"
    const val PARENTAL_CONTROLS = "parental_controls"
    const val EMPTY_STATE = "empty_state"
    const val ERROR_STATE = "error_state"
    const val EXPIRED_ACCOUNT = "expired_account"
    const val INVALID_PLAYLIST = "invalid_playlist"

    fun addSource(mode: AddSourceMode = AddSourceMode.Onboarding) = "add_source?mode=${mode.name}"
    fun profileSelection(mode: ProfileSelectionMode = ProfileSelectionMode.Onboarding) =
        "profile_selection?mode=${mode.name}"

    fun movieDetails(movieId: String) = "movie_details/$movieId"
    fun seriesDetails(seriesId: String) = "series_details/$seriesId"
    fun player(contentId: String, contentType: String, seriesId: String? = null): String =
        if (seriesId.isNullOrBlank()) {
            "player/$contentId/$contentType"
        } else {
            "player/$contentId/$contentType?seriesId=$seriesId"
        }
    fun movies(filter: String = "") = "movies?filter=$filter"
    fun series(filter: String = "") = "series?filter=$filter"
    fun liveTv(channelId: String? = null): String = "live_tv?channelId=${channelId.orEmpty()}"

    fun navItemToRoute(item: NavItem): String = when (item) {
        Home -> HOME
        LiveTv -> liveTv()
        Movies -> movies()
        Series -> series()
        MyList -> MY_LIST
        Search -> SEARCH
        Settings -> SETTINGS
        Profile -> PROFILE_SELECTION
    }

    fun routeToNavItem(route: String?): NavItem? = when (route?.substringBefore("?")?.substringBefore("/")) {
        HOME -> Home
        LIVE_TV_BASE -> LiveTv
        MOVIES.substringBefore("?") -> Movies
        SERIES.substringBefore("?") -> Series
        MY_LIST -> MyList
        SEARCH -> Search
        SETTINGS -> Settings
        else -> null
    }

    val mainShellRoutes = setOf(
        HOME,
        LIVE_TV_BASE,
        MOVIES.substringBefore("?"),
        SERIES.substringBefore("?"),
        SEARCH,
        MY_LIST,
        SETTINGS,
    )
}
