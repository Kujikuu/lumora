package com.iptvcinema.tv.core.xtream

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object XtreamStreamUrlBuilder {
    private val TIMESHIFT_START_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm", Locale.US)

    fun liveStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        extension: String = "ts",
    ): String = "${serverUrl.trimEnd('/')}/live/$username/$password/$streamId.${extension.ifBlank { "ts" }}"

    fun vodStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        extension: String = "mp4",
    ): String = "${serverUrl.trimEnd('/')}/movie/$username/$password/$streamId.${extension.ifBlank { "mp4" }}"

    fun seriesStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        episodeId: String,
        extension: String = "mp4",
    ): String = "${serverUrl.trimEnd('/')}/series/$username/$password/$episodeId.${extension.ifBlank { "mp4" }}"

    /**
     * Catch-up URL for a programme on an archived channel. Xtream servers read the start
     * time in their own timezone, so [serverZone] must come from the account's server_info.
     */
    fun timeshiftUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        startEpochMs: Long,
        durationMinutes: Int,
        serverZone: ZoneId,
        extension: String = "ts",
    ): String {
        val start = TIMESHIFT_START_FORMAT.format(Instant.ofEpochMilli(startEpochMs).atZone(serverZone))
        return "${serverUrl.trimEnd('/')}/timeshift/$username/$password/" +
            "${durationMinutes.coerceAtLeast(1)}/$start/$streamId.${extension.ifBlank { "ts" }}"
    }
}
