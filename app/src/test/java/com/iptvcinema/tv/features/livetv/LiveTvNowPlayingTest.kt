package com.iptvcinema.tv.features.livetv

import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTvNowPlayingTest {
    private val now = 1_000_000L

    @Test
    fun currentProgramsByChannel_keepsOnlyWhatIsOnNow() {
        val onNow = program("a", start = now - 10, end = now + 10)
        val later = program("a", start = now + 10, end = now + 20, id = "later")
        val ended = program("b", start = now - 20, end = now)

        val result = LiveTvNowPlaying.currentProgramsByChannel(listOf(onNow, later, ended), now)

        assertEquals(mapOf("a" to onNow), result)
    }

    @Test
    fun withCurrentProgram_fillsTheProgramFields() {
        val channel = channel("a")
        val onNow = program("a", start = now - 50, end = now + 50)

        val result = LiveTvNowPlaying.withCurrentProgram(channel, mapOf("a" to onNow), now)

        assertEquals("Show a", result.currentProgram)
        assertEquals(0.5f, result.programProgress, 0.001f)
    }

    @Test
    fun withCurrentProgram_returnsTheSameItemWhenNothingIsOn() {
        val channel = channel("a")

        assertSame(channel, LiveTvNowPlaying.withCurrentProgram(channel, emptyMap(), now))
    }

    @Test
    fun previewChannel_usesTheFirstChannelsCurrentProgramWhenNothingIsSelected() {
        val channels = listOf(channel("a"), channel("b"))
        val state = LiveTvUiState(
            loadState = com.iptvcinema.tv.core.data.repository.CatalogLoadState.Ready,
            channels = channels,
            currentPrograms = mapOf("a" to program("a", start = now - 10, end = now + 10)),
            nowMs = now,
        )

        assertEquals("Show a", state.previewChannel?.currentProgram)
        // The list itself keeps the plain items, so a clock tick does not replace every row.
        assertSame(channels[0], state.channels[0])
        assertTrue(state.channels[0].currentProgram == "No info")
    }

    private fun channel(id: String) = ChannelItem(
        id = id,
        name = "Channel $id",
        category = "News",
        currentProgram = "No info",
        programStart = "",
        programEnd = "",
        programProgress = 0f,
    )

    private fun program(channelId: String, start: Long, end: Long, id: String = "p-$channelId") = EpgProgram(
        id = id,
        channelId = channelId,
        title = "Show $channelId",
        startHour = 0,
        startMinute = 0,
        durationMinutes = 1,
        startEpochMs = start,
        endEpochMs = end,
    )
}
