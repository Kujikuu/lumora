package com.iptvcinema.tv.core.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info") val userInfo: XtreamUserInfoDto? = null,
    @SerialName("server_info") val serverInfo: XtreamServerInfoDto? = null,
)

@Serializable
data class XtreamUserInfoDto(
    val username: String? = null,
    val password: String? = null,
    val message: String? = null,
    val auth: JsonElement? = null,
    val status: String? = null,
    @SerialName("exp_date") val expDate: String? = null,
    @SerialName("is_trial") val isTrial: String? = null,
    @SerialName("active_cons") val activeCons: String? = null,
    @SerialName("max_connections") val maxConnections: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("allowed_output_formats") val allowedOutputFormats: List<String>? = null,
)

@Serializable
data class XtreamServerInfoDto(
    val url: String? = null,
    val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val serverProtocol: String? = null,
    @SerialName("rtmp_port") val rtmpPort: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("timestamp_now") val timestampNow: Long? = null,
    @SerialName("time_now") val timeNow: String? = null,
)

@Serializable
data class XtreamCategoryDto(
    @SerialName("category_id") val categoryId: JsonElement? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("parent_id") val parentId: JsonElement? = null,
)

@Serializable
data class XtreamLiveStreamDto(
    val num: JsonElement? = null,
    val name: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: JsonElement? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: JsonElement? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("tv_archive") val tvArchive: JsonElement? = null,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("is_adult") val isAdult: JsonElement? = null,
)

@Serializable
data class XtreamVodStreamDto(
    val num: JsonElement? = null,
    val name: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: JsonElement? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: JsonElement? = null,
    @SerialName("added") val added: String? = null,
    @SerialName("category_id") val categoryId: JsonElement? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("direct_source") val directSource: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("duration") val duration: String? = null,
    @SerialName("duration_secs") val durationSecs: JsonElement? = null,
    @SerialName("backdrop_path") val backdropPath: JsonElement? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("genre") val genre: String? = null,
)

@Serializable
data class XtreamSeriesDto(
    val num: JsonElement? = null,
    val name: String? = null,
    @SerialName("series_id") val seriesId: JsonElement? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("last_modified") val lastModified: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: JsonElement? = null,
    @SerialName("category_id") val categoryId: JsonElement? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("backdrop_path") val backdropPath: JsonElement? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
)

@Serializable
data class XtreamSeriesInfoResponse(
    val seasons: List<XtreamSeasonDto>? = null,
    val info: XtreamSeriesDetailDto? = null,
    val episodes: Map<String, List<XtreamEpisodeDto>>? = null,
)

@Serializable
data class XtreamSeasonDto(
    @SerialName("season_number") val seasonNumber: JsonElement? = null,
    val name: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_count") val episodeCount: JsonElement? = null,
    val overview: String? = null,
)

@Serializable
data class XtreamSeriesDetailDto(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("backdrop_path") val backdropPath: JsonElement? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("category_id") val categoryId: JsonElement? = null,
)

@Serializable
data class XtreamEpisodeDto(
    val id: JsonElement? = null,
    @SerialName("episode_num") val episodeNum: JsonElement? = null,
    val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    val info: XtreamEpisodeInfoDto? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("added") val added: String? = null,
    @SerialName("season") val season: JsonElement? = null,
    @SerialName("direct_source") val directSource: String? = null,
)

@Serializable
data class XtreamEpisodeInfoDto(
    val plot: String? = null,
    val duration: String? = null,
    @SerialName("duration_secs") val durationSecs: JsonElement? = null,
    @SerialName("movie_image") val movieImage: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("rating") val rating: String? = null,
)

fun JsonElement?.asIdString(): String? = when (this) {
    null -> null
    is JsonPrimitive -> when {
        isString -> contentOrNull
        intOrNull != null -> intOrNull.toString()
        else -> contentOrNull
    }
    else -> toString()
}

fun JsonElement?.asIntOrNull(): Int? = when (this) {
    null -> null
    is JsonPrimitive -> intOrNull ?: contentOrNull?.toIntOrNull()
    else -> null
}

fun JsonElement?.asBooleanOrFalse(): Boolean = when (this) {
    null -> false
    is JsonPrimitive -> {
        when {
            intOrNull == 1 -> true
            intOrNull == 0 -> false
            contentOrNull.equals("1", ignoreCase = true) -> true
            contentOrNull.equals("true", ignoreCase = true) -> true
            else -> false
        }
    }
    else -> false
}

fun JsonElement?.firstStringOrNull(): String? = when (this) {
    null -> null
    is JsonPrimitive -> contentOrNull
    is JsonObject -> null
    else -> null
}

fun XtreamUserInfoDto.isAuthenticated(): Boolean {
    if (message?.contains("invalid", ignoreCase = true) == true) return false
    if (status?.equals("Expired", ignoreCase = true) == true) return false
    return when (val authValue = auth) {
        null -> username != null
        is JsonPrimitive -> authValue.intOrNull == 1 ||
            authValue.contentOrNull.equals("1", ignoreCase = true) ||
            authValue.contentOrNull.equals("true", ignoreCase = true)
        else -> true
    }
}
