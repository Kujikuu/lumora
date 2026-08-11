package com.iptvcinema.tv.core.xtream

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.Response

interface XtreamApi {
    @GET("player_api.php")
    suspend fun authenticate(
        @Query("username") username: String,
        @Query("password") password: String,
    ): XtreamAuthResponse

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<List<XtreamCategoryDto>>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null,
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<List<XtreamLiveStreamDto>>

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories",
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<List<XtreamCategoryDto>>

    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null,
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<List<XtreamVodStreamDto>>

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories",
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<List<XtreamCategoryDto>>

    @GET("player_api.php")
    suspend fun getSeries(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null,
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<List<XtreamSeriesDto>>

    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String,
    ): XtreamSeriesInfoResponse

    @GET("xmltv.php")
    @Streaming
    suspend fun getXmltv(
        @Query("username") username: String,
        @Query("password") password: String,
    ): ResponseBody
}
