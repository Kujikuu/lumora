package com.iptvcinema.launcher.content

/**
 * Copy of the app's `core/tvhome/launcher/LauncherRowsContract.kt`: the provider authority, its
 * columns and the signature permission. The launcher cannot depend on the app module, so the two
 * files are kept identical by hand; change both together.
 */
object LauncherRowsContract {
    const val APP_PACKAGE = "com.iptvcinema.tv"
    const val AUTHORITY = "com.iptvcinema.tv.launcher"
    const val CONTENT_URI = "content://$AUTHORITY/rows"
    const val METHOD_REFRESH = "refresh"

    const val COLUMN_ROW_KEY = "row_key"
    const val COLUMN_KEY = "key"
    const val COLUMN_KIND = "kind"
    const val COLUMN_TITLE = "title"
    const val COLUMN_SUBTITLE = "subtitle"
    const val COLUMN_IMAGE_URL = "image_url"
    const val COLUMN_WIDE = "wide"
    const val COLUMN_PROGRESS = "progress"
    const val COLUMN_VALID_UNTIL = "valid_until"
    const val COLUMN_INTENT_URI = "intent_uri"
    const val COLUMN_GENERATED_AT = "generated_at"

    const val ROW_CONTINUE = "continue"
    const val ROW_FAVORITES = "favorites"
    const val ROW_LIVE = "live"
    const val ROW_NEW = "new"
    val ROW_ORDER = listOf(ROW_CONTINUE, ROW_FAVORITES, ROW_LIVE, ROW_NEW)

    const val KIND_MOVIE = "movie"
    const val KIND_SERIES = "series"
    const val KIND_CHANNEL = "channel"
}
