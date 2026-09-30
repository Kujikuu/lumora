package com.iptvcinema.tv.core.tvhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackDeepLinkTest {
    @Test
    fun roundTrip_keepsEveryFieldIncludingReservedCharacters() {
        val link = PlaybackDeepLink(
            contentType = "episode",
            contentId = "ep 1&2",
            seriesId = "series/9",
            profileId = "profile-1",
        )

        assertEquals(link, PlaybackDeepLink.parse(link.toUri()))
    }

    @Test
    fun toUri_omitsMissingOptionalFields() {
        assertEquals(
            "lumora://play?type=movie&id=42",
            PlaybackDeepLink(contentType = "movie", contentId = "42").toUri(),
        )
    }

    @Test
    fun parse_rejectsForeignOrIncompleteLinks() {
        assertNull(PlaybackDeepLink.parse(null))
        assertNull(PlaybackDeepLink.parse("https://play?type=movie&id=1"))
        assertNull(PlaybackDeepLink.parse("lumora://other?type=movie&id=1"))
        assertNull(PlaybackDeepLink.parse("lumora://play?type=live&id=1"))
        assertNull(PlaybackDeepLink.parse("lumora://play?type=movie"))
        assertNull(PlaybackDeepLink.parse("not a uri"))
    }
}
