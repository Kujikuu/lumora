package com.iptvcinema.tv.features.player

import com.iptvcinema.tv.core.model.EpisodeItem
import com.iptvcinema.tv.core.model.SeasonItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodePickerLoadCoordinatorTest {
    @Test
    fun dismiss_preventsInFlightLoadFromPublishing() = runBlocking {
        withTimeout(5_000L) {
            val loadStarted = CompletableDeferred<Unit>()
            val releaseLoad = CompletableDeferred<Unit>()
            val published = mutableListOf<List<SeasonItem>>()
            val coordinator = EpisodePickerLoadCoordinator(this)

            val job = coordinator.launch(
                load = {
                    withContext(NonCancellable) {
                        loadStarted.complete(Unit)
                        releaseLoad.await()
                    }
                    seasons("obsolete")
                },
                publish = published::add,
            )
            loadStarted.await()

            coordinator.cancel()
            assertTrue(job.isCancelled)
            releaseLoad.complete(Unit)
            job.join()

            assertTrue(published.isEmpty())
        }
    }

    @Test
    fun rapidReopen_onlyPublishesNewestLoad() = runBlocking {
        withTimeout(5_000L) {
            val firstLoadStarted = CompletableDeferred<Unit>()
            val releaseFirstLoad = CompletableDeferred<Unit>()
            val publishedEpisodeIds = mutableListOf<String>()
            val coordinator = EpisodePickerLoadCoordinator(this)

            val firstJob = coordinator.launch(
                load = {
                    withContext(NonCancellable) {
                        firstLoadStarted.complete(Unit)
                        releaseFirstLoad.await()
                    }
                    seasons("obsolete")
                },
                publish = { result ->
                    publishedEpisodeIds += result.single().episodes.single().id
                },
            )
            firstLoadStarted.await()

            val secondJob = coordinator.launch(
                load = { seasons("fresh") },
                publish = { result ->
                    publishedEpisodeIds += result.single().episodes.single().id
                },
            )
            assertTrue(firstJob.isCancelled)
            secondJob.join()
            releaseFirstLoad.complete(Unit)
            firstJob.join()

            assertEquals(listOf("fresh"), publishedEpisodeIds)
        }
    }

    private fun seasons(episodeId: String) = listOf(
        SeasonItem(
            id = "series-a-s1",
            seasonNumber = 1,
            episodes = listOf(
                EpisodeItem(
                    id = episodeId,
                    episodeNumber = 1,
                    title = episodeId,
                    durationMinutes = 45,
                ),
            ),
        ),
    )
}
