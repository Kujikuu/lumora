package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.model.XtreamCredentials
import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamNormalizerTest {
    private val credentials = XtreamCredentials(
        serverUrl = "http://example.com",
        username = "user",
        password = "pass",
        accountName = "Test Account",
    )

    @Test
    fun normalizeSeriesInfo_usesSeasonMapKeyWhenEpisodeSeasonMissing() {
        val response = XtreamSeriesInfoResponse(
            episodes = mapOf(
                "2" to listOf(
                    XtreamEpisodeDto(
                        id = kotlinx.serialization.json.JsonPrimitive("101"),
                        episodeNum = kotlinx.serialization.json.JsonPrimitive(1),
                        title = "S2E1",
                        season = null,
                    ),
                ),
                "3" to listOf(
                    XtreamEpisodeDto(
                        id = kotlinx.serialization.json.JsonPrimitive("102"),
                        episodeNum = kotlinx.serialization.json.JsonPrimitive(2),
                        title = "S3E2",
                        season = null,
                    ),
                ),
            ),
        )

        val episodes = XtreamNormalizer.normalizeSeriesInfo(
            sourceId = "src1",
            seriesId = "series1",
            credentials = credentials,
            serverUrl = "http://example.com",
            response = response,
        )

        assertEquals(listOf(2, 3), episodes.map { it.seasonNumber })
        assertEquals(listOf(1, 2), episodes.map { it.episodeNumber })
    }

    @Test
    fun normalizeSeriesInfo_prefersEpisodeSeasonOverMapKey() {
        val response = XtreamSeriesInfoResponse(
            episodes = mapOf(
                "1" to listOf(
                    XtreamEpisodeDto(
                        id = kotlinx.serialization.json.JsonPrimitive("201"),
                        episodeNum = kotlinx.serialization.json.JsonPrimitive(5),
                        title = "Actual Season 4",
                        season = kotlinx.serialization.json.JsonPrimitive(4),
                    ),
                ),
            ),
        )

        val episodes = XtreamNormalizer.normalizeSeriesInfo(
            sourceId = "src1",
            seriesId = "series1",
            credentials = credentials,
            serverUrl = "http://example.com",
            response = response,
        )

        assertEquals(4, episodes.single().seasonNumber)
        assertEquals(5, episodes.single().episodeNumber)
    }

    @Test
    fun seasonNumbersFromSeriesInfo_mergesSeasonMetadataAndEpisodes() {
        val response = XtreamSeriesInfoResponse(
            seasons = listOf(
                XtreamSeasonDto(seasonNumber = kotlinx.serialization.json.JsonPrimitive(1)),
                XtreamSeasonDto(seasonNumber = kotlinx.serialization.json.JsonPrimitive(2)),
            ),
            episodes = mapOf(
                "3" to listOf(
                    XtreamEpisodeDto(
                        id = kotlinx.serialization.json.JsonPrimitive("301"),
                        episodeNum = kotlinx.serialization.json.JsonPrimitive(1),
                        title = "S3E1",
                        season = null,
                    ),
                ),
            ),
        )

        assertEquals(listOf(1, 2, 3), XtreamNormalizer.seasonNumbersFromSeriesInfo(response))
    }
}
