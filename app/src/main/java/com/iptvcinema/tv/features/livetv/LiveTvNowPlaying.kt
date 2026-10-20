package com.iptvcinema.tv.features.livetv

import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram

/**
 * What is on now, kept apart from the channel list. The clock ticks every minute and the guide
 * reloads often; copying every channel each time made the whole list look new to Compose.
 */
internal object LiveTvNowPlaying {
    fun currentProgramsByChannel(programs: List<EpgProgram>, nowMs: Long): Map<String, EpgProgram> =
        programs
            .filter { program -> program.startEpochMs <= nowMs && program.endEpochMs > nowMs }
            .associateBy { program -> program.channelId }

    /** [channel] with its current programme filled in, or [channel] itself when nothing is on. */
    fun withCurrentProgram(channel: ChannelItem, currentPrograms: Map<String, EpgProgram>, nowMs: Long): ChannelItem =
        currentPrograms[channel.id]?.let { program ->
            GuideLayoutHelper.channelItemWithProgram(channel, program, nowMs)
        } ?: channel
}
