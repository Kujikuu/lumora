package com.iptvcinema.tv.features.details

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.features.home.episode
import com.iptvcinema.tv.features.home.history
import com.iptvcinema.tv.features.home.series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesEpisodesBuilderTest {
    private val labels = EpisodeLabels(
        seasonTitle = { season, count -> "Season $season, $count episodes" },
        episodeTitle = { number -> "Episode $number" },
        watched = "Watched",
    )

    private val episodes = listOf(
        episode("s2e1", "s", season = 2, number = 1),
        episode("s1e2", "s", season = 1, number = 2),
        episode("s1e1", "s", season = 1, number = 1),
    )

    private fun watched(id: String, at: Long, position: Long) =
        history(id, WatchHistoryContentType.EPISODE, watchedAtSeconds = at, positionMs = position, durationMs = 100_000, seriesId = "s")

    @Test
    fun `one rail per season in order, episodes in order`() {
        val content = SeriesEpisodesBuilder.build(series("s"), episodes, emptyList(), labels)
        assertEquals(listOf("season:1", "season:2"), content.sections.map { it.id })
        assertEquals(listOf("s1e1", "s1e2"), content.sections[0].items.map { it.contentId })
        assertEquals("Season 1, 2 episodes", content.sections[0].title.text)
    }

    @Test
    fun `nothing watched starts at the beginning`() {
        val content = SeriesEpisodesBuilder.build(series("s"), episodes, emptyList(), labels)
        assertNull(content.resumeFocus)
    }

    @Test
    fun `an episode in progress is resumed`() {
        val content = SeriesEpisodesBuilder.build(series("s"), episodes, listOf(watched("s1e2", at = 10, position = 30_000)), labels)
        assertEquals("season:1" to 1, content.resumeFocus)
    }

    @Test
    fun `a finished episode moves on to the next one, across seasons`() {
        val history = listOf(watched("s1e1", at = 5, position = 99_000), watched("s1e2", at = 10, position = 99_000))
        val content = SeriesEpisodesBuilder.build(series("s"), episodes, history, labels)
        assertEquals("s2e1", content.resumeEpisodeId)
        assertEquals("season:2" to 0, content.resumeFocus)
    }

    @Test
    fun `finished episodes show a full bar and say watched`() {
        val content = SeriesEpisodesBuilder.build(series("s"), episodes, listOf(watched("s1e1", at = 5, position = 96_000)), labels)
        val card = content.sections[0].items.first { it.contentId == "s1e1" }
        assertEquals(1f, card.progress)
        assertTrue(card.subtitle!!.endsWith("Watched"))
        assertTrue(card.subtitle!!.startsWith("S1E1"))
    }

    @Test
    fun `history of other titles is ignored`() {
        val other = history("m1", WatchHistoryContentType.MOVIE, watchedAtSeconds = 50)
        val content = SeriesEpisodesBuilder.build(series("s"), episodes, listOf(other), labels)
        assertNull(content.resumeEpisodeId)
        assertTrue(content.sections.flatMap { it.items }.all { it.progress == null })
    }

    @Test
    fun `an episode listed twice shows once`() {
        val content = SeriesEpisodesBuilder.build(series("s"), episodes + episode("s1e1", "s"), emptyList(), labels)
        assertEquals(listOf("s1e1", "s1e2"), content.sections[0].items.map { it.contentId })
    }
}
