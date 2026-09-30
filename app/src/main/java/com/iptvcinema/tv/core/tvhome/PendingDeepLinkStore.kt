package com.iptvcinema.tv.core.tvhome

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds a deep link from the launcher until the app can act on it: on a cold start it waits
 * for the splash screen and session checks, then the navigation graph plays it and consumes it.
 */
@Singleton
class PendingDeepLinkStore @Inject constructor() {
    private val _pending = MutableStateFlow<PlaybackDeepLink?>(null)
    val pending: StateFlow<PlaybackDeepLink?> = _pending.asStateFlow()

    fun offer(uri: String?) {
        PlaybackDeepLink.parse(uri)?.let { _pending.value = it }
    }

    fun consume(link: PlaybackDeepLink) {
        _pending.compareAndSet(link, null)
    }
}
