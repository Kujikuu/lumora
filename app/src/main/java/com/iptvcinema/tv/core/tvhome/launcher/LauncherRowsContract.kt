package com.iptvcinema.tv.core.tvhome.launcher

/**
 * The contract between Lumora Play and the Lumora Home launcher APK. The launcher keeps an identical
 * copy of these constants (it cannot depend on this module); change both together.
 */
object LauncherRowsContract {
    const val AUTHORITY = "com.iptvcinema.tv.launcher"
    const val PATH_ROWS = "rows"
    const val CONTENT_URI = "content://$AUTHORITY/$PATH_ROWS"

    /** Signature-level: only an APK signed with the app's key can read the rows. */
    const val READ_PERMISSION = "com.iptvcinema.tv.permission.READ_LAUNCHER_ROWS"

    /** `call` method that asks the app to rebuild its rows now. */
    const val METHOD_REFRESH = "refresh"

    const val COLUMN_ROW_KEY = "row_key"
    const val COLUMN_ROW_TITLE = "row_title"
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

    val COLUMNS = arrayOf(
        COLUMN_ROW_KEY, COLUMN_ROW_TITLE, COLUMN_KEY, COLUMN_KIND, COLUMN_TITLE, COLUMN_SUBTITLE,
        COLUMN_IMAGE_URL, COLUMN_WIDE, COLUMN_PROGRESS, COLUMN_VALID_UNTIL, COLUMN_INTENT_URI,
        COLUMN_GENERATED_AT,
    )

    const val KIND_MOVIE = "movie"
    const val KIND_SERIES = "series"
    const val KIND_CHANNEL = "channel"

    /** Snapshot file the app writes and the provider serves, under the app's files dir. */
    const val SNAPSHOT_DIR = "launcher"
    const val SNAPSHOT_FILE = "rows.json"
}
