package com.iptvcinema.tv.core.network

import com.iptvcinema.tv.core.m3u.M3uDownloader
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionalFetchTest {
    @Test
    fun responseValidators_overrideOnlyHeadersProviderReturned() {
        val previous = HttpValidators(etag = "old-etag", lastModified = "old-date")
        val response = HttpValidators(etag = "new-etag")

        assertEquals(
            HttpValidators(etag = "new-etag", lastModified = "old-date"),
            response.mergedWith(previous),
        )
    }

    @Test
    fun modifiedResponse_withoutValidatorHeadersClearsPreviousValidators() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("#EXTM3U\n".toResponseBody("audio/x-mpegurl".toMediaType()))
                    .build()
            }
            .build()

        val result = M3uDownloader(client).downloadConditional(
            url = "https://example.test/catalog.m3u",
            validators = HttpValidators(etag = "old-etag", lastModified = "old-date"),
        )

        assertTrue(result is ConditionalFetchResult.Modified)
        assertEquals(HttpValidators(), result.validators)
    }
}
