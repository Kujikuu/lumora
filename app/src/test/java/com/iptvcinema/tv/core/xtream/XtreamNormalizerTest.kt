package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.model.XtreamCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

class XtreamNormalizerTest {
    private val credentials = XtreamCredentials(
        serverUrl = "http://example.com",
        username = "user",
        password = "pass",
        accountName = "Test Account",
    )

    @Test
    fun normalizeCatalog_namespacesProviderCategoryIdsByContentType() {
        val providerCategory = XtreamCategoryDto(
            categoryId = JsonPrimitive("1"),
            categoryName = "Featured",
        )
        val liveCategory = XtreamNormalizer.normalizeLiveCategories("src1", listOf(providerCategory)).first.single()
        val vodCategory = XtreamNormalizer.normalizeVodCategories("src1", listOf(providerCategory)).single()
        val seriesCategory = XtreamNormalizer.normalizeSeriesCategories("src1", listOf(providerCategory)).single()

        assertNotEquals(liveCategory.id, vodCategory.id)
        assertNotEquals(liveCategory.id, seriesCategory.id)
        assertNotEquals(vodCategory.id, seriesCategory.id)

        val channel = XtreamNormalizer.normalizeLiveStreams(
            sourceId = "src1",
            credentials = credentials,
            serverUrl = credentials.serverUrl,
            dtos = listOf(
                XtreamLiveStreamDto(
                    streamId = JsonPrimitive("101"),
                    categoryId = JsonPrimitive("1"),
                    name = "Live",
                ),
            ),
            categoryNames = mapOf(liveCategory.id to liveCategory.name),
        ).single()
        val movie = XtreamNormalizer.normalizeVodStreams(
            sourceId = "src1",
            credentials = credentials,
            serverUrl = credentials.serverUrl,
            dtos = listOf(
                XtreamVodStreamDto(
                    streamId = JsonPrimitive("201"),
                    categoryId = JsonPrimitive("1"),
                    name = "Movie",
                ),
            ),
            categoryNames = mapOf(vodCategory.id to vodCategory.name),
        ).single()
        val series = XtreamNormalizer.normalizeSeries(
            sourceId = "src1",
            dtos = listOf(
                XtreamSeriesDto(
                    seriesId = JsonPrimitive("301"),
                    categoryId = JsonPrimitive("1"),
                    name = "Series",
                ),
            ),
            categoryNames = mapOf(seriesCategory.id to seriesCategory.name),
        ).single()

        assertEquals(liveCategory.id, channel.categoryId)
        assertEquals(vodCategory.id, movie.categoryId)
        assertEquals(seriesCategory.id, series.categoryId)
        assertEquals("Featured", channel.categoryName)
        assertEquals("Featured", movie.categoryName)
        assertEquals("Featured", series.categoryName)
    }

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

    @Test
    fun archiveDays_readsTvArchiveDurationOnlyWhenArchiveIsEnabled() {
        fun dto(archive: String?, duration: String?) = XtreamLiveStreamDto(
            streamId = JsonPrimitive("1"),
            tvArchive = archive?.let(::JsonPrimitive),
            tvArchiveDuration = duration?.let(::JsonPrimitive),
        )

        assertEquals(3, XtreamNormalizer.archiveDays(dto(archive = "1", duration = "3")))
        assertEquals(7, XtreamNormalizer.archiveDays(XtreamLiveStreamDto(
            streamId = JsonPrimitive("1"),
            tvArchive = JsonPrimitive(1),
            tvArchiveDuration = JsonPrimitive(7),
        )))
        assertEquals(0, XtreamNormalizer.archiveDays(dto(archive = "0", duration = "3")))
        assertEquals(0, XtreamNormalizer.archiveDays(dto(archive = "1", duration = null)))
        assertEquals(0, XtreamNormalizer.archiveDays(dto(archive = null, duration = null)))
    }
}
