package com.iptvcinema.tv.core.xtream

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XtreamStreamUrlBuilderTest {
    @Test
    fun timeshiftUrl_formatsStartInServerZoneAndDuration() {
        val start = LocalDateTime.of(2026, 9, 30, 20, 15).toInstant(ZoneOffset.UTC).toEpochMilli()

        val url = XtreamStreamUrlBuilder.timeshiftUrl(
            serverUrl = "http://tv.example.com:8080/",
            username = "user",
            password = "pass",
            streamId = "101",
            startEpochMs = start,
            durationMinutes = 45,
            serverZone = ZoneOffset.UTC,
        )

        assertEquals("http://tv.example.com:8080/timeshift/user/pass/45/2026-09-30:20-15/101.ts", url)
    }

    @Test
    fun timeshiftUrl_rollsDateForwardWhenServerZoneIsAhead() {
        // 22:30 UTC is already the next day in Dubai (UTC+4).
        val start = LocalDateTime.of(2026, 9, 30, 22, 30).toInstant(ZoneOffset.UTC).toEpochMilli()

        val url = XtreamStreamUrlBuilder.timeshiftUrl(
            serverUrl = "http://tv.example.com",
            username = "user",
            password = "pass",
            streamId = "7",
            startEpochMs = start,
            durationMinutes = 0,
            serverZone = ZoneId.of("Asia/Dubai"),
        )

        assertEquals("http://tv.example.com/timeshift/user/pass/1/2026-10-01:02-30/7.ts", url)
    }

    @Test
    fun parseZoneOrNull_acceptsRegionIdsAndRejectsGarbage() {
        assertEquals(ZoneId.of("Europe/London"), parseZoneOrNull(" Europe/London "))
        assertNull(parseZoneOrNull("Not/AZone"))
        assertNull(parseZoneOrNull(""))
        assertNull(parseZoneOrNull(null))
    }
}
