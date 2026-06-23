package com.iptvcinema.tv.core.m3u

data class M3uEntry(
    val name: String,
    val url: String,
    val tvgId: String?,
    val tvgName: String?,
    val logo: String?,
    val group: String?,
    val attributes: Map<String, String> = emptyMap(),
)
