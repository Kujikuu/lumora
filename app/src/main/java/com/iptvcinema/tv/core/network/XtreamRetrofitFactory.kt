package com.iptvcinema.tv.core.network

import com.iptvcinema.tv.core.xtream.XtreamApi
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Singleton
class XtreamRetrofitFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    fun create(baseUrl: String): XtreamApi {
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedBase)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(XtreamApi::class.java)
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
