package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.network.XtreamRetrofitFactory
import com.iptvcinema.tv.core.network.ConditionalFetchResult
import com.iptvcinema.tv.core.network.HttpValidators
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response

sealed class XtreamAuthResult {
    data class Success(val response: XtreamAuthResponse) : XtreamAuthResult()
    data class InvalidCredentials(val message: String) : XtreamAuthResult()
    data class Expired(val message: String) : XtreamAuthResult()
    data class Unreachable(val message: String, val retryable: Boolean = true) : XtreamAuthResult()
    data class Error(val message: String) : XtreamAuthResult()
}

@Singleton
class XtreamRepository @Inject constructor(
    private val retrofitFactory: XtreamRetrofitFactory,
    private val localCredentialsStore: LocalCredentialsStore,
) {
    fun getCredentials(sourceId: String): XtreamCredentials? =
        localCredentialsStore.getXtreamCredentials(sourceId)

    suspend fun validateAndAuthenticate(credentials: XtreamCredentials): XtreamAuthResult {
        val serverUrl = XtreamUrlNormalizer.normalize(credentials.serverUrl).getOrElse { error ->
            return XtreamAuthResult.Error(error.message ?: "Invalid server URL")
        }
        return runCatching {
            val api = retrofitFactory.create(serverUrl)
            val response = api.authenticate(
                username = credentials.username,
                password = credentials.password,
            )
            when {
                response.userInfo?.status?.equals("Expired", ignoreCase = true) == true ->
                    XtreamAuthResult.Expired("IPTV account expired")
                response.userInfo?.isAuthenticated() != true ->
                    XtreamAuthResult.InvalidCredentials(
                        response.userInfo?.message ?: "Invalid username or password",
                    )
                else -> XtreamAuthResult.Success(response)
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            mapThrowable(error)
        }
    }

    suspend fun fetchLiveCategories(
        credentials: XtreamCredentials,
        validators: HttpValidators = HttpValidators(),
    ): ConditionalFetchResult<List<XtreamCategoryDto>> = withApi(credentials) {
        it.getLiveCategories(
            credentials.username,
            credentials.password,
            ifNoneMatch = validators.etag,
            ifModifiedSince = validators.lastModified,
        ).toConditionalResult(validators)
    }

    suspend fun fetchLiveStreams(
        credentials: XtreamCredentials,
        validators: HttpValidators = HttpValidators(),
    ): ConditionalFetchResult<List<XtreamLiveStreamDto>> = withApi(credentials) {
        it.getLiveStreams(
            credentials.username,
            credentials.password,
            ifNoneMatch = validators.etag,
            ifModifiedSince = validators.lastModified,
        ).toConditionalResult(validators)
    }

    suspend fun fetchVodCategories(
        credentials: XtreamCredentials,
        validators: HttpValidators = HttpValidators(),
    ): ConditionalFetchResult<List<XtreamCategoryDto>> = withApi(credentials) {
        it.getVodCategories(
            credentials.username,
            credentials.password,
            ifNoneMatch = validators.etag,
            ifModifiedSince = validators.lastModified,
        ).toConditionalResult(validators)
    }

    suspend fun fetchVodStreams(
        credentials: XtreamCredentials,
        validators: HttpValidators = HttpValidators(),
    ): ConditionalFetchResult<List<XtreamVodStreamDto>> = withApi(credentials) {
        it.getVodStreams(
            credentials.username,
            credentials.password,
            ifNoneMatch = validators.etag,
            ifModifiedSince = validators.lastModified,
        ).toConditionalResult(validators)
    }

    suspend fun fetchSeriesCategories(
        credentials: XtreamCredentials,
        validators: HttpValidators = HttpValidators(),
    ): ConditionalFetchResult<List<XtreamCategoryDto>> = withApi(credentials) {
        it.getSeriesCategories(
            credentials.username,
            credentials.password,
            ifNoneMatch = validators.etag,
            ifModifiedSince = validators.lastModified,
        ).toConditionalResult(validators)
    }

    suspend fun fetchSeries(
        credentials: XtreamCredentials,
        validators: HttpValidators = HttpValidators(),
    ): ConditionalFetchResult<List<XtreamSeriesDto>> = withApi(credentials) {
        it.getSeries(
            credentials.username,
            credentials.password,
            ifNoneMatch = validators.etag,
            ifModifiedSince = validators.lastModified,
        ).toConditionalResult(validators)
    }

    suspend fun fetchSeriesInfo(credentials: XtreamCredentials, seriesId: String): XtreamSeriesInfoResponse =
        withApi(credentials) {
            it.getSeriesInfo(
                username = credentials.username,
                password = credentials.password,
                seriesId = seriesId,
            )
        }

    suspend fun fetchXmltv(credentials: XtreamCredentials): String {
        val serverUrl = normalizedServer(credentials)
        val api = retrofitFactory.create(serverUrl)
        return api.getXmltv(credentials.username, credentials.password).string()
    }

    fun normalizedServer(credentials: XtreamCredentials): String =
        XtreamUrlNormalizer.normalize(credentials.serverUrl).getOrThrow()

    private suspend fun <T> withApi(
        credentials: XtreamCredentials,
        block: suspend (XtreamApi) -> T,
    ): T {
        val serverUrl = normalizedServer(credentials)
        val api = retrofitFactory.create(serverUrl)
        return block(api)
    }

    private fun mapThrowable(error: Throwable): XtreamAuthResult = when (error) {
        is HttpException -> when (error.code()) {
            401, 403 -> XtreamAuthResult.InvalidCredentials("Invalid username or password")
            else -> XtreamAuthResult.Unreachable(
                message = "Server returned HTTP ${error.code()}",
                retryable = error.code() == 408 || error.code() == 429 || error.code() >= 500,
            )
        }
        is IOException -> XtreamAuthResult.Unreachable(
            "Unable to reach server. Check the URL, network connection, and that the provider is online.",
        )
        is IllegalArgumentException -> XtreamAuthResult.Error(error.message ?: "Invalid request")
        else -> XtreamAuthResult.Error(error.message ?: "Connection failed")
    }

    fun isRetryable(error: Throwable): Boolean = when (error) {
        is IOException -> true
        is HttpException -> error.code() == 408 || error.code() == 429 || error.code() >= 500
        else -> false
    }

    private fun <T> Response<T>.toConditionalResult(
        previous: HttpValidators,
    ): ConditionalFetchResult<T> {
        val current = HttpValidators(
            etag = headers()["ETag"],
            lastModified = headers()["Last-Modified"],
        )
        if (code() == 304) {
            return ConditionalFetchResult.NotModified(current.mergedWith(previous))
        }
        if (!isSuccessful) throw HttpException(this)
        val responseBody = body() ?: throw IOException("Provider returned an empty catalog response")
        return ConditionalFetchResult.Modified(responseBody, current)
    }

    fun authResultToStatus(result: XtreamAuthResult): SourceStatus = when (result) {
        is XtreamAuthResult.Success -> SourceStatus.ACTIVE
        is XtreamAuthResult.Expired -> SourceStatus.EXPIRED
        is XtreamAuthResult.InvalidCredentials -> SourceStatus.FAILED
        is XtreamAuthResult.Unreachable -> SourceStatus.NEEDS_ATTENTION
        is XtreamAuthResult.Error -> SourceStatus.FAILED
    }
}
