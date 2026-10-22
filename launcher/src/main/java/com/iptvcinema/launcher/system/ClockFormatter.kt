package com.iptvcinema.launcher.system

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Header clock text, built from a locale and the user's 12/24-hour choice. */
object ClockFormatter {
    fun format(epochMs: Long, locale: Locale, is24Hour: Boolean, zone: TimeZone = TimeZone.getDefault()): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, locale).apply { timeZone = zone }.format(Date(epochMs))
    }

    fun formatDate(epochMs: Long, locale: Locale, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("EEE d MMM", locale).apply { timeZone = zone }.format(Date(epochMs))
}
