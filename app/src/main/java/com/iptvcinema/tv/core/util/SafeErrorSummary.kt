package com.iptvcinema.tv.core.util

/**
 * One-line description of an error that is safe to log. Supabase errors carry the request
 * URL and headers (including the bearer token) after the first line, so only that is kept.
 */
fun Throwable?.safeSummary(): String =
    if (this == null) "unknown" else "${javaClass.simpleName}: ${message?.lineSequence()?.firstOrNull().orEmpty()}"
