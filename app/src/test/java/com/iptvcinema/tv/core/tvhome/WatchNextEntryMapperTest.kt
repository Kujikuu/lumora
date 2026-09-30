package com.iptvcinema.tv.core.tvhome

import com.iptvcinema.tv.core.data.repository.WatchHistoryCardDisplay
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchNextEntryMapperTest {
    private val display = WatchHistoryCardDisplay(
        title = "Show",
        subtitle = "S1 E2",
        posterUrl = "http://img/poster.jpg",
        backdropUrl = "http://img/backdrop.jpg",
    )

    @Test
    fun movieInProgress_isContinueWithPositionAndMovieKey() {
        val entry = WatchNextEntryMapper.from(
            item = item(id = "h1", contentId = "m1", type = WatchHistoryContentType.MOVIE, positionMs = 60_000),
            display = display,
            episode = null,
            profileId = "p1",
        )!!

        assertEquals("movie:m1", entry.key)
        assertEquals(WatchNextKind.Continue, entry.kind)
        assertEquals(WatchNextProgramKind.Movie, entry.programKind)
        assertEquals(60_000L, entry.positionMs)
        assertEquals(3_600_000L, entry.durationMs)
        assertEquals("http://img/backdrop.jpg", entry.posterUrl)
        assertEquals(
            PlaybackDeepLink(contentType = "movie", contentId = "m1", profileId = "p1"),
            PlaybackDeepLink.parse(entry.intentUri),
        )
    }

    @Test
    fun nextUpEpisode_isNextKeyedBySeries() {
        val entry = WatchNextEntryMapper.from(
            item = item(
                id = "next-up:s1:e3",
                contentId = "e3",
                type = WatchHistoryContentType.EPISODE,
                seriesId = "s1",
                positionMs = 0,
            ),
            display = display,
            episode = EpisodeNumbers(season = 1, episode = 3, title = "Third"),
            profileId = "p1",
        )!!

        assertEquals("series:s1", entry.key)
        assertEquals(WatchNextKind.Next, entry.kind)
        assertEquals(WatchNextProgramKind.Episode, entry.programKind)
        assertEquals(1, entry.seasonNumber)
        assertEquals(3, entry.episodeNumber)
        assertEquals("Third", entry.episodeTitle)
        assertEquals(0L, entry.positionMs)
        assertEquals(WatchNextKeys.Removed.Series("s1"), WatchNextKeys.parse(entry.key))
    }

    @Test
    fun channels_areNotPublished() {
        assertNull(
            WatchNextEntryMapper.from(
                item = item(id = "h", contentId = "c", type = WatchHistoryContentType.CHANNEL, positionMs = 0),
                display = display,
                episode = null,
                profileId = "p1",
            ),
        )
    }

    @Test
    fun keys_parseBackToHistoryItems() {
        assertEquals(
            WatchNextKeys.Removed.Item("m1", WatchHistoryContentType.MOVIE),
            WatchNextKeys.parse("movie:m1"),
        )
        assertEquals(
            WatchNextKeys.Removed.Item("e1", WatchHistoryContentType.EPISODE),
            WatchNextKeys.parse("episode:e1"),
        )
        assertNull(WatchNextKeys.parse("channel:1"))
        assertNull(WatchNextKeys.parse("movie:"))
    }

    private fun item(
        id: String,
        contentId: String,
        type: WatchHistoryContentType,
        seriesId: String? = null,
        positionMs: Long,
    ) = WatchHistoryItem(
        id = id,
        profileId = "p1",
        sourceId = "src",
        contentId = contentId,
        contentType = type,
        seriesId = seriesId,
        title = "t",
        posterUrl = null,
        positionMs = positionMs,
        durationMs = 3_600_000L,
        lastWatchedAt = Instant.ofEpochMilli(1_000L),
    )
}
