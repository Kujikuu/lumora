package com.iptvcinema.tv.core.util

import com.iptvcinema.tv.core.model.CastMember

object CastParser {
    fun parseCastMembers(castString: String?): List<CastMember> {
        val trimmed = castString?.trim().orEmpty()
        if (trimmed.isEmpty()) return emptyList()
        return trimmed.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { name -> CastMember(name = name, role = "") }
    }

    fun parseGenres(genreString: String?): List<String> {
        val trimmed = genreString?.trim().orEmpty()
        if (trimmed.isEmpty()) return emptyList()
        return trimmed.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
