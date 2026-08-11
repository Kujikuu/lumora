package com.iptvcinema.tv.core.m3u

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uRequestValidationTest {
    @Test
    fun hostlessHttpUri_isRejected() {
        assertFalse(isValidM3uHttpUrl("http:playlist"))
        assertFalse(isValidM3uHttpUrl("https:/playlist"))
        assertTrue(isValidM3uHttpUrl("https://provider.example/playlist.m3u"))
    }

    @Test
    fun invalidCustomHeader_isMappedToPermanentDownloadFailure() = runBlocking {
        val error = runCatching {
            M3uDownloader(OkHttpClient()).downloadConditional(
                url = "https://provider.example/playlist.m3u",
                options = M3uRequestOptions(customHeaders = mapOf("Invalid Header" to "value")),
            )
        }.exceptionOrNull()

        assertTrue(error is M3uDownloadException.InvalidRequest)
    }
}
