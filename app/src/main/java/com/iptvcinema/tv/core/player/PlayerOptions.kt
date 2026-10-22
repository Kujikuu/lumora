package com.iptvcinema.tv.core.player

/** How the picture is fitted to the screen. Many IPTV channels come 4:3 or letterboxed. */
enum class VideoAspectMode(val storageValue: String) {
    /** Whole picture visible, black bars if needed. */
    FIT("fit"),

    /** Picture scaled to fill the screen, edges cropped. */
    FILL("fill"),

    /** Picture stretched to the screen, ratio ignored. */
    STRETCH("stretch"),
    ;

    fun next(): VideoAspectMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromStorage(raw: String?): VideoAspectMode =
            entries.firstOrNull { it.storageValue == raw } ?: FIT
    }
}

/** Playback speed for films and episodes; live streams always play at 1x. */
enum class PlaybackSpeedOption(val speed: Float, val label: String) {
    X0_75(0.75f, "0.75x"),
    X1(1f, "1x"),
    X1_25(1.25f, "1.25x"),
    X1_5(1.5f, "1.5x"),
    X2(2f, "2x"),
    ;

    companion object {
        fun fromSpeed(speed: Float): PlaybackSpeedOption =
            entries.minByOrNull { kotlin.math.abs(it.speed - speed) } ?: X1
    }
}

/** Pauses playback after a while, for viewers who fall asleep in front of the TV. */
enum class SleepTimerOption(val minutes: Int) {
    OFF(0),
    MIN_15(15),
    MIN_30(30),
    MIN_60(60),
    MIN_90(90),
    ;

    val durationMs: Long get() = minutes * 60_000L
    val isOff: Boolean get() = this == OFF

    companion object {
        /** Minutes left before [endsAtMs], never below zero. */
        fun minutesLeft(endsAtMs: Long, nowMs: Long): Int =
            ((endsAtMs - nowMs + 59_999L) / 60_000L).coerceAtLeast(0L).toInt()
    }
}
