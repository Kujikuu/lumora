package com.iptvcinema.launcher.apps

/** One installed app that can be opened from the Apps row. Plain data, so sorting is unit tested. */
data class AppEntry(
    val packageName: String,
    val label: String,
    /** True when the app declares a LEANBACK_LAUNCHER activity (made for TV). */
    val isLeanback: Boolean,
    val isSystem: Boolean,
    /** Changes on every update; part of the icon cache key so a new icon replaces the old file. */
    val lastUpdateTimeMs: Long,
)
