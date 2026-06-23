package com.iptvcinema.tv.core.supabase.mapper

import com.iptvcinema.tv.core.model.ActivationSessionStatus
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.supabase.dto.DeviceActivationSessionDto
import com.iptvcinema.tv.core.supabase.dto.FavoriteDto
import com.iptvcinema.tv.core.supabase.dto.ProfileDto
import com.iptvcinema.tv.core.supabase.dto.UserSettingsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SupabaseMappersTest {

    @Test
    fun profileDto_mapsToUserProfile() {
        val dto = ProfileDto(
            id = "profile-1",
            userId = "user-1",
            name = "Main",
            type = "MAIN",
        )

        val profile = dto.toUserProfile()

        assertEquals("profile-1", profile.id)
        assertEquals("Main", profile.name)
        assertEquals(ProfileType.MAIN, profile.type)
        assertEquals("M", profile.avatarInitial)
    }

    @Test
    fun deviceActivationSessionDto_mapsStatusAndInstant() {
        val dto = DeviceActivationSessionDto(
            id = "session-1",
            code = "ABCD-12",
            qrToken = "token",
            status = "APPROVED",
            userId = "user-1",
            expiresAt = "2026-06-23T12:00:00Z",
        )

        val session = dto.toDomain()

        assertEquals(ActivationSessionStatus.APPROVED, session.status)
        assertEquals("user-1", session.userId)
    }

    @Test
    fun deviceActivationSessionDto_parsesPostgresOffsetTimestamp() {
        val dto = DeviceActivationSessionDto(
            id = "session-2",
            code = "WXYZ-99",
            qrToken = "token",
            status = "PENDING",
            expiresAt = "2026-06-23T21:32:48.288696+00:00",
        )

        val session = dto.toDomain()

        assertEquals(ActivationSessionStatus.PENDING, session.status)
        assertEquals("WXYZ-99", session.code)
    }

    @Test
    fun favoriteDto_mapsContentType() {
        val dto = FavoriteDto(
            id = "fav-1",
            userId = "user-1",
            profileId = "profile-1",
            contentId = "movie-1",
            contentType = "MOVIE",
            title = "Sample Movie",
        )

        val favorite = dto.toDomain()

        assertEquals(FavoriteContentType.MOVIE, favorite.contentType)
        assertEquals("Sample Movie", favorite.title)
    }

    @Test
    fun userSettings_roundTripPreservesValues() {
        val original = UserSettingsDto(
            id = "settings-1",
            userId = "user-1",
            autoplayNextEpisode = false,
            continueWatchingEnabled = true,
        ).toDomain()

        val dto = original.toDto(userId = "user-1")

        assertFalse(dto.autoplayNextEpisode)
        assertEquals("settings-1", dto.id)
    }

    @Test
    fun playlistSourceDto_mapsSourceTypeAndStatus() {
        val dto = com.iptvcinema.tv.core.supabase.dto.PlaylistSourceDto(
            id = "source-1",
            userId = "user-1",
            name = "Demo",
            type = "DEMO",
            status = "ACTIVE",
        )

        val source = dto.toDomain()

        assertEquals(SourceType.DEMO, source.type)
        assertEquals(SourceStatus.ACTIVE, source.status)
    }
}
