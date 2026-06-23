package com.iptvcinema.tv.core.m3u

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
}

@Singleton
class M3uDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun download(url: String, options: M3uRequestOptions = M3uRequestOptions()): String =
        withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder().url(url)
            options.userAgent?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.header("User-Agent", it)
            }
            options.referer?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.header("Referer", it)
            }
            options.customHeaders.forEach { (key, value) ->
                if (key.isNotBlank() && value.isNotBlank()) {
                    requestBuilder.header(key, value)
                }
            }

            val response = runCatching {
                okHttpClient.newCall(requestBuilder.build()).execute()
            }.getOrElse { error ->
                throw M3uDownloadException.Unreachable(
                    error.message?.let { "Unable to reach playlist: $it" } ?: "Unable to reach playlist",
                )
            }

            response.use { httpResponse ->
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
                body
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
}
