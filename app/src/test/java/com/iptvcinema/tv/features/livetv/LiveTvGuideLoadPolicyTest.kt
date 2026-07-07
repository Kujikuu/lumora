package com.iptvcinema.tv.features.livetv

import com.iptvcinema.tv.core.model.ChannelItem
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveTvGuideLoadPolicyTest {
    @Test
    fun defaultProgramChannelIds_keepsSelectedChannelAndSmallOnNowSet() {
        val channels = (1..8).map { channel("channel-$it") }

        val result = LiveTvGuideLoadPolicy.defaultProgramChannelIds(
            channels = channels,
            selectedChannelId = "channel-7",
        )

        assertEquals(
            listOf("channel-7", "channel-1", "channel-2", "channel-3", "channel-4", "channel-5"),
            result,
        )
    }

    @Test
    fun fullGuideChannelIds_returnsEveryChannel() {
        val channels = (1..8).map { channel("channel-$it") }

        val result = LiveTvGuideLoadPolicy.fullGuideChannelIds(channels)

        assertEquals(channels.map { it.id }, result)
    }

    private fun channel(id: String) = ChannelItem(
        id = id,
        name = id,
        category = "News",
        currentProgram = "Now",
        programStart = "10:00",
        programEnd = "11:00",
        programProgress = 0.5f,
    )
}
