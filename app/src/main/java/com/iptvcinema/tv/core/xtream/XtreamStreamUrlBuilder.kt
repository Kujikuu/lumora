package com.iptvcinema.tv.core.xtream

object XtreamStreamUrlBuilder {
    fun liveStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        extension: String = "ts",
    ): String = "${serverUrl.trimEnd('/')}/live/$username/$password/$streamId.${extension.ifBlank { "ts" }}"

    fun vodStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        extension: String = "mp4",
    ): String = "${serverUrl.trimEnd('/')}/movie/$username/$password/$streamId.${extension.ifBlank { "mp4" }}"

    fun seriesStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        episodeId: String,
        extension: String = "mp4",
    ): String = "${serverUrl.trimEnd('/')}/series/$username/$password/$episodeId.${extension.ifBlank { "mp4" }}"
}
