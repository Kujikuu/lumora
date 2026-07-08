package com.iptvcinema.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackPreferencesApplierTest {
    private val audioTracks = listOf(
        MediaTrackOption(index = 0, label = "French", language = "fr"),
        MediaTrackOption(index = 1, label = "English", language = "en"),
    )

    private val subtitleTracks = listOf(
        MediaTrackOption(index = 0, label = "Arabic", language = "ar"),
        MediaTrackOption(index = 1, label = "English", language = "en"),
    )

    @Test
    fun selectAudioTrackIndex_prefersExactLanguageMatch() {
        val selected = PlaybackPreferencesApplier.selectAudioTrackIndex(audioTracks, "en")
        assertEquals(1, selected)
    }

    @Test
    fun selectAudioTrackIndex_fallsBackToFirstTrack() {
        val selected = PlaybackPreferencesApplier.selectAudioTrackIndex(audioTracks, "de")
        assertEquals(0, selected)
    }

    @Test
    fun selectSubtitleTrackIndex_returnsNullWhenDisabled() {
        val selected = PlaybackPreferencesApplier.selectSubtitleTrackIndex(
            tracks = subtitleTracks,
            preferredLanguage = "en",
            subtitlesEnabled = false,
        )
        assertNull(selected)
    }

    @Test
    fun selectSubtitleTrackIndex_prefersRequestedLanguage() {
        val selected = PlaybackPreferencesApplier.selectSubtitleTrackIndex(
            tracks = subtitleTracks,
            preferredLanguage = "ar",
            subtitlesEnabled = true,
        )
        assertEquals(0, selected)
    }

    @Test
    fun maxVideoHeight_mapsQualityTiers() {
        assertNull(PlaybackPreferencesApplier.maxVideoHeight("AUTO"))
        assertEquals(1080, PlaybackPreferencesApplier.maxVideoHeight("1080"))
        assertEquals(720, PlaybackPreferencesApplier.maxVideoHeight("720p"))
        assertEquals(480, PlaybackPreferencesApplier.maxVideoHeight("480"))
    }

    @Test
    fun streamingQualityOption_normalizesLegacyValues() {
        assertEquals("AUTO", StreamingQualityOption.normalize("auto"))
        assertEquals("1080", StreamingQualityOption.normalize("1080P"))
    }
}
