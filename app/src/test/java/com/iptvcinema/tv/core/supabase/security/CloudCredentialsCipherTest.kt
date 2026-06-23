package com.iptvcinema.tv.core.supabase.security

import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.supabase.dto.EncryptedCredentialsDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CloudCredentialsCipherTest {
    private val cipher = CloudCredentialsCipher(Json { ignoreUnknownKeys = true })
    private val userId = "user-123"

    @Test
    fun encryptXtream_roundTripsCredentials() {
        val original = XtreamCredentials(
            serverUrl = "http://example.com:8080",
            username = "demo",
            password = "secret",
            accountName = "Home IPTV",
        )

        val encrypted = cipher.encryptXtream(userId, original)
        val restored = cipher.decryptXtream(userId, encrypted)

        assertNotNull(restored)
        assertEquals(original, restored)
    }

    @Test
    fun encryptM3u_roundTripsCredentials() {
        val original = M3uCredentials(
            playlistUrl = "http://example.com/list.m3u",
            epgUrl = "http://example.com/epg.xml",
            playlistName = "Main Playlist",
            userAgent = "IPTV Cinema",
        )

        val encrypted = cipher.encryptM3u(userId, original)
        val restored = cipher.decryptM3u(userId, encrypted)

        assertNotNull(restored)
        assertEquals(original, restored)
    }

    @Test
    fun decryptXtream_rejectsWrongUserKey() {
        val encrypted = cipher.encryptXtream(userId, XtreamCredentials(
            serverUrl = "http://example.com:8080",
            username = "demo",
            password = "secret",
            accountName = "Home IPTV",
        ))

        assertNull(cipher.decryptXtream("other-user", encrypted))
    }

    @Test
    fun decryptXtream_rejectsM3uPayload() {
        val encrypted = cipher.encryptM3u(userId, M3uCredentials(
            playlistUrl = "http://example.com/list.m3u",
            epgUrl = null,
            playlistName = "Main Playlist",
            userAgent = null,
        ))

        assertNull(cipher.decryptXtream(userId, encrypted))
    }

    @Test
    fun encryptedPayload_matchesSupabasePostgresOffsetTimestampShape() {
        val encrypted = cipher.encryptXtream(userId, XtreamCredentials(
            serverUrl = "http://example.com:8080",
            username = "demo",
            password = "secret",
            accountName = "Home IPTV",
        ))

        assertEquals("XTREAM_CODES", encrypted.type)
        assertEquals(1, encrypted.v)
    }
}
