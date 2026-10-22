package com.iptvcinema.tv.core.reminders

import android.util.Log
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.util.safeSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Watches the current profile's reminders and raises [current] a minute before a programme
 * starts. A raised reminder is removed from storage, so it fires once; the UI clears it with
 * [dismiss].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProgramReminderAlerts @Inject constructor(
    appSessionRepository: AppSessionRepository,
    private val repository: ProgramReminderRepository,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    private val _current = MutableStateFlow<ProgramReminder?>(null)
    val current: StateFlow<ProgramReminder?> = _current.asStateFlow()

    init {
        applicationScope.launch {
            appSessionRepository.sessionState
                .map { it.currentProfileId }
                .distinctUntilChanged()
                .flatMapLatest { profileId ->
                    if (profileId == null) flowOf(emptyList()) else repository.observe(profileId)
                }
                .catch { error -> Log.w(TAG, "Reminders unavailable: ${error.safeSummary()}") }
                .collectLatest { reminders ->
                    try {
                        waitAndRaise(reminders)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.w(TAG, "Reminder check failed: ${error.safeSummary()}")
                    }
                }
        }
    }

    fun dismiss() {
        _current.value = null
    }

    private suspend fun waitAndRaise(reminders: List<ProgramReminder>) {
        while (true) {
            val now = System.currentTimeMillis()
            val stale = reminders.size - ProgramReminderPolicy.prune(reminders, now).size
            if (stale > 0) {
                reminders.firstOrNull()?.let { repository.prune(it.profileId, now) }
                return
            }
            // One card at a time: a second due reminder waits until the first is dismissed.
            if (_current.value != null) {
                delay(RECHECK_WHILE_SHOWING_MS)
                continue
            }
            val due = ProgramReminderPolicy.due(reminders, now)
            if (due != null) {
                _current.value = due
                // Removing it re-emits the list, which cancels this wait and starts a fresh one.
                repository.remove(due.profileId, due.id)
                return
            }
            val delayMs = ProgramReminderPolicy.nextWakeDelayMs(reminders, now) ?: return
            delay(delayMs.coerceAtMost(MAX_SLEEP_MS))
        }
    }

    private companion object {
        const val TAG = "ProgramReminders"
        // Re-check at least this often, in case the TV clock jumps after standby.
        const val MAX_SLEEP_MS = 5 * 60_000L
        const val RECHECK_WHILE_SHOWING_MS = 5_000L
    }
}
