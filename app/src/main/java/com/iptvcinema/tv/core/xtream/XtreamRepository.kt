package com.iptvcinema.tv.core.xtream

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.network.XtreamRetrofitFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

sealed class XtreamAuthResult {
    data class Success(val response: XtreamAuthResponse) : XtreamAuthResult()
    data class InvalidCredentials(val message: String) : XtreamAuthResult()
    data class Expired(val message: String) : XtreamAuthResult()
    data class Unreachable(val message: String) : XtreamAuthResult()
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
            mapThrowable(error)
        }
    }

    suspend fun fetchLiveCategories(credentials: XtreamCredentials): List<XtreamCategoryDto> =
        withApi(credentials) { it.getLiveCategories(credentials.username, credentials.password) }

    suspend fun fetchLiveStreams(credentials: XtreamCredentials): List<XtreamLiveStreamDto> =
        withApi(credentials) { it.getLiveStreams(credentials.username, credentials.password) }

    suspend fun fetchVodCategories(credentials: XtreamCredentials): List<XtreamCategoryDto> =
        withApi(credentials) { it.getVodCategories(credentials.username, credentials.password) }

    suspend fun fetchVodStreams(credentials: XtreamCredentials): List<XtreamVodStreamDto> =
        withApi(credentials) { it.getVodStreams(credentials.username, credentials.password) }

    suspend fun fetchSeriesCategories(credentials: XtreamCredentials): List<XtreamCategoryDto> =
        withApi(credentials) { it.getSeriesCategories(credentials.username, credentials.password) }

    suspend fun fetchSeries(credentials: XtreamCredentials): List<XtreamSeriesDto> =
        withApi(credentials) { it.getSeries(credentials.username, credentials.password) }

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
            else -> XtreamAuthResult.Unreachable("Server returned HTTP ${error.code()}")
        }
        is IOException -> XtreamAuthResult.Unreachable(
            "Unable to reach server. Check the URL, network connection, and that the provider is online.",
        )
        is IllegalArgumentException -> XtreamAuthResult.Error(error.message ?: "Invalid request")
        else -> XtreamAuthResult.Error(error.message ?: "Connection failed")
    }

    fun authResultToStatus(result: XtreamAuthResult): SourceStatus = when (result) {
        is XtreamAuthResult.Success -> SourceStatus.ACTIVE
        is XtreamAuthResult.Expired -> SourceStatus.EXPIRED
        is XtreamAuthResult.InvalidCredentials -> SourceStatus.FAILED
        is XtreamAuthResult.Unreachable -> SourceStatus.NEEDS_ATTENTION
        is XtreamAuthResult.Error -> SourceStatus.FAILED
    }
}
