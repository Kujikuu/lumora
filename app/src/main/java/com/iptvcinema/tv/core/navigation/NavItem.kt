package com.iptvcinema.tv.core.navigation

import androidx.annotation.StringRes
import com.iptvcinema.tv.R

enum class NavItem(
    @StringRes val labelRes: Int,
) {
    Home(R.string.nav_home),
    LiveTv(R.string.nav_live_tv),
    Movies(R.string.nav_movies),
    Series(R.string.nav_series),
    MyList(R.string.nav_favorites),
    Search(R.string.nav_search),
    Settings(R.string.nav_settings),
    Profile(R.string.nav_profile),
}
