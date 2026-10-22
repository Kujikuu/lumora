package com.iptvcinema.tv.features.search

import com.iptvcinema.tv.core.design.components.SearchKeyboardLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceSearchTest {
    @Test
    fun languageTag_followsTheKeyboardLayout() {
        assertEquals("ar", VoiceSearch.languageTag(SearchKeyboardLayout.Arabic))
        assertEquals("en-US", VoiceSearch.languageTag(SearchKeyboardLayout.English))
    }

    @Test
    fun bestTranscript_skipsBlankCandidates() {
        assertEquals("the office", VoiceSearch.bestTranscript(listOf("  ", " the office ", "office")))
        assertNull(VoiceSearch.bestTranscript(listOf("", "   ")))
        assertNull(VoiceSearch.bestTranscript(null))
    }
}
