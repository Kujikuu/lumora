package com.iptvcinema.tv.core.supabase.mapper

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Parses ISO-8601 timestamps returned by Supabase/Postgres, which may use
 * `Z` or a numeric offset (e.g. `+00:00`) and fractional seconds.
 */
internal fun parseSupabaseInstant(value: String): Instant =
    runCatching { Instant.parse(value) }
        .getOrElse {
            OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        }
