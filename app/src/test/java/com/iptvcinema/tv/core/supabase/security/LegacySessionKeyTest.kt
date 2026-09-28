package com.iptvcinema.tv.core.supabase.security

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacySessionKeyTest {
    @Test
    fun matchesSupabaseDefaultKey() {
        assertEquals(
            "sb-https:--abc-supabase-co-session",
            EncryptedSessionManager.legacySessionKey("https://abc.supabase.co/"),
        )
    }
}
