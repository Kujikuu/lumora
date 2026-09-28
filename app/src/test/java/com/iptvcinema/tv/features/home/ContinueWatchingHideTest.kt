package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.util.continueWatchingKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueWatchingHideTest {
    private val episode = HomeContentCard(contentId = "ep-2", contentType = "episode", seriesId = "s-1", title = "S1E2")
    private val movie = HomeContentCard(contentId = "m-1", contentType = "movie", title = "Movie")

    @Test
    fun episodeKey_isTheSeries_soOtherEpisodesHideToo() {
        val otherEpisode = episode.copy(contentId = "ep-1")
        assertEquals(episode.continueWatchingKey(), otherEpisode.continueWatchingKey())
    }

    @Test
    fun hidingRemovesCardAndDropsEmptyRails() {
        val state = HomeUiState(
            sections = listOf(
                HomeSection.ContinueWatching(listOf(episode, movie)),
                HomeSection.NextEpisode(listOf(episode.copy(contentId = "ep-3"))),
            ),
        )

        val result = state.withoutContinueWatching(setOf(episode.continueWatchingKey()))

        assertEquals(1, result.sections.size)
        assertEquals(listOf(movie), result.sections.single().items)
    }

    @Test
    fun nothingHidden_returnsSameState() {
        val state = HomeUiState(sections = listOf(HomeSection.ContinueWatching(listOf(movie))))
        assertTrue(state.withoutContinueWatching(emptySet()) === state)
    }
}
