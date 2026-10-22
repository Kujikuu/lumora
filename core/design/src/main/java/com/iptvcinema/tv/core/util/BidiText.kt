package com.iptvcinema.tv.core.util

private const val FIRST_STRONG_ISOLATE = '\u2068'
private const val POP_DIRECTIONAL_ISOLATE = '\u2069'

/**
 * Lets text from the catalog (usually English) keep its own direction inside an Arabic
 * screen. Without this, trailing punctuation and "S1 E2" style codes jump to the wrong side.
 * Arabic text is unaffected: the isolate picks the direction from the first strong letter.
 */
fun String.isolateDirection(): String =
    if (isEmpty() || first() == FIRST_STRONG_ISOLATE) this else "$FIRST_STRONG_ISOLATE$this$POP_DIRECTIONAL_ISOLATE"
