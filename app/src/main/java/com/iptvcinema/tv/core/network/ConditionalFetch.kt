package com.iptvcinema.tv.core.network

data class HttpValidators(
    val etag: String? = null,
    val lastModified: String? = null,
) {
    fun mergedWith(fallback: HttpValidators): HttpValidators = HttpValidators(
        etag = etag ?: fallback.etag,
        lastModified = lastModified ?: fallback.lastModified,
    )
}

sealed interface ConditionalFetchResult<out T> {
    val validators: HttpValidators

    data class Modified<T>(
        val body: T,
        override val validators: HttpValidators,
    ) : ConditionalFetchResult<T>

    data class NotModified(
        override val validators: HttpValidators,
    ) : ConditionalFetchResult<Nothing>
}
