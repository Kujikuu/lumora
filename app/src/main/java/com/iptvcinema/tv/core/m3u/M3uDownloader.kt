package com.iptvcinema.tv.core.m3u

import com.iptvcinema.tv.core.network.ConditionalFetchResult
import com.iptvcinema.tv.core.network.HttpValidators
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

data class M3uRequestOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
)

sealed class M3uDownloadException(message: String) : IOException(message) {
    class Unreachable(message: String) : M3uDownloadException(message)
    class HttpError(val code: Int, message: String) : M3uDownloadException(message)
    class EmptyResponse(message: String) : M3uDownloadException(message)
    class InvalidRequest(message: String) : M3uDownloadException(message)
}

internal fun isValidM3uHttpUrl(url: String): Boolean {
    val normalized = url.trim()
    if (!normalized.startsWith("http://", ignoreCase = true) &&
        !normalized.startsWith("https://", ignoreCase = true)
    ) {
        return false
    }
    return normalized.toHttpUrlOrNull()?.host?.isNotBlank() == true
}

@Singleton
class M3uDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun download(url: String, options: M3uRequestOptions = M3uRequestOptions()): String =
        when (val result = downloadConditional(url, options)) {
            is ConditionalFetchResult.Modified -> result.body
            is ConditionalFetchResult.NotModified -> throw M3uDownloadException.EmptyResponse(
                "Playlist returned not modified without cached content",
            )
        }

    suspend fun downloadConditional(
        url: String,
        options: M3uRequestOptions = M3uRequestOptions(),
        validators: HttpValidators = HttpValidators(),
    ): ConditionalFetchResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(url)
                options.userAgent?.takeIf { it.isNotBlank() }?.let {
                    requestBuilder.header("User-Agent", it)
                }
                options.referer?.takeIf { it.isNotBlank() }?.let {
                    requestBuilder.header("Referer", it)
                }
                options.customHeaders.forEach { (key, value) ->
                    if (key.isNotBlank() && value.isNotBlank() && !key.isConditionalRequestHeader()) {
                        requestBuilder.header(key, value)
                    }
                }
                validators.etag?.let { requestBuilder.header("If-None-Match", it) }
                validators.lastModified?.let { requestBuilder.header("If-Modified-Since", it) }

                okHttpClient.newCall(requestBuilder.build()).execute().use { httpResponse ->
                    val responseValidators = HttpValidators(
                        etag = httpResponse.header("ETag"),
                        lastModified = httpResponse.header("Last-Modified"),
                    )
                    if (httpResponse.code == 304) {
                        return@withContext ConditionalFetchResult.NotModified(
                            responseValidators.mergedWith(validators),
                        )
                    }
                    if (!httpResponse.isSuccessful) {
                        throw M3uDownloadException.HttpError(
                            code = httpResponse.code,
                            message = when (httpResponse.code) {
                                403 -> "Playlist access denied (403)"
                                404 -> "Playlist not found (404)"
                                else -> "Playlist download failed (${httpResponse.code})"
                            },
                        )
                    }
                    val body = httpResponse.body?.string()?.trim().orEmpty()
                    if (body.isBlank()) {
                        throw M3uDownloadException.EmptyResponse("Playlist is empty")
                    }
                    ConditionalFetchResult.Modified(body, responseValidators)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: M3uDownloadException) {
                throw error
            } catch (error: IllegalArgumentException) {
                throw M3uDownloadException.InvalidRequest(error.message ?: "Invalid playlist request")
            } catch (error: IOException) {
                throw M3uDownloadException.Unreachable(
                    error.message?.let { "Unable to reach playlist: $it" } ?: "Unable to reach playlist",
                )
            }
        }

    companion object {
        fun parseCustomHeaders(raw: String): Map<String, String> =
            raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val separator = line.indexOf(':')
                    if (separator <= 0) return@mapNotNull null
                    val key = line.substring(0, separator).trim()
                    val value = line.substring(separator + 1).trim()
                    if (key.isBlank() || value.isBlank()) null else key to value
                }
                .toMap()
    }

    private fun String.isConditionalRequestHeader(): Boolean =
        equals("If-None-Match", ignoreCase = true) || equals("If-Modified-Since", ignoreCase = true)
}
