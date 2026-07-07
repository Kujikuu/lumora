package com.iptvcinema.tv.features.livetv

import com.iptvcinema.tv.core.model.ChannelItem

internal object LiveTvGuideLoadPolicy {
    private const val ON_NOW_CHANNEL_LIMIT = 5

    fun defaultProgramChannelIds(
        channels: List<ChannelItem>,
        selectedChannelId: String?,
    ): List<String> = buildList {
        selectedChannelId
            ?.takeIf { selectedId -> channels.any { it.id == selectedId } }
            ?.let(::add)

        channels.take(ON_NOW_CHANNEL_LIMIT).forEach { channel ->
            if (channel.id !in this) add(channel.id)
        }
    }

    fun fullGuideChannelIds(channels: List<ChannelItem>): List<String> = channels.map { it.id }
}
