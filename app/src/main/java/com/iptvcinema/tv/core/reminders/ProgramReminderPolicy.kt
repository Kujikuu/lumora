package com.iptvcinema.tv.core.reminders

/** When reminders fire and when they are stale, kept pure so the timing is testable. */
object ProgramReminderPolicy {
    /** The alert shows this long before the programme starts. */
    const val LEAD_MS = 60_000L

    /** A reminder whose programme started this long ago is dropped unseen. */
    const val GRACE_MS = 10 * 60_000L

    /** Only programmes that have not started yet can be reminded about. */
    fun canRemind(startEpochMs: Long, nowMs: Long): Boolean = startEpochMs > nowMs + LEAD_MS

    /** Reminders still worth keeping: not yet past their grace window. */
    fun prune(reminders: List<ProgramReminder>, nowMs: Long): List<ProgramReminder> =
        reminders.filter { it.startEpochMs + GRACE_MS > nowMs }

    /** The reminder to show now, earliest first, or null when none is due yet. */
    fun due(reminders: List<ProgramReminder>, nowMs: Long): ProgramReminder? =
        prune(reminders, nowMs)
            .filter { it.startEpochMs - LEAD_MS <= nowMs }
            .minByOrNull { it.startEpochMs }

    /** How long to sleep before the next reminder is due, or null when there is none. */
    fun nextWakeDelayMs(reminders: List<ProgramReminder>, nowMs: Long): Long? =
        prune(reminders, nowMs)
            .minOfOrNull { it.startEpochMs - LEAD_MS - nowMs }
            ?.coerceAtLeast(0L)
}
