package com.iptvcinema.tv.core.player

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PlaybackSessionTracker @Inject constructor() {
    private val _currentLiveChannelId = MutableStateFlow<String?>(null)
    val currentLiveChannelId: StateFlow<String?> = _currentLiveChannelId.asStateFlow()

    fun setCurrentLiveChannel(channelId: String?) {
        _currentLiveChannelId.value = channelId
    }
}
