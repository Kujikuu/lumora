package com.iptvcinema.launcher.system

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatterTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val afternoon = 1_700_000_000_000L // 2023-11-14 22:13:20 UTC

    @Test
    fun twentyFourHour_hasNoSuffix() {
        assertEquals("22:13", ClockFormatter.format(afternoon, Locale.US, is24Hour = true, zone = utc))
    }

    @Test
    fun twelveHour_hasTheLocaleMarker() {
        assertEquals("10:13 PM", ClockFormatter.format(afternoon, Locale.US, is24Hour = false, zone = utc))
    }

    @Test
    fun arabicLatinDigits_keepWesternNumerals() {
        val arabic = Locale.forLanguageTag("ar-u-nu-latn")
        assertEquals("22:13", ClockFormatter.format(afternoon, arabic, is24Hour = true, zone = utc))
    }

    @Test
    fun date_isShortWeekdayDayMonth() {
        assertEquals("Tue 14 Nov", ClockFormatter.formatDate(afternoon, Locale.US, zone = utc))
    }
}
